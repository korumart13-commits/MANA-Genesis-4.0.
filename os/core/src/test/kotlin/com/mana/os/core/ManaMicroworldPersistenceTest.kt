package com.mana.os.core

import java.io.File
import java.nio.file.Files
import java.security.Key
import java.time.Instant
import javax.crypto.KeyGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ManaMicroworldPersistenceTest {
    private class TestKeys : ManaSecretKeyProvider {
        private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        override fun getOrCreate(alias: String): Key = key
    }

    @Test
    fun encryptedSnapshotSurvivesServiceRestart() {
        val directory = Files.createTempDirectory("mana-world-test").toFile()
        val file = File(directory, "world.mwld")
        val keys = TestKeys()
        try {
            val first = ManaMicroworldService(EncryptedFileMicroworldStore(file, keys))
            val root = first.createSpace("MANA Home", "Persistent root")
            val objectItem = first.createObject(root.id, "Memory crystal", "A stored world object")
            first.recordObservation(
                objectItem.id,
                MicroworldEvidenceKind.USER_INPUT,
                "Created by owner",
                "owner"
            )

            val restored = ManaMicroworldService(EncryptedFileMicroworldStore(file, keys)).snapshot()
            assertEquals(3L, restored.revision)
            assertEquals("MANA Home", restored.spaces.single().name)
            assertEquals("Memory crystal", restored.objects.single().name)
            assertEquals("Created by owner", restored.observations.single().statement)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun encryptedSnapshotRejectsWrongKeyOrTampering() {
        val directory = Files.createTempDirectory("mana-world-tamper").toFile()
        val file = File(directory, "world.mwld")
        try {
            val writerKeys = TestKeys()
            val world = ManaMicroworldService(EncryptedFileMicroworldStore(file, writerKeys))
            world.createSpace("Private space")

            val bytes = file.readBytes()
            bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x01).toByte()
            file.writeBytes(bytes)
            assertFails {
                EncryptedFileMicroworldStore(file, writerKeys).load()
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun encryptedSnapshotRejectsDifferentKey() {
        val directory = Files.createTempDirectory("mana-world-key").toFile()
        val file = File(directory, "world.mwld")
        try {
            ManaMicroworldService(EncryptedFileMicroworldStore(file, TestKeys()))
                .createSpace("Private space")
            assertFails {
                EncryptedFileMicroworldStore(file, TestKeys()).load()
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
