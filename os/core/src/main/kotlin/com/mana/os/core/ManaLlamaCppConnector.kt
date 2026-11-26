package com.mana.os.core

import java.io.File

/**
 * Narrow native boundary around llama.cpp. The JVM core can be tested with a fake
 * implementation; Android production wiring supplies the JNI implementation.
 */
interface ManaLlamaCppRuntime {
    fun loadModel(modelPath: String, contextSize: Int, threads: Int): Long
    fun generate(modelHandle: Long, prompt: String, maxTokens: Int, temperature: Float): String
    fun releaseModel(modelHandle: Long)
}

/**
 * JNI binding contract. The shared library "mana_llama_jni" must be packaged for the
 * target ABI and linked against a verified llama.cpp build before this can run on-device.
 */
class JniManaLlamaCppRuntime : ManaLlamaCppRuntime {
    private external fun nativeLoadModel(modelPath: String, contextSize: Int, threads: Int): Long
    private external fun nativeGenerate(
        modelHandle: Long, prompt: String, maxTokens: Int, temperature: Float
    ): String
    private external fun nativeReleaseModel(modelHandle: Long)

    override fun loadModel(modelPath: String, contextSize: Int, threads: Int): Long =
        nativeLoadModel(modelPath, contextSize, threads)

    override fun generate(modelHandle: Long, prompt: String, maxTokens: Int, temperature: Float): String =
        nativeGenerate(modelHandle, prompt, maxTokens, temperature)

    override fun releaseModel(modelHandle: Long) = nativeReleaseModel(modelHandle)

    companion object {
        @Volatile private var loaded = false

        @Synchronized
        fun loadLibrary() {
            if (!loaded) {
                System.loadLibrary("mana_llama_jni")
                loaded = true
            }
        }
    }
}

/**
 * Local-only chat connector for a verified GGUF model. It does not download models,
 * execute shell commands, or route prompts to a network.
 */
class ManaLlamaCppConnector(
    private val modelFile: File,
    private val runtime: ManaLlamaCppRuntime,
    private val contextSize: Int = 2_048,
    private val threads: Int = 4,
    private val maxTokens: Int = 512,
    private val temperature: Float = 0.7f
) : ManaServiceConnector, AutoCloseable {
    override val descriptor = ManaConnectorDescriptor(
        id = "local.llama_cpp",
        displayName = "MANA Local llama.cpp",
        kind = ManaConnectorKind.AI_MODEL,
        capabilities = setOf("chat.respond"),
        state = ManaConnectorState.DISABLED,
        supportsSensitiveData = true
    )

    private val lock = Any()
    private var modelHandle: Long? = null

    init {
        require(contextSize in 256..32_768)
        require(threads in 1..64)
        require(maxTokens in 1..8_192)
        require(temperature.isFinite() && temperature in 0.0f..2.0f)
        require(modelFile.name.endsWith(".gguf", ignoreCase = true)) {
            "llama.cpp requires a GGUF model file"
        }
    }

    override suspend fun execute(capability: String, request: String): String {
        require(capability == "chat.respond") { "Unsupported local model capability" }
        val parsed = parseRequest(request)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            synchronized(lock) {
                val file = modelFile.canonicalFile
                require(file.isFile && file.canRead()) { "Local GGUF model is missing or unreadable" }
                val handle = modelHandle ?: runtime.loadModel(file.absolutePath, contextSize, threads)
                    .also { loaded ->
                        check(loaded != 0L) { "llama.cpp failed to load the GGUF model" }
                        modelHandle = loaded
                    }
                runtime.generate(handle, parsed.prompt, maxTokens, temperature)
                    .also { output -> require(output.isNotBlank()) { "llama.cpp returned an empty response" } }
            }
        }
    }

    override fun close() {
        synchronized(lock) {
            modelHandle?.let(runtime::releaseModel)
            modelHandle = null
        }
    }

    private data class ParsedRequest(val sensitive: Boolean, val prompt: String)

    private fun parseRequest(payload: String): ParsedRequest {
        require(payload.length <= 32_128) { "Reasoning payload is too large" }
        val first = payload.indexOf('\n')
        val second = if (first >= 0) payload.indexOf('\n', first + 1) else -1
        val third = if (second >= 0) payload.indexOf('\n', second + 1) else -1
        require(first >= 0 && second >= 0 && third >= 0) { "Malformed MANA reasoning envelope" }
        require(payload.substring(0, first) == "MANA-REASONING/1") { "Unsupported reasoning envelope" }
        val sensitivity = payload.substring(first + 1, second)
        require(sensitivity == "sensitive=true" || sensitivity == "sensitive=false") {
            "Malformed sensitivity field"
        }
        val lengthLine = payload.substring(second + 1, third)
        require(lengthLine.startsWith("prompt-length=")) { "Missing prompt length" }
        val expectedLength = lengthLine.removePrefix("prompt-length=").toIntOrNull()
            ?: error("Invalid prompt length")
        val prompt = payload.substring(third + 1)
        require(expectedLength == prompt.length && prompt.isNotBlank()) {
            "Prompt length does not match the reasoning envelope"
        }
        return ParsedRequest(sensitivity == "sensitive=true", prompt)
    }
}
