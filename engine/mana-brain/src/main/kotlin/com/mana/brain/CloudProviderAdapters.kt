package com.mana.brain

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URI
import java.nio.charset.StandardCharsets

enum class CloudProviderKind(val id: String, val defaultBaseUrl: String) {
    OPENAI("openai", "https://api.openai.com"),
    ANTHROPIC("anthropic", "https://api.anthropic.com"),
    GEMINI("gemini", "https://generativelanguage.googleapis.com"),
    XAI("xai", "https://api.x.ai")
}

fun interface ManaSecretStore {
    /** Returns a secret only to the caller; implementations must never log or export it. */
    fun readSecret(alias: String): String?
}

data class CloudProviderConfig(
    val kind: CloudProviderKind,
    val model: String,
    val secretAlias: String,
    val enabled: Boolean = false,
    val baseUrl: String = kind.defaultBaseUrl
) {
    init {
        require(model.isNotBlank())
        require(secretAlias.isNotBlank())
        require(baseUrl.startsWith("https://")) { "Cloud provider endpoints must use HTTPS." }
        require(URI(baseUrl).host == URI(kind.defaultBaseUrl).host) { "Cloud credentials may only be sent to the provider official API host." }
    }
}

data class CloudHttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val body: String
)

data class CloudHttpResponse(val statusCode: Int, val body: String)

fun interface CloudHttpTransport {
    suspend fun post(request: CloudHttpRequest): CloudHttpResponse
}

/** Bounded HTTPS transport. Secrets are never included in exceptions or diagnostics. */
class UrlConnectionCloudTransport(
    private val connectTimeoutMs: Int = 15_000,
    private val readTimeoutMs: Int = 90_000,
    private val maxResponseBytes: Int = 4 * 1024 * 1024
) : CloudHttpTransport {
    override suspend fun post(request: CloudHttpRequest): CloudHttpResponse = withContext(Dispatchers.IO) {
        val connection = (URL(request.url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            request.headers.forEach { (name, value) -> setRequestProperty(name, value) }
        }
        try {
            val payload = request.body.toByteArray(StandardCharsets.UTF_8)
            connection.setFixedLengthStreamingMode(payload.size)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.use { input ->
                val bytes = input.readNBytes(maxResponseBytes + 1)
                require(bytes.size <= maxResponseBytes) { "Cloud response exceeded the safe size limit." }
                String(bytes, StandardCharsets.UTF_8)
            }.orEmpty()
            CloudHttpResponse(status, body)
        } finally {
            connection.disconnect()
        }
    }
}

class CloudInferenceProvider(
    private val config: CloudProviderConfig,
    private val secrets: ManaSecretStore,
    private val transport: CloudHttpTransport = UrlConnectionCloudTransport()
) : InferenceProvider {
    override val id: String = config.kind.id

    override fun isAvailable(): Boolean =
        config.enabled && !secrets.readSecret(config.secretAlias).isNullOrBlank()

    override suspend fun generate(prompt: String): String {
        require(prompt.isNotBlank())
        check(config.enabled) { "Cloud provider is disabled by owner settings." }
        val key = secrets.readSecret(config.secretAlias)?.takeIf { it.isNotBlank() }
            ?: error("API credential for ${config.kind.id} is not configured.")
        val request = buildRequest(prompt, key)
        val response = transport.post(request)
        if (response.statusCode !in 200..299) {
            throw IllegalStateException("Cloud provider ${config.kind.id} returned HTTP ${response.statusCode}. Check provider settings and quota.")
        }
        return extractText(response.body).ifBlank { error("Cloud provider ${config.kind.id} returned an empty response.") }
    }

    private fun buildRequest(prompt: String, key: String): CloudHttpRequest {
        val headers = linkedMapOf<String, String>()
        val body = JsonObject()
        val url = when (config.kind) {
            CloudProviderKind.OPENAI -> {
                headers["Authorization"] = "Bearer $key"
                body.addProperty("model", config.model)
                body.addProperty("input", prompt)
                "${config.baseUrl.trimEnd('/')}/v1/responses"
            }
            CloudProviderKind.XAI -> {
                headers["Authorization"] = "Bearer $key"
                body.addProperty("model", config.model)
                body.addProperty("input", prompt)
                "${config.baseUrl.trimEnd('/')}/v1/responses"
            }
            CloudProviderKind.ANTHROPIC -> {
                headers["x-api-key"] = key
                headers["anthropic-version"] = "2023-06-01"
                body.addProperty("model", config.model)
                body.addProperty("max_tokens", 2048)
                val messages = JsonArray()
                messages.add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", prompt)
                })
                body.add("messages", messages)
                "${config.baseUrl.trimEnd('/')}/v1/messages"
            }
            CloudProviderKind.GEMINI -> {
                headers["x-goog-api-key"] = key
                val parts = JsonArray().apply {
                    add(JsonObject().apply { addProperty("text", prompt) })
                }
                val contents = JsonArray().apply {
                    add(JsonObject().apply { add("parts", parts) })
                }
                body.add("contents", contents)
                "${config.baseUrl.trimEnd('/')}/v1beta/models/${config.model}:generateContent"
            }
        }
        return CloudHttpRequest(url, headers, body.toString())
    }

    private fun extractText(raw: String): String {
        val root = JsonParser.parseString(raw).asJsonObject
        return when (config.kind) {
            CloudProviderKind.OPENAI -> {
                val output = root.getAsJsonArray("output") ?: return ""
                output.flatMap { item ->
                    val obj = item.asJsonObject
                    if (obj.get("type")?.asString != "message") emptyList()
                    else (obj.getAsJsonArray("content") ?: JsonArray()).mapNotNull { part ->
                        val content = part.asJsonObject
                        if (content.get("type")?.asString == "output_text") content.get("text")?.asString else null
                    }
                }.joinToString("")
            }
            CloudProviderKind.XAI -> {
                val output = root.getAsJsonArray("output") ?: return ""
                output.flatMap { item ->
                    val obj = item.asJsonObject
                    if (obj.get("type")?.asString != "message") emptyList()
                    else (obj.getAsJsonArray("content") ?: JsonArray()).mapNotNull { part ->
                        val content = part.asJsonObject
                        if (content.get("type")?.asString == "output_text") content.get("text")?.asString else null
                    }
                }.joinToString("")
            }
            CloudProviderKind.ANTHROPIC ->
                (root.getAsJsonArray("content") ?: JsonArray()).mapNotNull { part ->
                    part.asJsonObject.get("text")?.asString
                }.joinToString("")
            CloudProviderKind.GEMINI -> {
                val candidates = root.getAsJsonArray("candidates") ?: return ""
                candidates.firstOrNull()?.asJsonObject?.getAsJsonObject("content")
                    ?.getAsJsonArray("parts")?.mapNotNull { part ->
                        part.asJsonObject.get("text")?.asString
                    }?.joinToString("").orEmpty()
            }
        }
    }
}

/** Builds only owner-enabled providers; no credentials or network access are created by default. */
object CloudProviderRegistry {
    fun create(
        configs: List<CloudProviderConfig>,
        secrets: ManaSecretStore,
        transport: CloudHttpTransport = UrlConnectionCloudTransport()
    ): List<InferenceProvider> {
        require(configs.map { it.kind }.distinct().size == configs.size) { "Only one configuration per cloud provider is allowed." }
        return configs.filter { it.enabled }.map { CloudInferenceProvider(it, secrets, transport) }
    }
}
