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
                    require(entry.title.length <= MAX_TEXT && entry.body.length <= MAX_TEXT) { "Diary text is too long" }
                    require(entry.tags.size <= MAX_TAGS) { "Too many diary tags" }
                    output.writeUTF(entry.id)
                    output.writeUTF(entry.title)
                    output.writeUTF(entry.body)
                    output.writeInt(entry.tags.size)
                    entry.tags.sorted().forEach(output::writeUTF)
                    output.writeLong(entry.createdAt.toEpochMilli())
                    output.writeLong(entry.updatedAt.toEpochMilli())
                }
            }
            bytes.toByteArray()
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
        private const val MAX_ENTRIES = 10000
        private const val MAX_TAGS = 32
        private const val MAX_TEXT = 60000
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

class ManaFutureUpdateService {
    private val ideas = linkedMapOf<String, ManaUpdateIdea>()

    @Synchronized fun capture(description: String, now: Instant = Instant.now()): ManaUpdateIdea {
        require(description.isNotBlank())
        return ManaUpdateIdea(java.util.UUID.randomUUID().toString(), description, ManaUpdateStage.IDEA, now)
            .also { ideas[it.id] = it }
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
        return idea.copy(stage = target, updatedAt = now).also { ideas[id] = it }
    }

    @Synchronized fun list(): List<ManaUpdateIdea> = ideas.values.toList()
}
