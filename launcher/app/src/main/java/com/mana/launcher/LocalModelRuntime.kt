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

    fun isReady(): Boolean = conversation != null && loadedModel?.isFile == true
    fun modelName(): String? = loadedModel?.name

    fun load(modelFile: File, callback: (Result<String>) -> Unit) {
        executor.execute {
            try {
                require(modelFile.isFile && modelFile.canRead()) { "Файл модели недоступен для чтения." }
                require(modelFile.extension.equals("litertlm", ignoreCase = true)) { "Выбери файл с расширением .litertlm." }
                closeLoaded()
                cacheDirectory.mkdirs()
                val candidate = Engine(EngineConfig(modelPath = modelFile.absolutePath, backend = Backend.CPU(), cacheDir = cacheDirectory.absolutePath))
                candidate.initialize()
                val newConversation = candidate.createConversation()
                engine = candidate
                conversation = newConversation
                loadedModel = modelFile
                callback(Result.success(modelFile.name))
            } catch (error: Throwable) {
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
                runBlocking { active.sendMessageAsync(prompt).collect { message -> onChunk(message.text) } }
                callback(Result.success(Unit))
            } catch (error: Throwable) {
                callback(Result.failure(error))
            }
        }
    }

    private fun closeLoaded() {
        try { conversation?.close() } catch (_: Throwable) { }
        conversation = null
        try { engine?.close() } catch (_: Throwable) { }
        engine = null
        loadedModel = null
    }

    fun shutdown() { executor.execute { closeLoaded() }; executor.shutdown() }
}