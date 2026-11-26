package com.mana.launcher

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelArtifactIntegrityTest {
    @Test
    fun acceptsExactDigestAndLengthInsideAllowedDirectory() {
        val root = createTempDir(prefix = "mana-model-")
        try {
            val file = File(root, "model.bin").apply { writeText("trusted-model") }
            assertTrue(ModelArtifactIntegrity.matches(file, file.length(), sha256("trusted-model"), root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsSameLengthReplacementEvenWhenMtimeIsRestored() {
        val root = createTempDir(prefix = "mana-model-")
        try {
            val file = File(root, "model.bin").apply { writeText("trusted-model") }
            val originalLength = file.length()
            val originalMtime = file.lastModified()
            file.writeText("trussed-model")
            file.setLastModified(originalMtime)
            assertFalse(ModelArtifactIntegrity.matches(file, originalLength, sha256("trusted-model"), root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsFileOutsideAllowedDirectory() {
        val root = createTempDir(prefix = "mana-model-root-")
        val outside = createTempDir(prefix = "mana-model-outside-")
        try {
            val file = File(outside, "model.bin").apply { writeText("trusted-model") }
            assertFalse(ModelArtifactIntegrity.matches(file, file.length(), sha256("trusted-model"), root))
        } finally {
            root.deleteRecursively()
            outside.deleteRecursively()
        }
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
}
