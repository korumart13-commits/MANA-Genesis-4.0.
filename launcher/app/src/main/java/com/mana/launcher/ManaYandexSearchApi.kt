package com.mana.launcher

import android.util.Base64
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL

data class ManaYandexSearchResult(val title: String, val url: String, val snippet: String)

internal fun buildYandexSearchRequest(query: String, folderId: String, maxResults: Int): JSONObject {
    require(query.isNotBlank() && query.length <= 400)
    require(folderId.isNotBlank() && folderId.length <= 50)
    require(maxResults in 1..10)
    return JSONObject()
        .put("query", JSONObject()
            .put("searchType", "SEARCH_TYPE_RU")
            .put("queryText", query)
            .put("familyMode", "FAMILY_MODE_STRICT")
            .put("page", "0")
            .put("fixTypoMode", "FIX_TYPO_MODE_ON"))
        .put("folderId", folderId)
        .put("groupSpec", JSONObject()
            .put("groupMode", "GROUP_MODE_DEEP")
            .put("groupsOnPage", maxResults.toString())
            .put("docsInGroup", "1"))
        .put("maxPassages", "2")
        .put("l10n", "LOCALIZATION_RU")
        .put("responseFormat", "FORMAT_XML")
}

/** Explicit, owner-triggered Yandex Search API v2 client for MANA Adaptive Intelligence. */
class ManaYandexSearchApi(private val credentials: ManaYandexAiStudioProvider) {
    suspend fun search(query: String, maxResults: Int = 5): List<ManaYandexSearchResult> = withContext(Dispatchers.IO) {
        require(query.isNotBlank() && query.length <= 400)
        require(maxResults in 1..10)
        val config = credentials.currentConfig() ?: error("Сначала настрой Yandex AI Studio.")
        require(config.enabled && config.apiKey.isNotBlank()) { "Yandex AI Studio выключен." }
        val body = buildYandexSearchRequest(query, config.folderId, maxResults)
        val connection = URL(ENDPOINT).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 45_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Api-Key ${config.apiKey}")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val response = readBounded(if (status in 200..299) connection.inputStream else connection.errorStream)
            check(status in 200..299) { "Yandex Search API returned HTTP $status." }
            val raw = JSONObject(response).optString("rawData")
            require(raw.isNotBlank()) { "Yandex Search API returned no result payload." }
            val decoded = Base64.decode(raw, Base64.DEFAULT).toString(Charsets.UTF_8)
            parseSmartSnippetJson(decoded, maxResults) ?: parseXml(decoded, maxResults)
        } finally { connection.disconnect() }
    }

    private fun parseSmartSnippetJson(raw: String, maxResults: Int): List<ManaYandexSearchResult>? = runCatching {
        val docs = JSONObject(raw).optJSONArray("docs") ?: return null
        buildList {
            for (index in 0 until minOf(docs.length(), maxResults)) {
                val doc = docs.optJSONObject(index) ?: continue
                val title = doc.optString("DocumentTitle").ifBlank { "Yandex Search result" }
                val url = doc.optString("FullUrl")
                val snippet = doc.optString("info_context").ifBlank { doc.optString("Description") }
                if (url.startsWith("https://") && snippet.isNotBlank()) {
                    add(ManaYandexSearchResult(title.take(240), url.take(2048), snippet.take(4000)))
                }
            }
        }
    }.getOrNull()

    private fun parseXml(xml: String, maxResults: Int): List<ManaYandexSearchResult> {
        require(xml.length <= MAX_XML_CHARS) { "Yandex Search XML exceeded the safety limit." }
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
        parser.setInput(StringReader(xml))
        val results = mutableListOf<ManaYandexSearchResult>()
        var inDoc = false
        var activeTag: String? = null
        var title = StringBuilder()
        var link = StringBuilder()
        var snippet = StringBuilder()
        while (parser.eventType != XmlPullParser.END_DOCUMENT && results.size < maxResults) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == "doc") {
                        inDoc = true
                        title = StringBuilder(); link = StringBuilder(); snippet = StringBuilder()
                    } else if (inDoc && tag in setOf("title", "url", "passage", "headline")) activeTag = tag
                }
                XmlPullParser.TEXT -> if (inDoc) {
                    when (activeTag) {
                        "title" -> title.append(parser.text)
                        "url" -> link.append(parser.text)
                        "passage", "headline" -> snippet.append(parser.text).append(' ')
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tag = parser.name.lowercase()
                    if (tag == activeTag) activeTag = null
                    if (tag == "doc" && inDoc) {
                        val safeUrl = link.toString().trim()
                        if (safeUrl.startsWith("https://") && title.isNotBlank()) {
                            results += ManaYandexSearchResult(title.toString().trim().take(240), safeUrl.take(2048), snippet.toString().trim().take(4000))
                        }
                        inDoc = false
                    }
                }
            }
            parser.next()
        }
        return results
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
                require(total <= limit) { "Yandex Search response exceeded 2 MiB." }
                output.write(buffer, 0, count)
            }
            return output.toString("UTF-8")
        }
    }

    companion object {
        private const val ENDPOINT = "https://searchapi.api.cloud.yandex.net/v2/web/search"
        private const val MAX_XML_CHARS = 2 * 1024 * 1024
    }
}
