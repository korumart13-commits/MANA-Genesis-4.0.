package com.mana.os.core

import java.net.URI
import java.time.Instant

data class ManaKnowledgeItem(
    val id: String,
    val title: String,
    val sourceUrl: String,
    val snippet: String,
    val savedAt: Instant
)

interface ManaAdaptiveKnowledgeStore {
    fun readAll(): List<ManaKnowledgeItem>
    fun writeAll(items: List<ManaKnowledgeItem>)
}

/** Encrypted persistence adapter using a dedicated EncryptedFileManaDiaryStore file and key alias. */
class EncryptedFileManaAdaptiveKnowledgeStore(private val backing: ManaDiaryStore) : ManaAdaptiveKnowledgeStore {
    override fun readAll(): List<ManaKnowledgeItem> = backing.readAll()
        .filter { "mana.adaptive-knowledge" in it.tags }
        .map { entry ->
            val separator = entry.body.indexOf('\n')
            require(separator > 0) { "Adaptive knowledge entry is malformed." }
            val url = entry.body.substring(0, separator)
            val snippet = entry.body.substring(separator + 1)
            ManaKnowledgeItem(entry.id, entry.title, url, snippet, entry.updatedAt)
        }

    override fun writeAll(items: List<ManaKnowledgeItem>) {
        require(items.size <= MAX_ITEMS) { "Adaptive knowledge item limit exceeded." }
        backing.writeAll(items.map { item ->
            ManaDiaryEntry(
                id = item.id,
                title = item.title,
                body = item.sourceUrl + "\n" + item.snippet,
                tags = setOf("mana.adaptive-knowledge"),
                createdAt = item.savedAt,
                updatedAt = item.savedAt
            )
        })
    }

    companion object { private const val MAX_ITEMS = 5000 }
}

/**
 * Owner-approved local knowledge cache. Web results remain untrusted source data;
 * they are never executed as instructions or silently promoted into model policy.
 */
class ManaAdaptiveKnowledgeService(private val store: ManaAdaptiveKnowledgeStore) {
    private val items = store.readAll().associateByTo(linkedMapOf()) { it.id }

    @Synchronized fun save(title: String, sourceUrl: String, snippet: String, ownerApproved: Boolean, now: Instant = Instant.now()): ManaKnowledgeItem {
        require(ownerApproved) { "Explicit owner approval is required to save web knowledge." }
        require(title.isNotBlank() && title.length <= 240)
        require(snippet.isNotBlank() && snippet.length <= 700)
        require(sourceUrl.length <= 2048) { "Source URL exceeds the storage limit." }
        val uri = URI(sourceUrl)
        require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null) { "Only HTTPS source URLs without embedded credentials can be saved." }
        val normalizedUrl = uri.toASCIIString()
        require(normalizedUrl.length <= 2048) { "Normalized source URL exceeds the storage limit." }
        val existing = items.values.firstOrNull { it.sourceUrl == normalizedUrl }
        val item = ManaKnowledgeItem(existing?.id ?: java.util.UUID.randomUUID().toString(),
            title.trim(), normalizedUrl, snippet.trim(), now)
        items[item.id] = item
        persist()
        return item
    }

    @Synchronized fun search(query: String, limit: Int = 5): List<ManaKnowledgeItem> {
        require(limit in 1..20)
        val terms = query.lowercase().split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 2 }.toSet()
        if (terms.isEmpty()) return emptyList()
        return items.values.map { item ->
            val haystack = (item.title + " " + item.snippet).lowercase()
            item to terms.count(haystack::contains)
        }.filter { it.second > 0 }.sortedWith(
            compareByDescending<Pair<ManaKnowledgeItem, Int>> { it.second }
                .thenByDescending { it.first.savedAt }
        ).take(limit).map { it.first }
    }

    @Synchronized fun list(): List<ManaKnowledgeItem> = items.values.sortedByDescending { it.savedAt }

    @Synchronized fun forget(id: String, ownerApproved: Boolean) {
        require(ownerApproved) { "Explicit owner approval is required to forget saved web knowledge." }
        require(items.remove(id) != null) { "Knowledge item not found." }
        persist()
    }

    private fun persist() = store.writeAll(items.values.toList())
}
