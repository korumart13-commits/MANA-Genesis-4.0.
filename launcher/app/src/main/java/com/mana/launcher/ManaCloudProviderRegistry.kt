package com.mana.launcher

import android.content.Context
import com.mana.brain.InferenceProvider
import com.mana.os.core.ManaSecretKeyProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Optional cloud credentials encrypted with an Android Keystore AES-GCM key.
 * Provider enablement is separate from per-request consent; neither is on by default.
 */
class ManaCloudCredentialStore(
    private val file: File,
    private val keys: ManaSecretKeyProvider
) {
    private val lock = Any()
    private val magic = "MANACLOUD1".toByteArray(Charsets.US_ASCII)

    fun set(providerId: String, apiKey: String) = synchronized(lock) {
        require(providerId in PROVIDER_IDS) { "Unknown cloud provider" }
        val normalized = apiKey.trim()
        require(normalized.length in 12..512) { "API key length is invalid" }
        val entries = readAll().toMutableMap()
        entries[providerId] = normalized
        writeAll(entries)
    }

    fun get(providerId: String): String? = synchronized(lock) { readAll()[providerId] }

    fun remove(providerId: String) = synchronized(lock) {
        val entries = readAll().toMutableMap()
        entries.remove(providerId)
        writeAll(entries)
    }

    private fun readAll(): Map<String, String> {
        if (!file.isFile) return emptyMap()
        val bytes = file.readBytes()
        require(bytes.size > magic.size + 12 + 16) { "Cloud credential file is truncated" }
        require(bytes.copyOfRange(0, magic.size).contentEquals(magic)) { "Unknown cloud credential format" }
        val nonce = bytes.copyOfRange(magic.size, magic.size + 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keys.getOrCreate(KEY_ALIAS), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(magic.size + 12, bytes.size)), Charsets.UTF_8))
        return buildMap {
            PROVIDER_IDS.forEach { id ->
                val value = json.optString(id, "")
                if (value.isNotBlank()) put(id, value)
            }
        }
    }

    private fun writeAll(entries: Map<String, String>) {
        val parent = file.absoluteFile.parentFile ?: error("Credential path has no parent")
        require(parent.exists() || parent.mkdirs()) { "Cannot create credential directory" }
        val clear = JSONObject().apply { entries.forEach { (id, key) -> put(id, key) } }
            .toString().toByteArray(Charsets.UTF_8)
        val nonce = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keys.getOrCreate(KEY_ALIAS), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val encrypted = cipher.doFinal(clear)
        val temp = File.createTempFile("mana-cloud-", ".part", parent)
        try {
            temp.outputStream().use { output ->
                output.write(magic)
                output.write(nonce)
                output.write(encrypted)
                output.flush()
            }
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    companion object {
        private const val KEY_ALIAS = "mana.cloud.credentials.v1"
        val PROVIDER_IDS = setOf("openai", "anthropic", "google", "xai")
    }
}

/** Provider adapters use HTTPS, bounded responses and never log credentials or prompts. */
class ManaCloudProviderRegistry(
    context: Context,
    private val credentials: ManaCloudCredentialStore =
        ManaCloudCredentialStore(File(context.noBackupFilesDir, "mana/cloud-credentials.bin"), ManaAndroidKeyProvider())
) {
    private val preferences = context.getSharedPreferences("mana.cloud", Context.MODE_PRIVATE)

    fun isEnabled(providerId: String): Boolean = providerId in ManaCloudCredentialStore.PROVIDER_IDS &&
        preferences.getBoolean("enabled.$providerId", false) && credentials.get(providerId) != null

    fun saveApiKey(providerId: String, apiKey: String) = credentials.set(providerId, apiKey)

    fun removeApiKey(providerId: String) {
        credentials.remove(providerId)
        preferences.edit().putBoolean("enabled.$providerId", false).apply()
    }

    fun setEnabled(providerId: String, enabled: Boolean) {
        require(providerId in ManaCloudCredentialStore.PROVIDER_IDS)
        require(!enabled || credentials.get(providerId) != null) { "Save this provider's API key first." }
        preferences.edit().putBoolean("enabled.$providerId", enabled).apply()
    }

    fun providers(): List<InferenceProvider> = listOf(
        CloudAdapter("openai", "https://api.openai.com/v1/chat/completions", "gpt-4.1-mini", credentials, ::isEnabled),
        CloudAdapter("anthropic", "https://api.anthropic.com/v1/messages", "claude-3-5-haiku-latest", credentials, ::isEnabled),
        CloudAdapter("google", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent", "", credentials, ::isEnabled),
        CloudAdapter("xai", "https://api.x.ai/v1/chat/completions", "grok-3-mini", credentials, ::isEnabled)
    )

    private class CloudAdapter(
        override val id: String,
        private val endpoint: String,
        private val model: String,
        private val credentials: ManaCloudCredentialStore,
        private val enabled: (String) -> Boolean
    ) : InferenceProvider {
        override fun isAvailable(): Boolean = enabled(id) && credentials.get(id) != null

        override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
            require(isAvailable()) { "Cloud provider $id is disabled or has no saved key." }
            require(prompt.isNotBlank() && prompt.length <= 120_000) { "Prompt length is invalid." }
            val key = credentials.get(id) ?: error("Provider key is unavailable.")
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 90_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                when (id) {
                    "anthropic" -> {
                        setRequestProperty("x-api-key", key)
                        setRequestProperty("anthropic-version", "2023-06-01")
                    }
                    "google" -> setRequestProperty("x-goog-api-key", key)
                    else -> setRequestProperty("Authorization", "Bearer $key")
                }
            }
            try {
                val payload = when (id) {
                    "anthropic" -> JSONObject()
                        .put("model", model).put("max_tokens", 2048)
                        .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
                    "google" -> JSONObject().put(
                        "contents",
                        JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))))
                    )
                    else -> JSONObject().put("model", model).put(
                        "messages",
                        JSONArray().put(JSONObject().put("role", "user").put("content", prompt))
                    )
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val body = stream?.use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= 2 * 1024 * 1024) { "Cloud response exceeded 2 MiB." }
                        output.write(buffer, 0, read)
                    }
                    String(output.toByteArray(), Charsets.UTF_8)
                }.orEmpty()
                if (status !in 200..299) {
                    throw IllegalStateException("Cloud provider $id returned HTTP $status: ${body.take(500)}")
                }
                extractText(id, JSONObject(body)).also { require(it.isNotBlank()) { "Cloud provider returned an empty response." } }
            } finally {
                connection.disconnect()
            }
        }

        private fun extractText(provider: String, response: JSONObject): String = when (provider) {
            "anthropic" -> {
                val blocks = response.optJSONArray("content") ?: JSONArray()
                (0 until blocks.length()).mapNotNull { blocks.optJSONObject(it)?.takeIf { b -> b.optString("type") == "text" }?.optString("text") }
                    .joinToString("")
            }
            "google" -> {
                val candidates = response.optJSONArray("candidates") ?: JSONArray()
                val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
                (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text") }.joinToString("")
            }
            else -> {
                val choices = response.optJSONArray("choices") ?: JSONArray()
                choices.optJSONObject(0)?.optJSONObject("message")?.optString("content").orEmpty()
            }
        }
    }
}
