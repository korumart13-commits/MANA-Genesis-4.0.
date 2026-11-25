package com.mana.os.core

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class ManaDiaryEntry(
    val id: String,
    val title: String,
    val body: String,
    val tags: Set<String>,
    val createdAt: Instant,
    val updatedAt: Instant
)

interface ManaDiaryStore {
    fun readAll(): List<ManaDiaryEntry>
    fun writeAll(entries: List<ManaDiaryEntry>)
}

fun interface ManaSecretKeyProvider {
    fun getOrCreate(alias: String): SecretKey
}

/**
 * AES-256-GCM encrypted diary file store. Production Android integration must
 * provide a key provider backed by Android Keystore; keys are never written here.
 */
class EncryptedFileManaDiaryStore(
    private val file: File,
    private val keys: ManaSecretKeyProvider,
    private val keyAlias: String = KEY_ALIAS
) : ManaDiaryStore {
    private val lock = Any()
    private val random = SecureRandom()
    private val magic = byteArrayOf(0x4d, 0x41, 0x4e, 0x41, 0x44, 0x31)

    override fun readAll(): List<ManaDiaryEntry> = synchronized(lock) {
        if (!file.isFile) return@synchronized emptyList()
        require(file.length() <= MAX_FILE_BYTES) { "Diary file exceeds the safe size limit" }
        val payload = file.readBytes()
        require(payload.size > magic.size + 12 + 16) { "Diary file is truncated" }
        require(payload.copyOfRange(0, magic.size).contentEquals(magic)) { "Unknown diary file format" }
        val nonce = payload.copyOfRange(magic.size, magic.size + 12)
        val encrypted = payload.copyOfRange(magic.size + 12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keys.getOrCreate(keyAlias), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val clear = cipher.doFinal(encrypted)
        DataInputStream(ByteArrayInputStream(clear)).use { input ->
            val count = input.readInt()
            require(count in 0..MAX_ENTRIES) { "Diary entry count is invalid" }
            List(count) {
                val id = input.readUTF()
                val title = input.readUTF()
                val body = input.readUTF()
                val tagCount = input.readInt()
                require(tagCount in 0..MAX_TAGS) { "Diary tag count is invalid" }
                val tags = buildSet { repeat(tagCount) { add(input.readUTF()) } }
                val created = Instant.ofEpochMilli(input.readLong())
                val updated = Instant.ofEpochMilli(input.readLong())
                ManaDiaryEntry(id, title, body, tags, created, updated)
            }.also { require(input.available() == 0) { "Unexpected trailing diary data" } }
        }
    }

    override fun writeAll(entries: List<ManaDiaryEntry>) = synchronized(lock) {
        require(entries.size <= MAX_ENTRIES) { "Diary entry limit exceeded" }
        val clearBytes = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(entries.size)
                entries.forEach { entry ->
                    require(entry.title.length <= MAX_TEXT && entry.body.length <= MAX_TEXT && entry.id.length <= 128) { "Diary text is too long" }
                    require(entry.tags.size <= MAX_TAGS) { "Too many diary tags" }
                    output.writeUTF(entry.id)
                    output.writeUTF(entry.title)
                    output.writeUTF(entry.body)
                    output.writeInt(entry.tags.size)
                    entry.tags.sorted().forEach { tag ->
                        require(tag.length <= MAX_TAG_LENGTH) { "Diary tag is too long" }
                        output.writeUTF(tag)
                    }
                    output.writeLong(entry.createdAt.toEpochMilli())
                    output.writeLong(entry.updatedAt.toEpochMilli())
                }
            }
            bytes.toByteArray().also { require(it.size <= MAX_FILE_BYTES) { "Diary data exceeds the safe size limit" } }
        }
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keys.getOrCreate(keyAlias), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val encrypted = cipher.doFinal(clearBytes)
        val parent = file.absoluteFile.parentFile ?: error("Diary path has no parent")
        require(parent.exists() || parent.mkdirs()) { "Cannot create diary directory" }
        val temp = File.createTempFile("mana-diary-", ".part", parent)
        try {
            FileOutputStream(temp).use { output ->
                output.write(magic)
                output.write(nonce)
                output.write(encrypted)
                output.fd.sync()
            }
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
        Unit
    }

    companion object {
        private const val KEY_ALIAS = "mana.diary.v1"
        private const val MAX_ENTRIES = 1000
        private const val MAX_TAGS = 32
        private const val MAX_TAG_LENGTH = 256
        private const val MAX_TEXT = 10000
        private const val MAX_FILE_BYTES = 64 * 1024 * 1024
    }
}

