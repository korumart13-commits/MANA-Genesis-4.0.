package com.mana.os.core

import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaModelManagerServiceTest {
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

    @Test fun installsOnlyAfterSizeHashAndLicenseChecks() {
        val dir = Files.createTempDirectory("mana-model-test").toFile()
        val bytes = "valid test model bytes".toByteArray()
        val artifact = ManaModelArtifact("gemma-test", "gemma-test.litertlm", bytes.size.toLong(), hash(bytes), "test-license", true)
        val result = ManaModelManagerService(dir, 0).install(artifact, ByteArrayInputStream(bytes))
        assertTrue(result.modelFile.isFile)
        assertEquals(bytes.size.toLong(), result.bytesWritten)
        assertTrue(File(dir, "gemma-test.metadata").isFile)
        dir.deleteRecursively()
    }

    @Test fun rejectsUnacceptedLicenseWithoutWritingModel() {
        val dir = Files.createTempDirectory("mana-model-license").toFile()
        val bytes = byteArrayOf(1, 2, 3)
        val artifact = ManaModelArtifact("test-model", "test-model.litertlm", 3, hash(bytes), "unknown-license", false)
        assertFailsWith<IllegalArgumentException> {
            ManaModelManagerService(dir, 0).install(artifact, ByteArrayInputStream(bytes))
        }
        assertFalse(File(dir, "test-model.litertlm").exists())
        dir.deleteRecursively()
    }

    @Test fun rejectsHashMismatchAndPreservesPreviouslyInstalledModel() {
        val dir = Files.createTempDirectory("mana-model-hash").toFile()
        val target = File(dir, "model.litertlm")
        target.writeText("previous-good-model")
        val bytes = "new-but-invalid".toByteArray()
        val artifact = ManaModelArtifact("model", "model.litertlm", bytes.size.toLong(), "0".repeat(64), "license", true)
        assertFailsWith<IllegalArgumentException> {
            ManaModelManagerService(dir, 0).install(artifact, ByteArrayInputStream(bytes))
        }
        assertEquals("previous-good-model", target.readText())
        dir.deleteRecursively()
    }

    @Test fun rejectsUnsafeFileNameAndOversizedStream() {
        assertFailsWith<IllegalArgumentException> {
            ManaModelArtifact("bad-model", "../escape.litertlm", 1, "0".repeat(64), "license", true)
        }
        val dir = Files.createTempDirectory("mana-model-size").toFile()
        val bytes = ByteArray(4)
        val artifact = ManaModelArtifact("small", "small.litertlm", 3, hash(bytes.copyOf(3)), "license", true, maxBytes = 3)
        assertFailsWith<IllegalArgumentException> {
            ManaModelManagerService(dir, 0).install(artifact, ByteArrayInputStream(bytes))
        }
        assertFalse(File(dir, "small.litertlm").exists())
        dir.deleteRecursively()
    }
}
