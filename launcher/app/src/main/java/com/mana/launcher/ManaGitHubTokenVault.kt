package com.mana.launcher

import android.content.Context
import com.mana.brain.GitHubTokenProvider
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/** Stores a fine-grained GitHub token encrypted with an AES key held by Android Keystore. */
class ManaGitHubTokenVault(
    context: Context,
    private val keyProvider: ManaAndroidKeyProvider
) : GitHubTokenProvider {
    private val app = context.applicationContext
    private val tokenFile = File(app.filesDir, "mana/github-actions-token.bin")
    private val random = SecureRandom()

    override fun accessToken(): String? {
        if (!tokenFile.isFile) return null
        val payload = tokenFile.readBytes()
        require(payload.size in 29..8_192) { "Зашифрованный GitHub token повреждён." }
        val nonce = payload.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keyProvider.getOrCreate(KEY_ALIAS), GCMParameterSpec(128, nonce))
        cipher.updateAAD(AAD)
        return String(cipher.doFinal(payload.copyOfRange(12, payload.size)), StandardCharsets.UTF_8)
            .also(::validateToken)
    }

    fun save(token: String) {
        validateToken(token)
        val parent = tokenFile.absoluteFile.parentFile ?: error("Не найден каталог MANA.")
        require(parent.exists() || parent.mkdirs()) { "Не удалось создать каталог MANA." }
        val nonce = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider.getOrCreate(KEY_ALIAS), GCMParameterSpec(128, nonce))
        cipher.updateAAD(AAD)
        val encrypted = cipher.doFinal(token.toByteArray(StandardCharsets.UTF_8))
        val temp = File.createTempFile("mana-github-", ".part", parent)
        try {
            java.io.FileOutputStream(temp).use { output ->
                output.write(nonce)
                output.write(encrypted)
                output.fd.sync()
            }
            try {
                Files.move(temp.toPath(), tokenFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temp.toPath(), tokenFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    fun isConfigured(): Boolean = runCatching { !accessToken().isNullOrBlank() }.getOrDefault(false)

    fun clear() {
        if (tokenFile.exists() && !tokenFile.delete()) {
            throw IllegalStateException("Не удалось удалить локальный GitHub token.")
        }
    }

    private fun validateToken(token: String) {
        require(token.length in 20..500 && token.none(Char::isWhitespace)) {
            "GitHub token должен быть непустым и не содержать пробелов."
        }
    }

    companion object {
        private const val KEY_ALIAS = "mana.github.actions.v1"
        private val AAD = "MANA-GITHUB-ACTIONS-TOKEN-v1".toByteArray(StandardCharsets.UTF_8)
    }
}
