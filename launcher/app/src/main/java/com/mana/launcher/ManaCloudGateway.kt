package com.mana.launcher

import android.content.Context
import com.mana.brain.InferenceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

enum class ManaCloudProvider(val title: String) {
    OPENAI("OpenAI"),
    ANTHROPIC("Anthropic Claude"),
    GEMINI("Google Gemini"),
    XAI("xAI Grok")
}

/** Explicitly enabled cloud fallback. Local LiteRT-LM remains first priority. */
class ManaCloudGateway(
    context: Context,
    private val keyProvider: ManaAndroidKeyProvider
) : InferenceProvider {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("mana-cloud-settings", Context.MODE_PRIVATE)
    private val keyFile = File(app.filesDir, "mana/cloud-keys.bin")
    private val random = SecureRandom()

    override val id: String get() = "cloud-${selectedProvider().name.lowercase()}"

    fun selectedProvider(): ManaCloudProvider =
        runCatching { ManaCloudProvider.valueOf(prefs.getString(KEY_PROVIDER, ManaCloudProvider.OPENAI.name)!!) }
            .getOrDefault(ManaCloudProvider.OPENAI)

    fun model(): String = prefs.getString(KEY_MODEL, "") ?: ""
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)
    fun hasKey(provider: ManaCloudProvider = selectedProvider()): Boolean =
        runCatching { readKeys().has(provider.name) }.getOrDefault(false)

    fun configure(provider: ManaCloudProvider, model: String, apiKey: String, enabled: Boolean) {
        require(model.matches(Regex("[A-Za-z0-9._:-]{2,100}"))) { "Укажи корректный идентификатор модели." }
        if (apiKey.isNotBlank()) {
            require(apiKey.length in 12..4096) { "Проверь API-ключ: его длина выглядит некорректной." }
            val keys = readKeys().put(provider.name, apiKey)
            writeKeys(keys)
        } else {
            require(hasKey(provider)) { "Для этого провайдера нужен API-ключ." }
        }
        prefs.edit().putString(KEY_PROVIDER, provider.name).putString(KEY_MODEL, model)
            .putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun setEnabled(enabled: Boolean) { prefs.edit().putBoolean(KEY_ENABLED, enabled).apply() }

    fun removeKey(provider: ManaCloudProvider = selectedProvider()) {
        val keys = readKeys()
        keys.remove(provider.name)
        writeKeys(keys)
        if (provider == selectedProvider()) setEnabled(false)
    }

    override fun isAvailable(): Boolean = isEnabled() && model().isNotBlank() && hasKey()

    override suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        require(isAvailable()) { "Облачный резерв не настроен или выключен." }
        require(prompt.isNotBlank() && prompt.length <= 100_000) { "Запрос пустой или слишком большой." }
        val provider = selectedProvider()
        val modelId = model()
        require(modelId.matches(Regex("[A-Za-z0-9._:-]{2,100}"))) { "Некорректный идентификатор модели." }
        val apiKey = readKeys().optString(provider.name, "")
        require(apiKey.isNotBlank()) { "API-ключ не найден." }

        val endpoint: String
        val body: JSONObject
        when (provider) {
            ManaCloudProvider.OPENAI, ManaCloudProvider.XAI -> {
                endpoint = if (provider == ManaCloudProvider.OPENAI)
                    "https://api.openai.com/v1/responses"
                else "https://api.x.ai/v1/responses"
                val input = JSONArray()
                    .put(JSONObject().put("role", "system").put(
                        "content",
                        ManaPersona.SYSTEM_PROMPT
                    ))
                    .put(JSONObject().put("role", "user").put("content", prompt))
                body = JSONObject()
                    .put("model", modelId)
                    .put("input", input)
                    .put("max_output_tokens", 2048)
                    .put("store", false)
            }
            ManaCloudProvider.ANTHROPIC -> {
                endpoint = "https://api.anthropic.com/v1/messages"
                body = JSONObject().put("model", modelId).put("max_tokens", 2048)
                    .put("system", ManaPersona.SYSTEM_PROMPT)
                    .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            }
            ManaCloudProvider.GEMINI -> {
                endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelId:generateContent"
                body = JSONObject()
                    .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", ManaPersona.SYSTEM_PROMPT))))
                    .put("contents", JSONArray().put(JSONObject()
                        .put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                    .put("generationConfig", JSONObject().put("maxOutputTokens", 2048))
            }
        }

        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 180_000
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            when (provider) {
                ManaCloudProvider.OPENAI, ManaCloudProvider.XAI ->
                    connection.setRequestProperty("Authorization", "Bearer $apiKey")
                ManaCloudProvider.ANTHROPIC -> {
                    connection.setRequestProperty("x-api-key", apiKey)
                    connection.setRequestProperty("anthropic-version", "2023-06-01")
                }
                ManaCloudProvider.GEMINI -> connection.setRequestProperty("x-goog-api-key", apiKey)
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(StandardCharsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val raw = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { reader ->
                val out = StringBuilder()
                val buffer = CharArray(8192)
                while (true) {
                    val n = reader.read(buffer)
                    if (n < 0) break
                    require(out.length + n <= 2_000_000) { "Ответ провайдера превышает 2 МБ." }
                    out.append(buffer, 0, n)
                }
                out.toString()
            }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("$provider вернул HTTP $status. Проверь ключ, идентификатор модели, квоту и настройки провайдера.")
            }
            val json = JSONObject(raw)
            val answer = when (provider) {
                ManaCloudProvider.OPENAI, ManaCloudProvider.XAI -> {
                    val output = json.getJSONArray("output")
                    buildString {
                        for (i in 0 until output.length()) {
                            val item = output.getJSONObject(i)
                            if (item.optString("type") != "message") continue
                            val blocks = item.optJSONArray("content") ?: continue
                            for (j in 0 until blocks.length()) {
                                val block = blocks.getJSONObject(j)
                                if (block.optString("type") == "output_text") append(block.optString("text"))
                            }
                        }
                    }
                }
                ManaCloudProvider.ANTHROPIC -> {
                    val blocks = json.getJSONArray("content")
                    (0 until blocks.length()).mapNotNull { i ->
                        blocks.getJSONObject(i).takeIf { it.optString("type") == "text" }?.optString("text")
                    }.joinToString("")
                }
                ManaCloudProvider.GEMINI -> {
                    val parts = json.getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts")
                    (0 until parts.length()).joinToString("") { i -> parts.getJSONObject(i).optString("text") }
                }
            }
            require(answer.isNotBlank()) { "Провайдер вернул пустой ответ." }
            answer
        } finally {
            connection.disconnect()
        }
    }

    private fun readKeys(): JSONObject {
        if (!keyFile.isFile) return JSONObject()
        val payload = keyFile.readBytes()
        require(payload.size in 29..128_000) { "Файл облачных ключей повреждён." }
        val nonce = payload.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyProvider.getOrCreate(KEY_ALIAS), GCMParameterSpec(128, nonce))
        cipher.updateAAD(AAD)
        return JSONObject(String(cipher.doFinal(payload.copyOfRange(12, payload.size)), StandardCharsets.UTF_8))
    }

    private fun writeKeys(keys: JSONObject) {
        val parent = keyFile.absoluteFile.parentFile ?: error("Нет каталога для ключей.")
        require(parent.exists() || parent.mkdirs()) { "Не удалось создать каталог ключей." }
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreate(KEY_ALIAS), GCMParameterSpec(128, nonce))
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(keys.toString().toByteArray(StandardCharsets.UTF_8))
        val temp = File.createTempFile("mana-cloud-", ".part", parent)
        try {
            java.io.FileOutputStream(temp).use { out ->
                out.write(nonce); out.write(encrypted); out.fd.sync()
            }
            try {
                Files.move(temp.toPath(), keyFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), keyFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { if (temp.exists()) temp.delete() }
    }

    companion object {
        private const val KEY_ALIAS = "mana.cloud.gateway.v1"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_MODEL = "model"
        private const val KEY_ENABLED = "enabled"
        private val AAD = "MANA-CLOUD-KEYS-v1".toByteArray(StandardCharsets.UTF_8)
    }
}
