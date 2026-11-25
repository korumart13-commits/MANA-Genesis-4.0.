package com.mana.os.core

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManaUpdatePackageVerifierTest {
    private val device = "RMX5566RU/RE645F/SM6225"

    private fun createPackage(directory: File, payload: ByteArray, rollback: Long = 2): Pair<File, java.security.PublicKey> {
        val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val hash = MessageDigest.getInstance("SHA-256").digest(payload)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        val canonical = listOf("4.0.3", device, "payload.bin", hash, rollback.toString())
            .joinToString("\n").toByteArray(StandardCharsets.UTF_8)
        val signer = Signature.getInstance("SHA256withECDSA").apply {
            initSign(keyPair.private)
            update(canonical)
        }
        val properties = Properties().apply {
            setProperty("version", "4.0.3")
            setProperty("device", device)
            setProperty("payload", "payload.bin")
            setProperty("sha256", hash)
            setProperty("rollback_index", rollback.toString())
            setProperty("signature", Base64.getEncoder().encodeToString(signer.sign()))
        }
        val manifest = ByteArrayOutputStream().also { properties.store(it, null) }.toByteArray()
        val archive = File(directory, "update.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(ManaUpdatePackageVerifier.MANIFEST_PATH))
            zip.write(manifest)
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(ManaUpdatePackageVerifier.PAYLOAD_NAME))
            zip.write(payload)
            zip.closeEntry()
        }
        return archive to keyPair.public
    }

    @Test fun verifiesSignatureDevicePayloadAndRollbackBeforeStaging() {
        val directory = java.nio.file.Files.createTempDirectory("mana-update-verify").toFile()
        val payload = "signed system update payload".toByteArray()
        val (archive, publicKey) = createPackage(directory, payload)
        val result = ManaUpdatePackageVerifier(publicKey, device).verify(archive, File(directory, "stage"), 1)
        assertEquals("4.0.3", result.version)
        assertEquals(device, result.device)
        assertTrue(result.payloadFile.isFile)
        assertContentEquals(payload, result.payloadFile.readBytes())
        assertFailsWith<IllegalArgumentException> {
            ManaUpdatePackageVerifier(publicKey, "OTHER_DEVICE").verify(archive, File(directory, "wrong-device"), 1)
        }
        assertFailsWith<IllegalArgumentException> {
            ManaUpdatePackageVerifier(publicKey, device).verify(archive, File(directory, "rollback"), 2)
        }
        directory.deleteRecursively()
    }

    @Test fun rejectsPayloadTamperingAndUnsafeZipPaths() {
        val directory = java.nio.file.Files.createTempDirectory("mana-update-tamper").toFile()
        val payload = "signed payload".toByteArray()
        val (validArchive, publicKey) = createPackage(directory, payload)
        val tampered = File(directory, "tampered.zip")
        ZipOutputStream(tampered.outputStream()).use { zip ->
            java.util.zip.ZipFile(validArchive).use { source ->
                val entries = source.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    zip.putNextEntry(ZipEntry(entry.name))
                    val bytes = source.getInputStream(entry).use { it.readBytes() }
                    zip.write(if (entry.name == ManaUpdatePackageVerifier.PAYLOAD_NAME) bytes + byteArrayOf(0) else bytes)
                    zip.closeEntry()
                }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            ManaUpdatePackageVerifier(publicKey, device).verify(tampered, File(directory, "tampered-stage"), 1)
        }
        val unsafe = File(directory, "unsafe.zip")
        ZipOutputStream(unsafe.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../escape"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }
        assertFailsWith<IllegalArgumentException> {
            ManaUpdatePackageVerifier(publicKey, device).verify(unsafe, File(directory, "unsafe-stage"), 1)
        }
        directory.deleteRecursively()
    }
}
