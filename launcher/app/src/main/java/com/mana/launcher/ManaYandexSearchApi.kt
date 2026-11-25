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

/** Explicit, owner-triggered Yandex Search API v2 client for MANA Adaptive Intelligence. */
class ManaYandexSearchApi(private val credentials: ManaYandexAiStudioProvider) {
    suspend fun search(query: String, maxResults: Int = 5): List<ManaYandexSearchResult> = withContext(Dispatchers.IO) {
        require(query.isNotBlank() && query.length <= 512)
        require(maxResults in 1..10)
        val config = credentials.currentConfig() ?: error("Сначала настрой Yandex AI Studio.")
        require(config.enabled && config.apiKey.isNotBlank()) { "Yandex AI Studio выключен." }
        val body = JSONObject()
            .put("query", JSONObject()
                .put("searchType", "SEARCH_TYPE_COM")
                .put("queryText", query)
                .put("familyMode", "strict")
                .put("fixTypoMode", "AUTO"))
            .put("folderId", config.folderId)
            .put("groupSpec", JSONObject().put("groupsOnPage", maxResults.toString()).put("docsInGroup", "1"))
            .put("maxPassages", "2")
            .put("l10n", "LOCALIZATION_RU")
            .put("responseFormat", "FORMAT_XML")
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
            val xml = Base64.decode(raw, Base64.DEFAULT).toString(Charsets.UTF_8)
            parseXml(xml, maxResults)
        } finally { connection.disconnect() }
    }

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
                            results += ManaYandexSearchResult(title.toString().trim().take(240), safeUrl.take(2048), snippet.toString().trim().take(700))
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
