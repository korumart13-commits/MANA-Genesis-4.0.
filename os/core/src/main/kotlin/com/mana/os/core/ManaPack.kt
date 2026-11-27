package com.mana.os.core

import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.PublicKey
import java.security.Signature
import java.util.Base64
import java.util.Properties
import java.util.zip.ZipFile

data class ManaPackModule(val id: String, val version: String, val entry: String, val sha256: String, val minOsVersion: String, val dependencies: Set<String>, val payloadFile: File)
data class VerifiedManaPack(val packId: String, val packVersion: String, val targetDevice: String, val minApi: Int, val maxApi: Int, val modules: List<ManaPackModule>, val stagingDirectory: File)

/** Verifies .manapack archives using an application-provisioned ECDSA trust anchor. */
class ManaPackVerifier(
    private val trustedKey: PublicKey, private val device: String, private val osVersion: String,
    private val apiLevel: Int, private val maxTotalPayloadBytes: Long = 4L * 1024 * 1024 * 1024
) {
    fun verify(file: File, stagingRoot: File): VerifiedManaPack {
        require(file.isFile && file.canRead()) { "MANA pack is not readable." }
        require(file.length() <= maxTotalPayloadBytes + MAX_OVERHEAD) { "MANA pack exceeds size limit." }
        require(device.isNotBlank() && osVersion.isNotBlank() && apiLevel > 0)
        require(stagingRoot.exists() || stagingRoot.mkdirs()) { "Cannot create staging directory." }
        ZipFile(file).use { zip ->
            val entries = mutableMapOf<String, java.util.zip.ZipEntry>()
            val enumeration = zip.entries()
            while (enumeration.hasMoreElements()) {
                val entry = enumeration.nextElement()
                val name = entry.name
                require(!entry.isDirectory && !name.startsWith("/") && !name.startsWith("\\") &&
                    name.split('/').none { it.isBlank() || it == "." || it == ".." } && !name.contains('\\')) {
                    "Unsafe MANA pack path: $name"
                }
                require(entries.put(name, entry) == null) { "Duplicate ZIP entry: $name" }
                require(name == MANIFEST_PATH || name.matches(Regex("modules/[a-z][a-z0-9_.-]{1,63}/payload\\.(bin|apk|jar|so|dat)"))) {
                    "Unexpected MANA pack entry: $name"
                }
            }
            val manifestEntry = entries[MANIFEST_PATH] ?: error("MANA pack manifest missing.")
            require(manifestEntry.size in 1..MAX_MANIFEST_BYTES) { "Invalid manifest size." }
            val props = Properties()
            zip.getInputStream(manifestEntry).use { props.load(it.reader(StandardCharsets.UTF_8)) }
            val packId = props.getProperty("pack.id").orEmpty()
            val packVersion = props.getProperty("pack.version").orEmpty()
            val target = props.getProperty("target.device").orEmpty()
            val minApi = props.getProperty("target.min_api")?.toIntOrNull() ?: -1
            val maxApi = props.getProperty("target.max_api")?.toIntOrNull() ?: -1
            require(packId.matches(ID_REGEX) && packVersion.matches(VERSION_REGEX)) { "Invalid pack identity/version." }
            require(target == device || target == "any") { "MANA pack targets a different device." }
            require(minApi > 0 && maxApi >= minApi && apiLevel in minApi..maxApi) { "Android API is incompatible." }
            val count = props.getProperty("modules.count")?.toIntOrNull() ?: -1
            require(count in 1..MAX_MODULES) { "Invalid module count." }
            val modules = (0 until count).map { index ->
                val prefix = "module.$index."
                val id = props.getProperty(prefix + "id").orEmpty()
                val version = props.getProperty(prefix + "version").orEmpty()
                val entry = props.getProperty(prefix + "entry").orEmpty()
                val hash = props.getProperty(prefix + "sha256").orEmpty().lowercase()
                val minOs = props.getProperty(prefix + "min_os").orEmpty()
                val deps = props.getProperty(prefix + "dependencies").orEmpty().split(',').filter { it.isNotBlank() }.toSet()
                require(id.matches(ID_REGEX) && version.matches(VERSION_REGEX)) { "Invalid module identity/version." }
                require(entry in setOf("bin", "apk", "jar", "so", "dat").map { "modules/$id/payload.$it" }) { "Invalid payload path for $id." }
                require(hash.matches(Regex("[a-f0-9]{64}"))) { "Invalid SHA-256 for $id." }
                require(minOs.isNotBlank() && compareVersions(osVersion, minOs) >= 0) { "OS version is too old for $id." }
                require(deps.all { it.matches(ID_REGEX) } && id !in deps) { "Invalid dependencies for $id." }
                require(entries.containsKey(entry)) { "Payload missing for $id." }
                ManaPackModule(id, version, entry, hash, minOs, deps, File(stagingRoot, "unused"))
            }
            require(modules.map { it.id }.distinct().size == modules.size) { "Duplicate module IDs." }
            val packIds = modules.map { it.id }.toSet()
            modules.forEach { module ->
                require((module.dependencies - packIds).isEmpty()) { "Missing dependency in package for \${module.id}: \${module.dependencies - packIds}" }
            }
            require(entries.keys == (setOf(MANIFEST_PATH) + modules.map { it.entry })) { "Manifest and ZIP entries do not match." }
            val signatureBytes = try { Base64.getDecoder().decode(props.getProperty("signature").orEmpty()) }
                catch (_: IllegalArgumentException) { error("Invalid signature encoding.") }
            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(trustedKey)
            verifier.update(canonical(packId, packVersion, target, minApi, maxApi, modules))
            require(verifier.verify(signatureBytes)) { "MANA pack signature is invalid." }
            val total = modules.sumOf { entries.getValue(it.entry).size }
            require(total in 1..maxTotalPayloadBytes) { "MANA pack payload size is invalid." }
            require(stagingRoot.usableSpace >= total + MIN_FREE_SPACE) { "Insufficient free space." }
            val stage = Files.createTempDirectory(stagingRoot.toPath(), "manapack-").toFile()
            try {
                var totalCopied = 0L
                val verifiedModules = modules.map { module ->
                    val destination = File(stage, module.id).apply { require(mkdirs()) }
                    val output = File(destination, "payload")
                    val digest = MessageDigest.getInstance("SHA-256")
                    var copied = 0L
                    zip.getInputStream(entries.getValue(module.entry)).use { input ->
                        FileOutputStream(output).use { stream ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                copied += n
                                totalCopied += n
                                require(totalCopied <= maxTotalPayloadBytes) { "Combined payload exceeds configured size." }
                                digest.update(buffer, 0, n)
                                stream.write(buffer, 0, n)
                            }
                            stream.fd.sync()
                        }
                    }
                    require(digest.digest().toHex() == module.sha256) { "SHA-256 mismatch for \${module.id}." }
                    module.copy(payloadFile = output)
                }
                return VerifiedManaPack(packId, packVersion, target, minApi, maxApi, verifiedModules, stage)
            } catch (failure: Throwable) {
                stage.deleteRecursively()
                throw failure
            }
        }
    }

    private fun canonical(id: String, version: String, target: String, minApi: Int, maxApi: Int, modules: List<ManaPackModule>): ByteArray {
        val lines = mutableListOf("MANA-PACK/1", id, version, target, minApi.toString(), maxApi.toString())
        modules.sortedBy { it.id }.forEach { m ->
            lines += listOf(m.id, m.version, m.entry, m.sha256, m.minOsVersion, m.dependencies.sorted().joinToString(",")).joinToString("|")
        }
        return lines.joinToString("\n").toByteArray(StandardCharsets.UTF_8)
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = left.split('.', '-', '+')
        val b = right.split('.', '-', '+')
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrNull(i) ?: "0"; val y = b.getOrNull(i) ?: "0"
            val xn = x.toLongOrNull(); val yn = y.toLongOrNull()
            val result = if (xn != null && yn != null) xn.compareTo(yn) else x.compareTo(y)
            if (result != 0) return result
        }
        return 0
    }

    companion object {
        const val MANIFEST_PATH = "META-INF/manapack.properties"
        private const val MAX_MANIFEST_BYTES = 64 * 1024L
        private const val MAX_OVERHEAD = 2 * 1024 * 1024L
        private const val MIN_FREE_SPACE = 32 * 1024 * 1024L
        private const val MAX_MODULES = 64
        private val ID_REGEX = Regex("[a-z][a-z0-9_.-]{1,63}")
        private val VERSION_REGEX = Regex("[A-Za-z0-9][A-Za-z0-9._+-]{0,63}")
    }
}

