package com.mana.os.core

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.PublicKey
import java.security.Signature
import java.util.Base64
import java.util.Properties
import java.util.zip.ZipFile
import java.security.MessageDigest

data class ManaUpdateManifest(
    val version: String,
    val device: String,
    val payloadName: String,
    val payloadSha256: String,
    val rollbackIndex: Long,
    val signatureBase64: String
)

data class VerifiedManaUpdatePackage(
    val version: String,
    val device: String,
    val rollbackIndex: Long,
    val payloadFile: File,
    val sha256: String
)

/**
 * Verifies a signed MANA update package without flashing or installing it.
 * A release private key must stay outside the repository; only its public key is supplied here.
 */
class ManaUpdatePackageVerifier(
    private val trustedReleaseKey: PublicKey,
    private val expectedDevice: String,
    private val maxPayloadBytes: Long = 1_500_000_000L
) {
    fun verify(packageFile: File, stagingDirectory: File, minimumRollbackIndex: Long): VerifiedManaUpdatePackage {
        require(packageFile.isFile && packageFile.canRead()) { "Update package is not readable." }
        require(expectedDevice.isNotBlank())
        require(minimumRollbackIndex >= 0)
        require(stagingDirectory.exists() || stagingDirectory.mkdirs()) { "Cannot create update staging directory." }

        ZipFile(packageFile).use { zip ->
            val names = mutableSetOf<String>()
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name
                require(names.add(name)) { "Duplicate ZIP entry is not allowed: $name" }
                require(!entry.isDirectory && !name.startsWith("/") && !name.startsWith("\\") &&
                    name.split('/', '\\').none { it == ".." || it.isBlank() }) {
                    "Unsafe update ZIP path: $name"
                }
                require(name in ALLOWED_ENTRIES) { "Unexpected update package entry: $name" }
            }

            val manifestEntry = zip.getEntry(MANIFEST_PATH) ?: error("Signed update manifest is missing.")
            val payloadEntry = zip.getEntry(PAYLOAD_NAME) ?: error("Update payload is missing.")
            require(manifestEntry.size in 1..MAX_MANIFEST_BYTES) { "Update manifest size is invalid." }
            require(payloadEntry.size in 1..maxPayloadBytes) { "Update payload size is invalid or exceeds the limit." }

            val properties = Properties()
            zip.getInputStream(manifestEntry).use { input -> properties.load(input.reader(StandardCharsets.UTF_8)) }
            val manifest = ManaUpdateManifest(
                version = properties.getProperty("version").orEmpty(),
                device = properties.getProperty("device").orEmpty(),
                payloadName = properties.getProperty("payload").orEmpty(),
                payloadSha256 = properties.getProperty("sha256").orEmpty().lowercase(),
                rollbackIndex = properties.getProperty("rollback_index")?.toLongOrNull() ?: -1L,
                signatureBase64 = properties.getProperty("signature").orEmpty()
            )
            validateManifest(manifest, minimumRollbackIndex)
            verifySignature(manifest)

            val temp = File.createTempFile("mana-update-", ".part", stagingDirectory)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                zip.getInputStream(payloadEntry).use { input ->
                    FileOutputStream(temp).use { output ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            copied += read
                            require(copied <= maxPayloadBytes) { "Update payload exceeded the configured size limit." }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                        }
                        output.fd.sync()
                    }
                }
                require(copied == payloadEntry.size) { "Update payload size changed while extracting." }
                val actualHash = digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
                require(actualHash == manifest.payloadSha256) { "Update payload SHA-256 mismatch." }
                val target = File(stagingDirectory, "mana-update-${manifest.version}.payload")
                try {
                    Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                    Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                return VerifiedManaUpdatePackage(
                    manifest.version, manifest.device, manifest.rollbackIndex, target, actualHash
                )
            } finally {
                if (temp.exists()) temp.delete()
            }
        }
    }

    private fun validateManifest(manifest: ManaUpdateManifest, minimumRollbackIndex: Long) {
        require(manifest.version.matches(Regex("[A-Za-z0-9._-]{1,64}"))) { "Update version is invalid." }
        require(manifest.device == expectedDevice) { "Update package targets a different device." }
        require(manifest.payloadName == PAYLOAD_NAME) { "Unexpected payload name." }
        require(manifest.payloadSha256.matches(Regex("[a-f0-9]{64}"))) { "Payload SHA-256 is invalid." }
        require(manifest.rollbackIndex > minimumRollbackIndex) { "Rollback protection rejected this package." }
        require(manifest.signatureBase64.isNotBlank() && manifest.signatureBase64.length <= MAX_SIGNATURE_CHARS) {
            "Update signature is missing or too large."
        }
    }

    private fun verifySignature(manifest: ManaUpdateManifest) {
        val signatureBytes = try { Base64.getDecoder().decode(manifest.signatureBase64) }
            catch (_: IllegalArgumentException) { error("Update signature is not valid Base64.") }
        val verifier = Signature.getInstance("SHA256withECDSA")
        verifier.initVerify(trustedReleaseKey)
        verifier.update(canonicalManifest(manifest))
        require(verifier.verify(signatureBytes)) { "Update package signature is invalid." }
    }

    private fun canonicalManifest(manifest: ManaUpdateManifest): ByteArray =
        listOf(manifest.version, manifest.device, manifest.payloadName, manifest.payloadSha256, manifest.rollbackIndex.toString())
            .joinToString("\n").toByteArray(StandardCharsets.UTF_8)

    companion object {
        const val MANIFEST_PATH = "META-INF/mana-update.properties"
        const val PAYLOAD_NAME = "payload.bin"
        private const val MAX_MANIFEST_BYTES = 64 * 1024L
        private const val MAX_SIGNATURE_CHARS = 4096
        private val ALLOWED_ENTRIES = setOf(MANIFEST_PATH, PAYLOAD_NAME, "release-notes.txt")
    }
}
