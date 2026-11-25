package com.mana.launcher

import android.content.Context
import com.mana.brain.InferenceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ManaYandexConfig(val apiKey: String, val folderId: String, val modelId: String, val enabled: Boolean)

/** Official Yandex AI Studio Text Generation API adapter. Never enabled until configured by the owner. */
class ManaYandexAiStudioProvider(context: Context) : InferenceProvider {
    private val vault = ManaEncryptedCredentialStore(context, "yandex-ai-studio.bin", "mana.yandex.credentials.v1")
    override val id: String = "yandex-ai-studio"

    override fun isAvailable(): Boolean = runCatching { config()?.let { it.enabled && it.apiKey.isNotBlank() } == true }.getOrDefault(false)

    fun configure(apiKey: String, folderId: String, modelId: String, enabled: Boolean = true) {
        val previous = runCatching { config() }.getOrNull()
        val key = apiKey.ifBlank { previous?.apiKey.orEmpty() }
        require(key.length >= 12) { "Yandex API key is required." }
        require(folderId.matches(Regex("[A-Za-z0-9_-]{10,64}"))) { "Yandex folder ID is invalid." }
        require(modelId.matches(Regex("[A-Za-z0-9._/-]{2,100}")) && !modelId.contains("..") && !modelId.startsWith("/") && !modelId.endsWith("/")) { "Yandex model ID is invalid." }
        val value = JSONObject().put("apiKey", key).put("folderId", folderId)
            .put("modelId", modelId).put("enabled", enabled)
        vault.write(value.toString())
    }

    fun disable() {
        val old = config() ?: return
        vault.write(JSONObject().put("apiKey", old.apiKey).put("folderId", old.folderId)
            .put("modelId", old.modelId).put("enabled", false).toString())
    }

    fun removeCredentials() = vault.clear()

    fun currentConfig(): ManaYandexConfig? = config()

    private fun config(): ManaYandexConfig? {
        val raw = vault.read() ?: return null
        val json = JSONObject(raw)
        return ManaYandexConfig(json.optString("apiKey"), json.optString("folderId"),
            json.optString("modelId"), json.optBoolean("enabled", false))
    }

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        require(prompt.isNotBlank() && prompt.length <= 100_000)
        val config = config() ?: error("Yandex AI Studio is not configured.")
        require(config.enabled) { "Yandex AI Studio is disabled." }
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Api-Key ${config.apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            val body = JSONObject()
                .put("modelUri", "gpt://${config.folderId}/${config.modelId}")
                .put("completionOptions", JSONObject().put("stream", false).put("temperature", 0.6).put("maxTokens", "2048"))
                .put("messages", org.json.JSONArray().put(JSONObject().put("role", "user").put("text", prompt)))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val response = readBounded(if (code in 200..299) connection.inputStream else connection.errorStream)
            check(code in 200..299) { "Yandex AI Studio returned HTTP $code. Check the folder, model, API key and quota." }
            extractText(JSONObject(response))
        } finally { connection.disconnect() }
    }

    private fun readBounded(stream: java.io.InputStream?, limit: Int = 2 * 1024 * 1024): String {
        if (stream == null) return ""
        stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                require(total <= limit) { "Yandex response exceeded the 2 MiB safety limit." }
                output.write(buffer, 0, count)
            }
            return output.toString("UTF-8")
        }
    }

    private fun extractText(root: JSONObject): String {
        val alternatives = root.optJSONObject("result")?.optJSONArray("alternatives") ?: JSONArray()
        val text = alternatives.optJSONObject(0)?.optJSONObject("message")?.optString("text").orEmpty()
        return text.ifBlank { error("Yandex AI Studio returned no text content.") }
    }

    companion object { private const val ENDPOINT = "https://ai.api.cloud.yandex.net/foundationModels/v1/completion" }
}