class ManaDiaryService(private val store: ManaDiaryStore) {
    private val lock = Any()
    private val entries = store.readAll().associateByTo(linkedMapOf()) { it.id }

    fun create(title: String, body: String, tags: Set<String> = emptySet(), now: Instant = Instant.now()): ManaDiaryEntry =
        synchronized(lock) {
            require(title.isNotBlank() && body.isNotBlank())
            val entry = ManaDiaryEntry(java.util.UUID.randomUUID().toString(), title, body, tags, now, now)
            entries[entry.id] = entry
            persist()
            entry
        }

    fun update(id: String, title: String, body: String, tags: Set<String>, now: Instant = Instant.now()): ManaDiaryEntry =
        synchronized(lock) {
            val previous = entries[id] ?: error("Diary entry not found")
            require(title.isNotBlank() && body.isNotBlank())
            val updated = previous.copy(title = title, body = body, tags = tags, updatedAt = now)
            entries[id] = updated
            persist()
            updated
        }

    fun search(query: String): List<ManaDiaryEntry> = synchronized(lock) {
        val term = query.trim()
        if (term.isEmpty()) entries.values.sortedByDescending { it.updatedAt }
        else entries.values.filter { entry ->
            entry.title.contains(term, true) || entry.body.contains(term, true) ||
                entry.tags.any { it.contains(term, true) }
        }.sortedByDescending { it.updatedAt }
    }

    fun exportText(): String = synchronized(lock) {
        entries.values.sortedBy { it.createdAt }.joinToString("\n\n") { entry ->
            buildString {
                append("# ").append(entry.title).append('\n')
                append("Date: ").append(entry.createdAt).append('\n')
                if (entry.tags.isNotEmpty()) append("Tags: ").append(entry.tags.sorted().joinToString(", ")).append('\n')
                append('\n').append(entry.body)
            }
        }
    }

    fun delete(id: String, approvedByUser: Boolean) = synchronized(lock) {
        require(approvedByUser) { "Explicit owner approval is required to delete a diary entry" }
        require(entries.remove(id) != null) { "Diary entry not found" }
        persist()
    }

    private fun persist() = store.writeAll(entries.values.toList())
}

data class ManaMemoryItem(
    val id: String,
    val category: String,
    val content: String,
    val sensitivity: DataSensitivity,
    val createdAt: Instant,
    val expiresAt: Instant? = null,
    val pinned: Boolean = false
)

interface ManaMemoryStore {
    fun readAll(): List<ManaMemoryItem>
    fun writeAll(items: List<ManaMemoryItem>)
}

/** Encrypted file-backed memory store using the same authenticated AES-GCM envelope. */
class EncryptedFileManaMemoryStore(private val diaryStore: ManaDiaryStore) : ManaMemoryStore {
    override fun readAll(): List<ManaMemoryItem> = diaryStore.readAll().filter { "mana:memory" in it.tags }.map { entry ->
        val tags = entry.tags
        val sensitivity = tags.firstOrNull { it.startsWith("sensitivity:") }
            ?.removePrefix("sensitivity:")?.let { runCatching { DataSensitivity.valueOf(it) }.getOrNull() }
            ?: DataSensitivity.PERSONAL
        val expiry = tags.firstOrNull { it.startsWith("expires:") }?.removePrefix("expires:")?.toLongOrNull()
        ManaMemoryItem(
            id = entry.id,
            category = entry.title,
            content = entry.body,
            sensitivity = sensitivity,
            createdAt = entry.createdAt,
            expiresAt = expiry?.let(Instant::ofEpochMilli),
            pinned = "pinned:true" in tags
        )
    }

