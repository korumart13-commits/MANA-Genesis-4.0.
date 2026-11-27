package com.mana.os.core

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManaPackTest {
    private val device = "RMX5566RU/RE645F/SM6225"
    private val payload = "verified mana module payload".toByteArray()

    private fun keyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
    private fun hash(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private fun createPack(dir: File, keys: KeyPair = keyPair(), bytes: ByteArray = payload): File {
        val id = "mana.example"
        val entry = "modules/$id/payload.bin"
        val sha = hash(bytes)
        val canonical = listOf("MANA-PACK/1", "mana.sample", "1.0.0", device, "29", "36",
            listOf(id, "1.2.0", entry, sha, "4.0.0", "").joinToString("|")
        ).joinToString("\n").toByteArray(StandardCharsets.UTF_8)
        val signer = Signature.getInstance("SHA256withECDSA").apply {
            initSign(keys.private); update(canonical)
        }
        val props = Properties().apply {
            setProperty("pack.id", "mana.sample")
            setProperty("pack.version", "1.0.0")
            setProperty("target.device", device)
            setProperty("target.min_api", "29")
            setProperty("target.max_api", "36")
            setProperty("modules.count", "1")
            setProperty("module.0.id", id)
            setProperty("module.0.version", "1.2.0")
            setProperty("module.0.entry", entry)
            setProperty("module.0.sha256", sha)
            setProperty("module.0.min_os", "4.0.0")
            setProperty("module.0.dependencies", "")
            setProperty("signature", Base64.getEncoder().encodeToString(signer.sign()))
        }
        val manifest = ByteArrayOutputStream().also { props.store(it, null) }.toByteArray()
        val out = File(dir, "sample.manapack")
        ZipOutputStream(out.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry(ManaPackVerifier.MANIFEST_PATH)); zip.write(manifest); zip.closeEntry()
            zip.putNextEntry(ZipEntry(entry)); zip.write(bytes); zip.closeEntry()
        }
        return out
    }

    @Test fun verifiesSignatureHashDeviceAndApiThenStagesModule() {
        val dir = java.nio.file.Files.createTempDirectory("mana-pack-valid").toFile()
        val keys = keyPair()
        val file = createPack(dir, keys)
        val verified = ManaPackVerifier(keys.public, device, "4.0.2", 36).verify(file, File(dir, "stage"))
        assertEquals("mana.sample", verified.packId)
        assertEquals("mana.example", verified.modules.single().id)
        assertEquals(hash(payload), hash(verified.modules.single().payloadFile.readBytes()))
        val root = File(dir, "modules")
        File(root, "mana.example").apply { mkdirs() }.resolve("payload").writeText("old")
        val applied = ManaModuleInstaller(root).apply(verified, emptySet())
        assertEquals(listOf("mana.example"), applied)
        assertEquals(payload.toList(), File(root, "mana.example/payload").readBytes().toList())
        assertEquals("1.2.0", File(root, "mana.example/version.txt").readText())
        dir.deleteRecursively()
    }

    @Test fun rejectsWrongTrustAnchorDeviceAndApi() {
        val dir = java.nio.file.Files.createTempDirectory("mana-pack-policy").toFile()
        val keys = keyPair()
        val file = createPack(dir, keys)
        assertFailsWith<IllegalArgumentException> {
            ManaPackVerifier(keyPair().public, device, "4.0.2", 36).verify(file, File(dir, "wrong-key"))
        }
        assertFailsWith<IllegalArgumentException> {
            ManaPackVerifier(keys.public, "OTHER_DEVICE", "4.0.2", 36).verify(file, File(dir, "wrong-device"))
        }
        assertFailsWith<IllegalArgumentException> {
            ManaPackVerifier(keys.public, device, "4.0.2", 28).verify(file, File(dir, "wrong-api"))
        }
        dir.deleteRecursively()
    }

    @Test fun rejectsTamperedPayloadAndUnsatisfiedDependencyWithoutChangingInstalledModule() {
        val dir = java.nio.file.Files.createTempDirectory("mana-pack-tamper").toFile()
        val keys = keyPair()
        val valid = createPack(dir, keys)
        val tampered = File(dir, "tampered.manapack")
        ZipFile(valid).use { source ->
            ZipOutputStream(tampered.outputStream()).use { target ->
                val entries = source.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val bytes = source.getInputStream(entry).use { it.readBytes() }
                    target.putNextEntry(ZipEntry(entry.name))
                    target.write(if (entry.name.endsWith("payload.bin")) bytes + byteArrayOf(1) else bytes)
                    target.closeEntry()
                }
            }
        }
        assertFailsWith<IllegalArgumentException> {
            ManaPackVerifier(keys.public, device, "4.0.2", 36).verify(tampered, File(dir, "tampered-stage"))
        }
        val verified = ManaPackVerifier(keys.public, device, "4.0.2", 36).verify(valid, File(dir, "valid-stage"))
        val root = File(dir, "active")
        val existing = File(root, "mana.example").apply { mkdirs() }.resolve("payload")
        existing.writeText("previous-version")
        val withMissingDependency = verified.copy(modules = verified.modules.map { it.copy(dependencies = setOf("mana.missing")) })
        assertFailsWith<IllegalArgumentException> { ManaModuleInstaller(root).apply(withMissingDependency, emptySet()) }
        assertEquals("previous-version", existing.readText())
        assertTrue(existing.isFile)
        dir.deleteRecursively()
    }
}
