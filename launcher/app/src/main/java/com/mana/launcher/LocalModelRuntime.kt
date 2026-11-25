package com.mana.launcher

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Conversation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.collect
import java.io.File
import java.util.concurrent.Executors

/** Owns LiteRT-LM Engine and Conversation on one dedicated background thread. */
class LocalModelRuntime(private val cacheDirectory: File) {
    private val executor = Executors.newSingleThreadExecutor { task -> Thread(task, "MANA-LiteRT-LM") }
    @Volatile private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null
    @Volatile private var loadedModel: File? = null
    @Volatile private var loadedBackend: String? = null

    fun isReady(): Boolean = conversation != null && loadedModel?.isFile == true
    fun modelName(): String? = loadedModel?.let { "${it.name} · ${loadedBackend ?: "unknown"}" }

    fun load(modelFile: File, callback: (Result<String>) -> Unit) {
        executor.execute {
            try {
                require(modelFile.isFile && modelFile.canRead()) { "Файл модели недоступен для чтения." }
                require(modelFile.extension.equals("litertlm", ignoreCase = true)) { "Выбери файл с расширением .litertlm." }
                closeLoaded()
                cacheDirectory.mkdirs()
                val (candidate, backendName) = createEngineWithFallback(modelFile)
                val newConversation = candidate.createConversation()
                engine = candidate
                conversation = newConversation
                loadedModel = modelFile
                loadedBackend = backendName
                callback(Result.success("${modelFile.name} · ${backendName}"))
            } catch (error: Exception) {
                closeLoaded()
                callback(Result.failure(error))
            }
        }
    }

    fun generate(prompt: String, onChunk: (String) -> Unit, callback: (Result<Unit>) -> Unit) {
        executor.execute {
            try {
                val active = conversation ?: error("Сначала загрузи файл модели .litertlm.")
                require(prompt.isNotBlank()) { "Пустой запрос." }
                runBlocking { active.sendMessageAsync(prompt).collect { message -> onChunk(message.toString()) } }
                callback(Result.success(Unit))
            } catch (error: Exception) {
                callback(Result.failure(error))
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

    private fun createEngineWithFallback(modelFile: File): Pair<Engine, String> {
        var gpuFailure: Throwable? = null
        var gpuEngine: Engine? = null
        try {
            gpuEngine = Engine(EngineConfig(modelPath = modelFile.absolutePath, backend = Backend.GPU(), cacheDir = cacheDirectory.absolutePath))
            gpuEngine.initialize()
            return gpuEngine to "GPU"
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
            return cpuEngine to "CPU"
        } catch (error: Exception) {
            try { cpuEngine.close() } catch (_: Exception) { }
            throw IllegalStateException("GPU backend failed (${gpuFailure?.message}); CPU fallback failed (${error.message})", error)
        }
    }

    fun shutdown() { executor.execute { closeLoaded() }; executor.shutdown() }
}