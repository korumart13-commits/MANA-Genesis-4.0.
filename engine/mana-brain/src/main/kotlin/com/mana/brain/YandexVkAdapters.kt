package com.mana.brain

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

enum class YandexCredentialKind { IAM_TOKEN, API_KEY }

data class YandexAiStudioConfig(
    val folderId: String,
    val modelUri: String,
    val credentialAlias: String,
    val credentialKind: YandexCredentialKind = YandexCredentialKind.IAM_TOKEN,
    val enabled: Boolean = false
) {
    init {
        require(folderId.matches(Regex("[A-Za-z0-9_-]{3,128}")))
        require(modelUri.startsWith("gpt://$folderId/")) { "Yandex model URI must belong to the configured folder." }
        require(credentialAlias.isNotBlank())
    }
}

/** Optional Yandex AI Studio model provider. It is never enabled by default. */
class YandexAiStudioProvider(
    private val config: YandexAiStudioConfig,
    private val secrets: ManaSecretStore,
    private val transport: CloudHttpTransport = UrlConnectionCloudTransport()
) : InferenceProvider {
    override val id: String = "yandex-ai-studio"

    override fun isAvailable(): Boolean =
        config.enabled && !secrets.readSecret(config.credentialAlias).isNullOrBlank()

    override suspend fun generate(prompt: String): String {
        require(prompt.isNotBlank() && prompt.length <= 100_000)
        check(config.enabled) { "Yandex AI Studio is disabled by owner settings." }
        val token = secrets.readSecret(config.credentialAlias)?.takeIf { it.isNotBlank() }
            ?: error("Yandex credential is not configured.")
        val authPrefix = if (config.credentialKind == YandexCredentialKind.IAM_TOKEN) "Bearer" else "Api-Key"
        val body = JsonObject().apply {
            addProperty("modelUri", config.modelUri)
            add("completionOptions", JsonObject().apply {
                addProperty("stream", false)
                addProperty("temperature", 0.3)
                addProperty("maxTokens", "2048")
            })
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("text", prompt)
                })
            })
        }
        val response = transport.post(
            CloudHttpRequest(
                url = "https://ai.api.cloud.yandex.net/foundationModels/v1/completion",
                headers = mapOf(
                    "Authorization" to "$authPrefix $token",
                    "x-folder-id" to config.folderId
                ),
                body = body.toString()
            )
        )
        if (response.statusCode !in 200..299) {
            throw IllegalStateException("Yandex AI Studio returned HTTP ${response.statusCode}. Check IAM/API-key permissions and folder access.")
        }
        val root = JsonParser.parseString(response.body).asJsonObject
        val alternatives = root.getAsJsonArray("alternatives") ?: JsonArray()
        val text = alternatives.firstOrNull()?.asJsonObject
            ?.getAsJsonObject("message")?.get("text")?.asString.orEmpty()
        return text.ifBlank { error("Yandex AI Studio returned an empty response.") }
    }
}

data class VkUserProfile(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val screenName: String
)

fun interface VkFormTransport {
    suspend fun postForm(url: String, fields: Map<String, String>): CloudHttpResponse
}

/** VK API form transport. Access tokens are sent in the POST body, never in the URL. */
class UrlConnectionVkTransport(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 30_000,
    private val maxResponseBytes: Int = 1_000_000
) : VkFormTransport {
    override suspend fun postForm(url: String, fields: Map<String, String>): CloudHttpResponse = withContext(Dispatchers.IO) {
        require(url == "https://api.vk.com/method/users.get") { "VK client is restricted to the approved read-only users.get endpoint." }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val encoded = fields.entries.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
            }.toByteArray(StandardCharsets.UTF_8)
            connection.setFixedLengthStreamingMode(encoded.size)
            connection.outputStream.use { it.write(encoded) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val bytes = input.readNBytes(maxResponseBytes + 1)
                require(bytes.size <= maxResponseBytes) { "VK response exceeded the safe size limit." }
                String(bytes, StandardCharsets.UTF_8)
            }.orEmpty()
            CloudHttpResponse(status, body)
        } finally {
            connection.disconnect()
        }
    }
}

/** Read-only VK connector. It intentionally has no post/message/friend mutation methods. */
class VkReadOnlyClient(
    private val secrets: ManaSecretStore,
    private val tokenAlias: String,
    private val apiVersion: String = "5.199",
    private val enabled: Boolean = false,
    private val transport: VkFormTransport = UrlConnectionVkTransport()
) {
    fun isAvailable(): Boolean = enabled && !secrets.readSecret(tokenAlias).isNullOrBlank()

    suspend fun getUserProfile(userId: Long): VkUserProfile {
        require(userId > 0)
        check(enabled) { "VK connector is disabled by owner settings." }
        val token = secrets.readSecret(tokenAlias)?.takeIf { it.isNotBlank() }
            ?: error("VK access token is not configured.")
        val response = transport.postForm(
            "https://api.vk.com/method/users.get",
            mapOf(
                "user_ids" to userId.toString(),
                "fields" to "screen_name",
                "access_token" to token,
                "v" to apiVersion
            )
        )
        if (response.statusCode !in 200..299) {
            throw IllegalStateException("VK API returned HTTP ${response.statusCode}.")
        }
        val root = JsonParser.parseString(response.body).asJsonObject
        if (root.has("error")) {
            val code = root.getAsJsonObject("error").get("error_code")?.asInt ?: -1
            throw IllegalStateException("VK API rejected the read-only profile request (error $code).")
        }
        val user = root.getAsJsonArray("response")?.firstOrNull()?.asJsonObject
            ?: error("VK returned no user profile.")
        return VkUserProfile(
            id = user.get("id")?.asLong ?: userId,
            firstName = user.get("first_name")?.asString.orEmpty(),
            lastName = user.get("last_name")?.asString.orEmpty(),
            screenName = user.get("screen_name")?.asString.orEmpty()
        )
    }
}
