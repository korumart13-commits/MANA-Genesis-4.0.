package com.mana.os.core

import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

enum class MicroworldRunState { ACTIVE, PAUSED }
enum class MicroworldEvidenceKind { USER_INPUT, LOCAL_OBSERVATION, VERIFIED_RESEARCH, SIMULATION }
enum class MicroworldExperimentState { DRAFT, COMPLETED, ABORTED }

data class MicroworldSpace(
    val id: String,
    val name: String,
    val description: String,
    val parentId: String?,
    val createdAt: Instant
)

data class MicroworldObject(
    val id: String,
    val spaceId: String,
    val name: String,
    val description: String,
    val tags: Set<String>,
    val createdAt: Instant
)

data class MicroworldObservation(
    val id: String,
    val subjectId: String?,
    val kind: MicroworldEvidenceKind,
    val statement: String,
    val source: String,
    val observedAt: Instant
)

data class MicroworldExperiment(
    val id: String,
    val spaceId: String,
    val hypothesis: String,
    val procedure: String,
    val state: MicroworldExperimentState,
    val outcome: String?,
    val createdAt: Instant,
    val completedAt: Instant?
)

data class MicroworldSnapshot(
    val revision: Long = 0,
    val runState: MicroworldRunState = MicroworldRunState.ACTIVE,
    val spaces: List<MicroworldSpace> = emptyList(),
    val objects: List<MicroworldObject> = emptyList(),
    val observations: List<MicroworldObservation> = emptyList(),
    val experiments: List<MicroworldExperiment> = emptyList()
)

data class MicroworldLimits(
    val maxSpaces: Int = 128,
    val maxObjects: Int = 2_000,
    val maxObservations: Int = 10_000,
    val maxExperiments: Int = 500,
    val maxTextLength: Int = 4_000,
    val maxTagsPerObject: Int = 24
) {
    init {
        require(maxSpaces > 0 && maxObjects > 0 && maxObservations > 0 && maxExperiments > 0)
        require(maxTextLength in 64..32_000 && maxTagsPerObject in 1..128)
    }
}

interface MicroworldStore {
    fun load(): MicroworldSnapshot
    fun save(snapshot: MicroworldSnapshot)
}

/** In-memory adapter for deterministic tests and previews. */
class InMemoryMicroworldStore(initial: MicroworldSnapshot = MicroworldSnapshot()) : MicroworldStore {
    private var value = initial
    @Synchronized override fun load() = value
    @Synchronized override fun save(snapshot: MicroworldSnapshot) { value = snapshot }
}

/**
 * Encrypted, atomic local snapshot store. The supplied key provider must use Android Keystore
 * in the Android product. This host implementation does not create or persist encryption keys.
 */
