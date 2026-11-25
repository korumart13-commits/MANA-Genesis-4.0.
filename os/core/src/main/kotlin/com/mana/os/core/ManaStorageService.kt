package com.mana.os.core

import java.time.Duration
import java.time.Instant

enum class StorageDisposition { ACTIVE, ARCHIVED, QUARANTINED }
enum class StorageAction { KEEP, ARCHIVE, QUARANTINE, DELETE }

data class StorageItem(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val lastAccessedAt: Instant,
    val sensitivity: DataSensitivity = DataSensitivity.PERSONAL,
    val userPinned: Boolean = false,
    val disposition: StorageDisposition = StorageDisposition.ACTIVE
) {
    init {
        require(id.isNotBlank() && displayName.isNotBlank())
        require(sizeBytes >= 0)
    }
}

data class StorageRecommendation(
    val itemId: String,
    val action: StorageAction,
    val reason: String,
    val reclaimableBytes: Long
)

data class StorageOperation(
    val itemId: String,
    val from: StorageDisposition,
    val to: StorageDisposition,
    val at: Instant = Instant.now()
)

/**
 * Safe metadata-only storage planner. It never reads, moves, or deletes real files.
 * Destructive deletion is deliberately not implemented.
 */
class ManaStorageService(private val archiveAfter: Duration = Duration.ofDays(90)) {
    private val items = linkedMapOf<String, StorageItem>()
    private val history = mutableListOf<StorageOperation>()

    @Synchronized
    fun register(item: StorageItem) {
        require(item.id !in items) { "Storage item already registered: ${item.id}" }
        items[item.id] = item
    }

    @Synchronized
    fun inventory(): List<StorageItem> = items.values.toList()

    @Synchronized
    fun recommend(itemId: String, now: Instant = Instant.now()): StorageRecommendation {
        val item = items[itemId] ?: error("Unknown storage item: $itemId")
        val age = Duration.between(item.lastAccessedAt, now)
        val action = when {
            item.userPinned -> StorageAction.KEEP
            item.sensitivity == DataSensitivity.SECRET -> StorageAction.KEEP
            item.disposition != StorageDisposition.ACTIVE -> StorageAction.KEEP
            age >= archiveAfter -> StorageAction.ARCHIVE
            else -> StorageAction.KEEP
        }
        val reason = when (action) {
            StorageAction.ARCHIVE -> "Inactive beyond archive threshold; reversible archive suggested"
            StorageAction.KEEP -> if (item.userPinned) "Pinned by user" else
                if (item.sensitivity == DataSensitivity.SECRET) "Secret data is protected from automatic handling" else
                "No safe cleanup action is currently justified"
            StorageAction.QUARANTINE -> "Quarantine requires explicit review"
            StorageAction.DELETE -> "Deletion is not supported"
        }
        return StorageRecommendation(itemId, action, reason,
            if (action == StorageAction.ARCHIVE) item.sizeBytes else 0L)
    }

    /**
     * Applies only reversible archive/quarantine after explicit user approval.
     * DELETE always fails, even when approved; permanent deletion requires a separate
     * future design with recovery guarantees and a distinct confirmation flow.
     */
    @Synchronized
    fun approveAndApply(
        itemId: String,
        action: StorageAction,
        approvedByUser: Boolean,
        now: Instant = Instant.now()
    ): StorageItem {
        require(approvedByUser) { "Explicit user approval is required" }
        require(action != StorageAction.DELETE) { "Permanent deletion is disabled" }
        val item = items[itemId] ?: error("Unknown storage item: $itemId")
        require(item.disposition == StorageDisposition.ACTIVE) { "Only active items can be moved" }
        require(!item.userPinned) { "Pinned items cannot be moved by this service" }
        val target = when (action) {
            StorageAction.ARCHIVE -> StorageDisposition.ARCHIVED
            StorageAction.QUARANTINE -> StorageDisposition.QUARANTINED
            StorageAction.KEEP -> return item
            StorageAction.DELETE -> error("Deletion is disabled")
        }
        val updated = item.copy(disposition = target)
        items[itemId] = updated
        history += StorageOperation(itemId, item.disposition, target, now)
        return updated
    }

    @Synchronized
    fun restore(itemId: String, approvedByUser: Boolean, now: Instant = Instant.now()): StorageItem {
        require(approvedByUser) { "Explicit user approval is required to restore" }
        val item = items[itemId] ?: error("Unknown storage item: $itemId")
        require(item.disposition != StorageDisposition.ACTIVE) { "Item is already active" }
        val updated = item.copy(disposition = StorageDisposition.ACTIVE)
        items[itemId] = updated
        history += StorageOperation(itemId, item.disposition, StorageDisposition.ACTIVE, now)
        return updated
    }

    @Synchronized
    fun operationHistory(): List<StorageOperation> = history.toList()
}
