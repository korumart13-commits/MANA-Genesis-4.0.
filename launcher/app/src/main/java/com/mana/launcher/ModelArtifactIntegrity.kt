package com.mana.launcher

import java.io.File
import java.security.MessageDigest

/** Recomputes the full digest; length/mtime caches are not trusted as integrity evidence. */
internal object ModelArtifactIntegrity {
    fun matches(file: File, expectedLength: Long, expectedSha256: String, allowedDirectory: File): Boolean {
        return try {
            if (!file.isFile || file.length() != expectedLength) return false
            val root = allowedDirectory.canonicalFile
            val candidate = file.canonicalFile
            if (candidate.parentFile != root) return false

            val digest = MessageDigest.getInstance("SHA-256")
            candidate.inputStream().buffered().use { input ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            val actual = digest.digest().joinToString("") {
                (it.toInt() and 0xff).toString(16).padStart(2, '0')
            }
            actual.equals(expectedSha256, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }
}