class EncryptedFileMicroworldStore(
    private val file: File,
    private val keys: ManaSecretKeyProvider,
    private val keyAlias: String = "mana.microworld.v1"
) : MicroworldStore {
    private val lock = Any()
    private val random = SecureRandom()
    private val magic = byteArrayOf(0x4d, 0x57, 0x4c, 0x44, 0x31)

    override fun load(): MicroworldSnapshot = synchronized(lock) {
        if (!file.isFile) return@synchronized MicroworldSnapshot()
        require(file.length() in (magic.size + NONCE_BYTES + TAG_BYTES + 1).toLong()..MAX_FILE_BYTES.toLong()) {
            "Microworld file size is invalid"
        }
        val bytes = file.readBytes()
        require(bytes.copyOfRange(0, magic.size).contentEquals(magic)) { "Unknown Microworld file format" }
        val nonce = bytes.copyOfRange(magic.size, magic.size + NONCE_BYTES)
        val encrypted = bytes.copyOfRange(magic.size + NONCE_BYTES, bytes.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keys.getOrCreate(keyAlias), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        decode(cipher.doFinal(encrypted))
    }

    override fun save(snapshot: MicroworldSnapshot) = synchronized(lock) {
        validateSnapshot(snapshot)
        val clear = encode(snapshot)
        require(clear.size <= MAX_FILE_BYTES) { "Microworld snapshot exceeds storage limit" }
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keys.getOrCreate(keyAlias), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val encrypted = cipher.doFinal(clear)
        val parent = file.absoluteFile.parentFile ?: error("Microworld path has no parent")
        require(parent.exists() || parent.mkdirs()) { "Cannot create Microworld directory" }
        val temp = File.createTempFile("mana-world-", ".part", parent)
        try {
            FileOutputStream(temp).use { out ->
                out.write(magic); out.write(nonce); out.write(encrypted); out.fd.sync()
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

    private fun encode(s: MicroworldSnapshot): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { out ->
            out.writeInt(FORMAT_VERSION); out.writeLong(s.revision); out.writeUTF(s.runState.name)
            out.writeInt(s.spaces.size)
            s.spaces.forEach { x -> out.writeUTF(x.id); out.writeUTF(x.name); out.writeUTF(x.description); out.writeNullable(x.parentId); out.writeLong(x.createdAt.toEpochMilli()) }
            out.writeInt(s.objects.size)
            s.objects.forEach { x -> out.writeUTF(x.id); out.writeUTF(x.spaceId); out.writeUTF(x.name); out.writeUTF(x.description); out.writeInt(x.tags.size); x.tags.sorted().forEach(out::writeUTF); out.writeLong(x.createdAt.toEpochMilli()) }
            out.writeInt(s.observations.size)
            s.observations.forEach { x -> out.writeUTF(x.id); out.writeNullable(x.subjectId); out.writeUTF(x.kind.name); out.writeUTF(x.statement); out.writeUTF(x.source); out.writeLong(x.observedAt.toEpochMilli()) }
            out.writeInt(s.experiments.size)
            s.experiments.forEach { x -> out.writeUTF(x.id); out.writeUTF(x.spaceId); out.writeUTF(x.hypothesis); out.writeUTF(x.procedure); out.writeUTF(x.state.name); out.writeNullable(x.outcome); out.writeLong(x.createdAt.toEpochMilli()); out.writeNullableLong(x.completedAt?.toEpochMilli()) }
        }
        bytes.toByteArray()
    }

    private fun decode(bytes: ByteArray): MicroworldSnapshot = DataInputStream(ByteArrayInputStream(bytes)).use { input ->
        require(input.readInt() == FORMAT_VERSION) { "Unsupported Microworld snapshot version" }
        val revision = input.readLong().also { require(it >= 0) }
        val state = MicroworldRunState.valueOf(input.readUTF())
        val spaces = List(input.readCount(MAX_SPACES)) { MicroworldSpace(input.readUTF(), input.readUTF(), input.readUTF(), input.readNullable(), Instant.ofEpochMilli(input.readLong())) }
        val objects = List(input.readCount(MAX_OBJECTS)) {
            val id = input.readUTF(); val space = input.readUTF(); val name = input.readUTF(); val description = input.readUTF()
            val tags = buildSet { repeat(input.readCount(MAX_TAGS)) { add(input.readUTF()) } }
            MicroworldObject(id, space, name, description, tags, Instant.ofEpochMilli(input.readLong()))
        }
        val observations = List(input.readCount(MAX_OBSERVATIONS)) { MicroworldObservation(input.readUTF(), input.readNullable(), MicroworldEvidenceKind.valueOf(input.readUTF()), input.readUTF(), input.readUTF(), Instant.ofEpochMilli(input.readLong())) }
        val experiments = List(input.readCount(MAX_EXPERIMENTS)) { MicroworldExperiment(input.readUTF(), input.readUTF(), input.readUTF(), input.readUTF(), MicroworldExperimentState.valueOf(input.readUTF()), input.readNullable(), Instant.ofEpochMilli(input.readLong()), input.readNullableLong()?.let(Instant::ofEpochMilli)) }
        require(input.available() == 0) { "Unexpected trailing Microworld data" }
        MicroworldSnapshot(revision, state, spaces, objects, observations, experiments).also(::validateSnapshot)
    }

    private fun validateSnapshot(s: MicroworldSnapshot) {
        require(s.revision >= 0)
        require(s.spaces.size <= MAX_SPACES && s.objects.size <= MAX_OBJECTS && s.observations.size <= MAX_OBSERVATIONS && s.experiments.size <= MAX_EXPERIMENTS)
        val spaceIds = s.spaces.map { it.id }.toSet()
        require(spaceIds.size == s.spaces.size)
        require(s.spaces.all { it.parentId == null || it.parentId in spaceIds })
        require(s.objects.map { it.id }.distinct().size == s.objects.size && s.objects.all { it.spaceId in spaceIds })
        require(s.observations.map { it.id }.distinct().size == s.observations.size)
        require(s.observations.all { it.subjectId == null || it.subjectId in spaceIds || s.objects.any { obj -> obj.id == it.subjectId } })
        require(s.experiments.map { it.id }.distinct().size == s.experiments.size && s.experiments.all { it.spaceId in spaceIds })
    }

    companion object {
        private const val FORMAT_VERSION = 1
        private const val NONCE_BYTES = 12
        private const val TAG_BYTES = 16
        private const val MAX_FILE_BYTES = 16 * 1024 * 1024
        private const val MAX_SPACES = 128
        private const val MAX_OBJECTS = 2_000
        private const val MAX_OBSERVATIONS = 10_000
        private const val MAX_EXPERIMENTS = 500
        private const val MAX_TAGS = 24
    }
}

private fun DataOutputStream.writeNullable(value: String?) { writeBoolean(value != null); if (value != null) writeUTF(value) }
private fun DataOutputStream.writeNullableLong(value: Long?) { writeBoolean(value != null); if (value != null) writeLong(value) }
private fun DataInputStream.readNullable(): String? = if (readBoolean()) readUTF() else null
private fun DataInputStream.readNullableLong(): Long? = if (readBoolean()) readLong() else null
private fun DataInputStream.readCount(max: Int): Int = readInt().also { require(it in 0..max) { "Microworld collection count is invalid" } }

/**
 * Persistent world-state service. Experiments are records/plans only: no arbitrary scripts,
 * shell commands, network calls, or device-control code are executed by this service.
 */
class ManaMicroworldService(
    private val store: MicroworldStore,
    private val limits: MicroworldLimits = MicroworldLimits(),
    private val clock: Clock = Clock.systemUTC(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) {
    private val lock = Any()
    private var state = store.load().also(::validate)

    fun snapshot(): MicroworldSnapshot = synchronized(lock) { state }

    fun pause(): MicroworldSnapshot = update { it.copy(runState = MicroworldRunState.PAUSED) }
    fun resume(): MicroworldSnapshot = update { it.copy(runState = MicroworldRunState.ACTIVE) }

    fun createSpace(name: String, description: String = "", parentId: String? = null): MicroworldSpace = synchronized(lock) {
        requireActive()
        val cleanName = text(name, "Space name")
        val cleanDescription = text(description, "Space description", allowBlank = true)
        require(state.spaces.size < limits.maxSpaces) { "Microworld space limit reached" }
        require(parentId == null || state.spaces.any { it.id == parentId }) { "Parent space does not exist" }
        val space = MicroworldSpace(idFactory(), cleanName, cleanDescription, parentId, clock.instant())
        commit(state.copy(revision = state.revision + 1, spaces = state.spaces + space))
        space
    }

    fun createObject(spaceId: String, name: String, description: String = "", tags: Set<String> = emptySet()): MicroworldObject = synchronized(lock) {
        requireActive()
        require(state.spaces.any { it.id == spaceId }) { "Space does not exist" }
        require(state.objects.size < limits.maxObjects) { "Microworld object limit reached" }
        require(tags.size <= limits.maxTagsPerObject && tags.all { it.isNotBlank() && it.length <= 128 }) { "Object tags exceed limits" }
        val item = MicroworldObject(idFactory(), spaceId, text(name, "Object name"), text(description, "Object description", true), tags.toSet(), clock.instant())
        commit(state.copy(revision = state.revision + 1, objects = state.objects + item))
        item
    }

    fun recordObservation(subjectId: String?, kind: MicroworldEvidenceKind, statement: String, source: String): MicroworldObservation = synchronized(lock) {
        requireActive()
        require(state.observations.size < limits.maxObservations) { "Microworld observation limit reached" }
        require(subjectId == null || state.spaces.any { it.id == subjectId } || state.objects.any { it.id == subjectId }) { "Observation subject does not exist" }
        val evidence = MicroworldObservation(idFactory(), subjectId, kind, text(statement, "Observation"), text(source, "Evidence source"), clock.instant())
        commit(state.copy(revision = state.revision + 1, observations = state.observations + evidence))
        evidence
    }

    fun createExperiment(spaceId: String, hypothesis: String, procedure: String): MicroworldExperiment = synchronized(lock) {
        requireActive()
        require(state.spaces.any { it.id == spaceId }) { "Experiment space does not exist" }
        require(state.experiments.size < limits.maxExperiments) { "Microworld experiment limit reached" }
        val experiment = MicroworldExperiment(idFactory(), spaceId, text(hypothesis, "Hypothesis"), text(procedure, "Procedure"), MicroworldExperimentState.DRAFT, null, clock.instant(), null)
        commit(state.copy(revision = state.revision + 1, experiments = state.experiments + experiment))
        experiment
    }

    fun finishExperiment(id: String, outcome: String, completed: Boolean = true): MicroworldExperiment = synchronized(lock) {
        requireActive()
        val result = text(outcome, "Experiment outcome")
        val old = state.experiments.firstOrNull { it.id == id } ?: error("Experiment does not exist")
        require(old.state == MicroworldExperimentState.DRAFT) { "Experiment is already closed" }
        val changed = old.copy(state = if (completed) MicroworldExperimentState.COMPLETED else MicroworldExperimentState.ABORTED, outcome = result, completedAt = clock.instant())
        commit(state.copy(revision = state.revision + 1, experiments = state.experiments.map { if (it.id == id) changed else it }))
        changed
    }

    private fun update(transform: (MicroworldSnapshot) -> MicroworldSnapshot): MicroworldSnapshot = synchronized(lock) {
        val next = transform(state).copy(revision = state.revision + 1)
        commit(next)
        state
    }

    private fun commit(next: MicroworldSnapshot) {
        validate(next)
        store.save(next)
        state = next
    }

    private fun requireActive() { check(state.runState == MicroworldRunState.ACTIVE) { "Microworld is paused" } }
    private fun text(value: String, field: String, allowBlank: Boolean = false): String {
        val clean = value.trim()
        require((allowBlank || clean.isNotEmpty()) && clean.length <= limits.maxTextLength) { "$field is empty or exceeds the configured limit" }
        return clean
    }

    private fun validate(s: MicroworldSnapshot) {
        require(s.spaces.size <= limits.maxSpaces && s.objects.size <= limits.maxObjects && s.observations.size <= limits.maxObservations && s.experiments.size <= limits.maxExperiments)
        require(s.spaces.map { it.id }.distinct().size == s.spaces.size)
        val spaces = s.spaces.map { it.id }.toSet()
        require(s.spaces.all { it.parentId == null || it.parentId in spaces })
        require(s.objects.map { it.id }.distinct().size == s.objects.size && s.objects.all { it.spaceId in spaces })
        require(s.observations.map { it.id }.distinct().size == s.observations.size)
        require(s.observations.all { it.subjectId == null || it.subjectId in spaces || s.objects.any { obj -> obj.id == it.subjectId } })
        require(s.experiments.map { it.id }.distinct().size == s.experiments.size && s.experiments.all { it.spaceId in spaces })
    }
}
