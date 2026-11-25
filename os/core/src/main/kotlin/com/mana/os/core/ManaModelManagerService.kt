package com.mana.os.core

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

data class ManaModelArtifact(
    val id: String,
    val fileName: String,
    val expectedBytes: Long,
    val expectedSha256: String,
    val licenseId: String,
    val licenseAccepted: Boolean,
    val maxBytes: Long = 16L * 1024L * 1024L * 1024L
) {
    init {
        require(id.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{1,63}"))) { "Invalid model identifier." }
        require(fileName.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{1,127}\\.litertlm"))) { "Model file name must be a safe .litertlm name." }
        require(expectedBytes in 1..maxBytes) { "Model size is outside the configured safe limit." }
        require(expectedSha256.matches(Regex("[a-fA-F0-9]{64}"))) { "A SHA-256 checksum is required." }
        require(licenseId.isNotBlank() && licenseId.length <= 160) { "Model license identifier is required." }
    }
}

data class ManaModelInstallResult(
    val modelFile: File,
    val bytesWritten: Long,
    val sha256: String,
    val licenseId: String
)

/**
 * Safe staging installer for owner-supplied LiteRT-LM model files.
 * It verifies owner license acceptance, exact length and SHA-256 before
 * replacing the active file. It never downloads models or executes them.
 */
class ManaModelManagerService(
    private val modelDirectory: File,
    private val reserveBytes: Long = 256L * 1024L * 1024L
) {
    init { require(reserveBytes >= 0L) }

    fun install(artifact: ManaModelArtifact, input: InputStream): ManaModelInstallResult {
        require(artifact.licenseAccepted) { "Owner must review and accept the model license before installation." }
        require(artifact.expectedBytes <= artifact.maxBytes) { "Model exceeds the maximum allowed size." }
        val directory = modelDirectory.canonicalFile
        require(directory.exists() || directory.mkdirs()) { "Cannot create model directory." }
        require(directory.isDirectory) { "Model path is not a directory." }
        val target = File(directory, artifact.fileName).canonicalFile
        require(target.parentFile == directory) { "Model path escapes the managed directory." }
        require(directory.usableSpace >= artifact.expectedBytes + reserveBytes) {
            "Insufficient free space for model installation and safety reserve."
        }

        val temp = File.createTempFile("mana-model-", ".part", directory)
        val digest = MessageDigest.getInstance("SHA-256")
        var copied = 0L
        try {
            input.use { source ->
                FileOutputStream(temp).use { file ->
                    val output = java.io.BufferedOutputStream(file)
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= artifact.maxBytes && copied <= artifact.expectedBytes) {
                            "Model stream exceeded its declared size."
                        }
                        digest.update(buffer, 0, count)
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                    file.fd.sync()
                }
            }
            require(copied == artifact.expectedBytes) { "Model size does not match its declared size." }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            require(hash.equals(artifact.expectedSha256, ignoreCase = true)) { "Model SHA-256 verification failed." }

            try {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            val metadata = File(directory, "${artifact.id}.metadata")
            val metadataTemp = File.createTempFile("mana-meta-", ".part", directory)
            try {
                metadataTemp.writeText(
                    "id=${artifact.id}\nfile=${artifact.fileName}\nbytes=$copied\nsha256=$hash\nlicense=${artifact.licenseId}\n"
                )
                try {
                    Files.move(metadataTemp.toPath(), metadata.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                    Files.move(metadataTemp.toPath(), metadata.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            } finally {
                if (metadataTemp.exists()) metadataTemp.delete()
            }
            return ManaModelInstallResult(target, copied, hash, artifact.licenseId)
        } finally {
            if (temp.exists()) temp.delete()
        }
    }
}