    override fun writeAll(items: List<ManaMemoryItem>) {
        diaryStore.writeAll(items.map { item ->
            ManaDiaryEntry(
                id = item.id,
                title = item.category,
                body = item.content,
                tags = setOf(
                    "mana:memory",
                    "sensitivity:${item.sensitivity.name}",
                    "pinned:${item.pinned}",
                    "expires:${item.expiresAt?.toEpochMilli() ?: "none"}"
                ),
                createdAt = item.createdAt,
                updatedAt = item.createdAt
            )
        })
    }
}

class ManaMemoryService(private val store: ManaMemoryStore) {
    private val items = store.readAll().associateByTo(linkedMapOf()) { it.id }

    @Synchronized fun remember(
        category: String,
        content: String,
        sensitivity: DataSensitivity = DataSensitivity.PERSONAL,
        expiresAt: Instant? = null,
        pinned: Boolean = false,
        now: Instant = Instant.now()
    ): ManaMemoryItem {
        require(category.isNotBlank() && content.isNotBlank())
        val item = ManaMemoryItem(java.util.UUID.randomUUID().toString(), category, content, sensitivity, now, expiresAt, pinned)
        items[item.id] = item
        persist()
        return item
    }

    @Synchronized fun recall(query: String = "", now: Instant = Instant.now()): List<ManaMemoryItem> {
        val term = query.trim()
        return items.values.filter { item ->
            (item.expiresAt == null || now.isBefore(item.expiresAt)) &&
                (term.isEmpty() || item.category.contains(term, true) || item.content.contains(term, true))
        }.sortedByDescending { it.createdAt }
    }

    @Synchronized fun expiredCandidates(now: Instant = Instant.now()): List<ManaMemoryItem> =
        items.values.filter { !it.pinned && it.expiresAt != null && !now.isBefore(it.expiresAt) }

    @Synchronized fun forget(id: String, approvedByUser: Boolean) {
        require(approvedByUser) { "Explicit owner approval is required to forget a memory" }
        require(items.remove(id) != null) { "Memory item not found" }
        persist()
    }

    private fun persist() = store.writeAll(items.values.toList())
}

data class ManaLifeTask(
    val id: String,
    val title: String,
    val dueAt: Instant?,
    val completed: Boolean = false,
    val requiresConfirmation: Boolean = false
)

interface ManaLifeTaskStore {
    fun readAll(): List<ManaLifeTask>
    fun writeAll(tasks: List<ManaLifeTask>)
}

/** Reuses the encrypted diary file format with a separate file/key alias per data domain. */
class EncryptedFileManaLifeTaskStore(private val diaryStore: ManaDiaryStore) : ManaLifeTaskStore {
    override fun readAll(): List<ManaLifeTask> = diaryStore.readAll().filter { "mana:life-task" in it.tags }.map { entry ->
        val tags = entry.tags
        val due = tags.firstOrNull { it.startsWith("due:") }?.removePrefix("due:")?.toLongOrNull()
        ManaLifeTask(
            id = entry.id,
            title = entry.body,
            dueAt = due?.let(Instant::ofEpochMilli),
            completed = "completed:true" in tags,
            requiresConfirmation = "confirm:true" in tags
        )
    }

    override fun writeAll(tasks: List<ManaLifeTask>) {
        diaryStore.writeAll(tasks.map { task ->
            ManaDiaryEntry(
                id = task.id,
                title = "MANA Life Task",
                body = task.title,
                tags = setOf(
                    "mana:life-task",
                    "completed:${task.completed}",
                    "confirm:${task.requiresConfirmation}",
                    "due:${task.dueAt?.toEpochMilli() ?: "none"}"
                ),
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        })
    }
}

class ManaLifeManagerService(private val store: ManaLifeTaskStore? = null) {
    private val tasks = store?.readAll()?.associateByTo(linkedMapOf()) { it.id } ?: linkedMapOf()

