package com.mana.launcher

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.mana.os.core.ManaSecretKeyProvider
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/** AES key provider backed by Android Keystore; key bytes never leave the keystore. */
class ManaAndroidKeyProvider : ManaSecretKeyProvider {
    override fun getOrCreate(alias: String): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }
}
