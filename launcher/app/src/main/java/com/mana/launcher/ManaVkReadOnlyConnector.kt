package com.mana.launcher

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class ManaVkProfile(val id: Long, val firstName: String, val lastName: String, val photoUrl: String?)
data class ManaVkWallPost(val id: Long, val text: String, val dateEpochSeconds: Long)

/** Narrow read-only VK adapter. It exposes only users.get and wall.get; no write methods exist. */
class ManaVkReadOnlyConnector(context: Context) {
    private val vault = ManaEncryptedCredentialStore(context, "vk-readonly.bin", "mana.vk.credentials.v1")

    fun configureAccessToken(accessToken: String) {
        require(accessToken.length in 20..4096) { "VK access token is invalid." }
        vault.write(accessToken)
    }

    fun isConfigured(): Boolean = runCatching { !vault.read().isNullOrBlank() }.getOrDefault(false)
    fun removeCredentials() = vault.clear()

    suspend fun readProfile(userId: String? = null): ManaVkProfile = withContext(Dispatchers.IO) {
        val params = linkedMapOf("fields" to "photo_200", "v" to API_VERSION)
        if (!userId.isNullOrBlank()) params["user_ids"] = userId
        val response = callReadOnly("users.get", params).getJSONArray("response")
        val profile = response.getJSONObject(0)
        ManaVkProfile(
            id = profile.getLong("id"),
            firstName = profile.optString("first_name"),
            lastName = profile.optString("last_name"),
            photoUrl = profile.optString("photo_200").takeIf { it.startsWith("https://") }
        )
    }

    suspend fun readOwnWall(count: Int = 10): List<ManaVkWallPost> = withContext(Dispatchers.IO) {
        require(count in 1..20)
        val response = callReadOnly("wall.get", mapOf("count" to count.toString(), "filter" to "owner", "v" to API_VERSION))
            .getJSONObject("response").getJSONArray("items")
        buildList {
            for (i in 0 until response.length()) {
                val item = response.getJSONObject(i)
                add(ManaVkWallPost(item.getLong("id"), item.optString("text"), item.optLong("date")))
            }
        }
    }

    private fun callReadOnly(method: String, params: Map<String, String>): JSONObject {
        require(method in ALLOWED_METHODS) { "VK method is not in the read-only allowlist." }
        val token = vault.read() ?: error("VK is not connected.")
        val connection = URL("https://api.vk.com/method/$method").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            val form = (params + ("access_token" to token)).entries.joinToString("&") {
                URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
            }
            connection.outputStream.use { it.write(form.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in 200..299) { "VK API returned HTTP $code." }
            val json = JSONObject(body)
            require(!json.has("error")) { "VK API error: " + json.optJSONObject("error")?.optInt("error_code") }
            return json
        } finally { connection.disconnect() }
    }

    companion object {
        private const val API_VERSION = "5.199"
        private val ALLOWED_METHODS = setOf("users.get", "wall.get")
    }
}