    @Synchronized fun add(title: String, dueAt: Instant? = null, requiresConfirmation: Boolean = false): ManaLifeTask {
        require(title.isNotBlank())
        val task = ManaLifeTask(java.util.UUID.randomUUID().toString(), title, dueAt, requiresConfirmation = requiresConfirmation)
        tasks[task.id] = task
        persist()
        return task
    }

    @Synchronized fun complete(id: String, approvedByUser: Boolean): ManaLifeTask {
        val task = tasks[id] ?: error("Task not found")
        if (task.requiresConfirmation) require(approvedByUser) { "Owner confirmation is required for this task" }
        return task.copy(completed = true).also { tasks[id] = it; persist() }
    }

    @Synchronized fun list(includeCompleted: Boolean = false): List<ManaLifeTask> =
        tasks.values.filter { includeCompleted || !it.completed }.sortedBy { it.dueAt ?: Instant.MAX }

    private fun persist() { store?.writeAll(tasks.values.toList()) }
}

data class ManaReceptionistSettings(
    val personality: String = "playful",
    val autoAnswerDelay: Duration = Duration.ofSeconds(10),
    val summarizeCalls: Boolean = true,
    val handleUnknownNumbers: Boolean = true
)

data class ManaCallDecision(
    val delay: Duration,
    val unknownCaller: Boolean,
    val offerSummary: Boolean,
    val requiresOwnerConsent: Boolean = true
)

/** Policy only: actual call screening/answering requires Android/OEM telephony roles and consent. */
class ManaSmartReceptionistPolicy(val settings: ManaReceptionistSettings = ManaReceptionistSettings()) {
    fun onIncomingCall(numberKnown: Boolean): ManaCallDecision = ManaCallDecision(
        delay = settings.autoAnswerDelay,
        unknownCaller = !numberKnown && settings.handleUnknownNumbers,
        offerSummary = settings.summarizeCalls,
        requiresOwnerConsent = true
    )
}

data class ManaGameSession(val gameId: String, val startedAt: Instant, val adapterId: String)
enum class ManaGameAction { READ_STATE, MOVE, INTERACT, BUILD, SEND_CHAT }

interface ManaGameAdapter {
    val id: String
    fun supports(gameId: String, action: ManaGameAction): Boolean
    fun execute(gameId: String, action: ManaGameAction, payload: String): String
}

/** Game actions are denied unless an installed adapter supports them and the owner approves. */
class ManaGameIntelligenceService(private val adapters: List<ManaGameAdapter>) {
    private val sessions = linkedMapOf<String, ManaGameSession>()

    @Synchronized fun beginSession(gameId: String, adapterId: String, now: Instant = Instant.now()): ManaGameSession {
        require(gameId.isNotBlank())
        val adapter = adapters.firstOrNull { it.id == adapterId } ?: error("Game adapter is not installed")
        require(adapter.supports(gameId, ManaGameAction.READ_STATE)) { "Adapter does not support this game" }
        return ManaGameSession(gameId, now, adapterId).also { sessions[gameId] = it }
    }

    @Synchronized fun perform(gameId: String, action: ManaGameAction, payload: String = "", ownerApproved: Boolean = false): String {
        val session = sessions[gameId] ?: error("No active game session")
        val adapter = adapters.firstOrNull { it.id == session.adapterId } ?: error("Game adapter disappeared")
        require(adapter.supports(gameId, action)) { "This game action is not supported by the installed adapter" }
        if (action != ManaGameAction.READ_STATE) require(ownerApproved) { "Owner approval is required for game actions" }
        return adapter.execute(gameId, action, payload)
    }

