package com.mana.launcher

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Preserves an unreadable encrypted persistence file without decrypting, deleting,
 * replacing, or changing its original. The recovery copy stays app-private.
 */
internal object ManaEncryptedDataRecovery {
    private const val MAX_RECOVERY_BYTES = 64L * 1024L * 1024L

    fun preserve(source: File, recoveryDirectory: File, logicalName: String): File {
        require(logicalName.matches(Regex("[a-zA-Z0-9._-]{1,48}"))) {
            "Invalid recovery data label"
        }
        require(source.isFile && source.length() in 1..MAX_RECOVERY_BYTES) {
            "Encrypted source is missing, empty, or exceeds the recovery size limit"
        }
        val original = source.canonicalFile
        val directory = recoveryDirectory.canonicalFile
        require(directory.exists() || directory.mkdirs()) {
            "Cannot create private recovery directory"
        }
        require(directory.isDirectory && directory != original.parentFile) {
            "Invalid recovery directory"
        }

        val beforeLength = original.length()
        val beforeDigest = digest(original)
        val copy = File.createTempFile("mana-$logicalName-", ".encrypted-recovery", directory)
        try {
            FileInputStream(original).use { input ->
                FileOutputStream(copy).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            require(original.length() == beforeLength && copy.length() == beforeLength &&
                digest(original) == beforeDigest && digest(copy) == beforeDigest) {
                "Encrypted source changed while its recovery copy was being made"
            }
            return copy.canonicalFile
        } catch (error: Exception) {
            copy.delete()
            throw error
        }
    }

    private fun digest(file: File): String {
        val sha = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                sha.update(buffer, 0, count)
            }
        }
        return sha.digest().joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
    }
}
