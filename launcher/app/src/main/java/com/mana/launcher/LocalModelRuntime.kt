package com.mana.launcher

import android.content.Context
import android.content.ComponentCallbacks2
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/** Owns LiteRT-LM Engine and Conversation on one dedicated background thread. */
class LocalModelRuntime(
    private val cacheDirectory: File,
    private val appContext: Context? = null
) : LocalInferenceRuntime {
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "MANA-LiteRT-LM") }
    private val closed = AtomicBoolean(false)
    @Volatile private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null
    @Volatile private var loadedModel: File? = null
    @Volatile private var loadedBackend: String? = null
    @Volatile private var selectedModelFile: File? = null
    private val activeJobs = java.util.concurrent.ConcurrentHashMap.newKeySet<Job>()


    override fun isReady(): Boolean = conversation != null && loadedModel?.isFile == true
    override fun modelName(): String? = loadedModel?.let { "${it.name} · ${loadedBackend ?: "unknown"}" }

    override fun load(modelFile: File, callback: (Result<String>) -> Unit) {
        val completed = AtomicBoolean(false)
        fun complete(result: Result<String>) {
            if (completed.compareAndSet(false, true)) runCatching { callback(result) }
        }
        if (closed.get()) {
            complete(Result.failure(IllegalStateException("MANA local inference runtime is shut down")))
            return
        }
        try {
            executor.execute {
                try {
                    require(modelFile.isFile && modelFile.canRead()) { "Файл модели недоступен для чтения." }
                    require(modelFile.extension.equals("litertlm", ignoreCase = true)) { "Выбери файл с расширением .litertlm." }
                    appContext?.let { context ->
                        require(ManaResourceGovernor.canLoadModel(context, modelFile)) {
                            "Недостаточно свободной оперативной памяти для безопасной загрузки модели. MANA отложила запуск модулей, чтобы не перегружать телефон."
                        }
                    }
                    closeLoaded()
                    cacheDirectory.mkdirs()
                    val (candidate, newConversation, backendName) = createEngineWithFallback(modelFile)
                    engine = candidate
                    conversation = newConversation
                    loadedModel = modelFile
                    selectedModelFile = modelFile
                    loadedBackend = backendName
                    complete(Result.success("${modelFile.name} · ${backendName}"))
                } catch (error: Exception) {
                    closeLoaded()
                    complete(Result.failure(error))
                }
            }
        } catch (rejected: RejectedExecutionException) {
            complete(Result.failure(IllegalStateException("MANA local inference runtime is shutting down", rejected)))
        }
    }

    override fun generate(prompt: String, onChunk: (String) -> Unit, callback: (Result<Unit>) -> Unit): LocalInferenceCancellation {
        val cancellation = LocalInferenceCancellation()
        val completed = AtomicBoolean(false)
        fun complete(result: Result<Unit>) {
            if (completed.compareAndSet(false, true)) runCatching { callback(result) }
        }
        if (closed.get()) {
            complete(Result.failure(IllegalStateException("MANA local inference runtime is shut down")))
            cancellation.cancel()
            return cancellation
        }
        try {
            executor.execute {
                if (cancellation.isCancelled()) return@execute
                try {
                    if (appContext != null && ManaResourceGovernor.shouldUnloadModel(appContext)) {
                        closeLoaded()
                        error("MANA выгрузила локальную модель из-за нехватки RAM. Освободи память; при следующем запросе MANA попробует загрузить её снова.")
                    }
                    val active = conversation ?: restoreSelectedConversation()
                    require(prompt.isNotBlank()) { "Пустой запрос." }
                    runBlocking {
                        val generationJob = checkNotNull(coroutineContext[Job])
                        activeJobs.add(generationJob)
                        cancellation.attach(generationJob)
                        try {
                            active.sendMessageAsync(prompt).collect { message -> onChunk(message.toString()) }
                        } finally {
                            activeJobs.remove(generationJob)
                        }
                    }
                    complete(Result.success(Unit))
                } catch (error: Exception) {
                    complete(Result.failure(error))
                }
            }
        } catch (rejected: RejectedExecutionException) {
            cancellation.cancel()
            complete(Result.failure(IllegalStateException("MANA local inference runtime is shutting down", rejected)))
        }
        return cancellation
    }

    private fun restoreSelectedConversation(): Conversation {
        val modelFile = selectedModelFile ?: error("Сначала загрузи файл модели .litertlm.")
        appContext?.let { context ->
            require(ManaResourceGovernor.canLoadModel(context, modelFile)) {
                "Свободной RAM пока недостаточно. MANA отложила загрузку локальной модели."
            }
        }
        val (candidate, restoredConversation, backendName) = createEngineWithFallback(modelFile)
        engine = candidate
        conversation = restoredConversation
        loadedModel = modelFile
        loadedBackend = backendName
        return restoredConversation
    }

    fun onTrimMemory(level: Int) {
        if (level < ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) return
        runCatching {
            executor.execute {
                if (appContext == null || ManaResourceGovernor.shouldUnloadModel(appContext) ||
                    level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
                    closeLoaded()
                }
            }
        }
    }

    private fun closeLoaded() {
        try { conversation?.close() } catch (_: Exception) { }
        conversation = null
        try { engine?.close() } catch (_: Exception) { }
        engine = null
        loadedModel = null
        loadedBackend = null
    }

    private fun createConversation(engine: Engine): Conversation =
        engine.createConversation(
            ConversationConfig(
                systemInstruction = Contents.of(ManaPersona.SYSTEM_PROMPT),
                samplerConfig = SamplerConfig(topK = 40, topP = 0.92, temperature = 0.72)
            )
        )

    private fun createEngineWithFallback(modelFile: File): Triple<Engine, Conversation, String> {
        var gpuFailure: Throwable? = null
        var gpuEngine: Engine? = null
        try {
            gpuEngine = Engine(EngineConfig(modelPath = modelFile.absolutePath, backend = Backend.GPU(), cacheDir = cacheDirectory.absolutePath))
            gpuEngine.initialize()
            val conversation = createConversation(gpuEngine)
            return Triple(gpuEngine, conversation, "GPU")
        } catch (error: Exception) {
            gpuFailure = error
            try { gpuEngine?.close() } catch (_: Exception) { }
        } catch (error: LinkageError) {
            gpuFailure = error
            try { gpuEngine?.close() } catch (_: Exception) { }
        }

        val cpuEngine = Engine(EngineConfig(modelPath = modelFile.absolutePath, backend = Backend.CPU(), cacheDir = cacheDirectory.absolutePath))
        try {
            cpuEngine.initialize()
            val conversation = createConversation(cpuEngine)
            return Triple(cpuEngine, conversation, "CPU")
        } catch (error: Exception) {
            try { cpuEngine.close() } catch (_: Exception) { }
            throw IllegalStateException(
                "GPU backend failed (${gpuFailure?.message}); CPU fallback failed (${error.message})",
                error
            )
        }
    }

    override fun shutdown() {
        activeJobs.forEach { it.cancel(CancellationException("MANA local runtime is shutting down")) }
        if (closed.compareAndSet(false, true)) {
            executor.execute { closeLoaded() }
            executor.shutdown()
        }
    }
}