    @Synchronized fun endSession(gameId: String) { sessions.remove(gameId) }
    @Synchronized fun activeSessions(): List<ManaGameSession> = sessions.values.toList()
}

enum class ManaUpdateStage { IDEA, PLANNED, BUILDING, TESTED, OWNER_APPROVED, INSTALLED, REJECTED }
data class ManaUpdateIdea(val id: String, val description: String, val stage: ManaUpdateStage, val updatedAt: Instant)

interface ManaFutureUpdateStore {
    fun readAll(): List<ManaUpdateIdea>
    fun writeAll(ideas: List<ManaUpdateIdea>)
}

/** Stores future-update ideas in a dedicated encrypted diary-format file. */
class EncryptedFileManaFutureUpdateStore(private val backing: ManaDiaryStore) : ManaFutureUpdateStore {
    override fun readAll(): List<ManaUpdateIdea> = backing.readAll()
        .filter { "mana.future-update" in it.tags }
        .map { entry ->
            val stageName = entry.tags.firstOrNull { it.startsWith("stage:") }?.removePrefix("stage:")
                ?: ManaUpdateStage.IDEA.name
            ManaUpdateIdea(entry.id, entry.body, ManaUpdateStage.valueOf(stageName), entry.updatedAt)
        }

    override fun writeAll(ideas: List<ManaUpdateIdea>) {
        backing.writeAll(ideas.map { idea ->
            ManaDiaryEntry(
                id = idea.id,
                title = "MANA Future Update",
                body = idea.description,
                tags = setOf("mana.future-update", "stage:${idea.stage.name}"),
                createdAt = idea.updatedAt,
                updatedAt = idea.updatedAt
            )
        })
    }
}

class ManaFutureUpdateService(private val store: ManaFutureUpdateStore? = null) {
    private val ideas = linkedMapOf<String, ManaUpdateIdea>()

    init { store?.readAll()?.forEach { ideas[it.id] = it } }

    private fun persist() { store?.writeAll(ideas.values.toList()) }

    @Synchronized fun capture(description: String, now: Instant = Instant.now()): ManaUpdateIdea {
        require(description.isNotBlank())
        return ManaUpdateIdea(java.util.UUID.randomUUID().toString(), description, ManaUpdateStage.IDEA, now)
            .also { ideas[it.id] = it; persist() }
    }

    @Synchronized fun advance(id: String, target: ManaUpdateStage, ownerApproved: Boolean = false, now: Instant = Instant.now()): ManaUpdateIdea {
        val idea = ideas[id] ?: error("Update idea not found")
        val allowed = when (idea.stage) {
            ManaUpdateStage.IDEA -> target == ManaUpdateStage.PLANNED || target == ManaUpdateStage.REJECTED
            ManaUpdateStage.PLANNED -> target == ManaUpdateStage.BUILDING || target == ManaUpdateStage.REJECTED
            ManaUpdateStage.BUILDING -> target == ManaUpdateStage.TESTED || target == ManaUpdateStage.REJECTED
            ManaUpdateStage.TESTED -> target == ManaUpdateStage.OWNER_APPROVED || target == ManaUpdateStage.REJECTED
            ManaUpdateStage.OWNER_APPROVED -> target == ManaUpdateStage.INSTALLED || target == ManaUpdateStage.REJECTED
            ManaUpdateStage.INSTALLED, ManaUpdateStage.REJECTED -> false
        }
        require(allowed) { "Invalid update lifecycle transition: ${idea.stage} -> $target" }
        if (target == ManaUpdateStage.OWNER_APPROVED || target == ManaUpdateStage.INSTALLED || target == ManaUpdateStage.REJECTED) {
            require(ownerApproved) { "Explicit owner approval is required for this transition" }
        }
        return idea.copy(stage = target, updatedAt = now).also { ideas[id] = it; persist() }
    }

    @Synchronized fun delete(id: String, ownerApproved: Boolean = false) {
        require(ownerApproved) { "Explicit owner approval is required to delete an update idea" }
        require(ideas.remove(id) != null) { "Update idea not found" }
        persist()
    }

    @Synchronized fun list(): List<ManaUpdateIdea> = ideas.values.toList()
}