/** Transactional file staging. This is not Android PackageInstaller and grants no system privileges. */
class ManaModuleInstaller(private val modulesRoot: File) {
    fun apply(pack: VerifiedManaPack, installedModuleIds: Set<String>): List<String> {
        require(modulesRoot.exists() || modulesRoot.mkdirs()) { "Cannot create module directory." }
        val incoming = pack.modules.associateBy { it.id }
        pack.modules.forEach { module ->
            val missing = module.dependencies - incoming.keys - installedModuleIds
            require(missing.isEmpty()) { "Unsatisfied dependencies for \${module.id}: $missing" }
        }
        val transaction = Files.createTempDirectory(modulesRoot.toPath(), ".mana-transaction-").toFile()
        val backups = mutableMapOf<String, File>()
        val activated = mutableListOf<String>()
        try {
            pack.modules.forEach { module ->
                val current = File(modulesRoot, module.id)
                if (current.exists()) {
                    val backup = File(transaction, module.id + ".backup")
                    move(current, backup); backups[module.id] = backup
                }
                val next = File(transaction, module.id + ".new").apply { mkdirs() }
                Files.copy(module.payloadFile.toPath(), File(next, "payload").toPath(), StandardCopyOption.REPLACE_EXISTING)
                File(next, "version.txt").writeText(module.version, StandardCharsets.UTF_8)
                move(next, current); activated += module.id
            }
            transaction.deleteRecursively()
            return activated
        } catch (failure: Throwable) {
            activated.asReversed().forEach { File(modulesRoot, it).deleteRecursively() }
            backups.forEach { (id, backup) -> if (backup.exists()) move(backup, File(modulesRoot, id)) }
            transaction.deleteRecursively()
            throw IllegalStateException("MANA module transaction failed; previous modules restored.", failure)
        }
    }
    private fun move(source: File, target: File) {
        try { Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE) }
        catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(source.toPath(), target.toPath()) }
    }
}
private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }
