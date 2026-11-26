package com.mana.launcher

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ManaEncryptedDataRecoveryTest {
    @Test
    fun preservesEncryptedBytesAndLeavesOriginalUntouched() {
        val root = createTempDir(prefix = "mana-recovery-")
        try {
            val source = File(root, "memory.bin").apply { writeBytes(ByteArray(4096) { (it % 251).toByte() }) }
            val original = source.readBytes()
            val recovery = File(root, "private-recovery")
            val copy = ManaEncryptedDataRecovery.preserve(source, recovery, "memory")

            assertArrayEquals(original, copy.readBytes())
            assertArrayEquals(original, source.readBytes())
            assertNotEquals(source.canonicalPath, copy.canonicalPath)
            assertEquals(sha256(original), sha256(copy.readBytes()))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun createsDistinctCopiesInsteadOfOverwritingPreviousRecovery() {
        val root = createTempDir(prefix = "mana-recovery-")
        try {
            val source = File(root, "diary.bin").apply { writeText("encrypted-payload") }
            val recovery = File(root, "private-recovery")
            val first = ManaEncryptedDataRecovery.preserve(source, recovery, "diary")
            val second = ManaEncryptedDataRecovery.preserve(source, recovery, "diary")
            assertNotEquals(first.name, second.name)
            assertEquals("encrypted-payload", first.readText())
            assertEquals("encrypted-payload", second.readText())
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsInvalidLabelAndMissingSource() {
        val root = createTempDir(prefix = "mana-recovery-")
        try {
            val source = File(root, "empty.bin")
            assertThrows(IllegalArgumentException::class.java) {
                ManaEncryptedDataRecovery.preserve(source, File(root, "recovery"), "../memory")
            }
            source.writeText("payload")
            assertThrows(IllegalArgumentException::class.java) {
                ManaEncryptedDataRecovery.preserve(source, File(root, "recovery"), "../memory")
            }
        } finally {
            root.deleteRecursively()
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
            (it.toInt() and 0xff).toString(16).padStart(2, '0')
        }
}
