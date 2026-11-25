package com.mana.launcher

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small AES-GCM credential vault. The encryption key remains inside Android Keystore. */
class ManaEncryptedCredentialStore(
    context: Context,
    fileName: String,
    private val alias: String
) {
    private val file = File(context.applicationContext.filesDir, "mana/credentials/$fileName")
    private val random = SecureRandom()
    private val magic = byteArrayOf(0x4d, 0x41, 0x4e, 0x41, 0x43, 0x31)

    @Synchronized fun write(secret: String) {
        require(secret.isNotBlank() && secret.length <= 16_384)
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        val encrypted = cipher.doFinal(secret.toByteArray(Charsets.UTF_8))
        val parent = file.parentFile ?: error("Credential path has no parent")
        require(parent.exists() || parent.mkdirs()) { "Cannot create credential directory" }
        val temp = File.createTempFile("mana-credential-", ".part", parent)
        try {
            FileOutputStream(temp).use { output ->
                output.write(magic); output.write(nonce); output.write(encrypted); output.fd.sync()
            }
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { if (temp.exists()) temp.delete() }
    }

    @Synchronized fun read(): String? {
        if (!file.isFile) return null
        val bytes = file.readBytes()
        require(bytes.size > magic.size + 12 + 16 && bytes.copyOfRange(0, magic.size).contentEquals(magic)) {
            "Encrypted credential file is invalid."
        }
        val nonce = bytes.copyOfRange(magic.size, magic.size + 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, nonce))
        cipher.updateAAD(magic)
        return cipher.doFinal(bytes.copyOfRange(magic.size + 12, bytes.size)).toString(Charsets.UTF_8)
    }

    @Synchronized fun clear() { if (file.exists()) require(file.delete()) { "Could not remove encrypted credential file." } }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
