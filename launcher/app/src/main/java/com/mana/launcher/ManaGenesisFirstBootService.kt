package com.mana.launcher

import android.content.Context
import android.os.Build
import com.mana.os.core.GenesisCapabilitySnapshot
import com.mana.os.core.GenesisDevelopmentPlan
import com.mana.os.core.ManaGenesisBacklog
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties

/**
 * First-boot capability inventory and encrypted backlog checkpoint.
 *
 * It records observed app/device facts and a conservative development plan.
 * It never treats a missing OEM source as available, downloads firmware, changes
 * partitions, grants permissions, runs generated code or installs updates.
 */
class ManaGenesisFirstBootService(context: Context) {
    private val app = context.applicationContext
    private val file = File(app.filesDir, "mana/genesis-first-boot.enc")
    private val keyAlias = "mana.genesis.backlog.v1"

    @Synchronized
    fun refresh(): GenesisDevelopmentPlan {
        val snapshot = collectSnapshot()
        val plan = ManaGenesisBacklog.initial().plan(snapshot)
        persist(snapshot, plan)
        return plan
    }

    @Synchronized
    fun readCheckpoint(): JSONObject? {
        if (!file.isFile || file.length() !in 29..(2L * 1024L * 1024L)) return null
        return runCatching {
            val bytes = file.readBytes()
            val nonce = bytes.copyOfRange(0, 12)
            val ciphertext = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, nonce))
            JSONObject(String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8))
        }.getOrNull()
    }

    private fun collectSnapshot(): GenesisCapabilitySnapshot {
        val installed = linkedSetOf(
            "mana.launcher",
            "mana.brain",
            "mana.core",
            "mana.engine-service",
            "mana.encrypted-memory",
            "mana.microworld",
            "mana.cloud-gateway",
            "mana.yandex-gateway",
            "mana.self-development-contract"
        )
        val capabilities = linkedSetOf<String>()
        val modelFile = File(app.filesDir, "mana-models/selected-model.litertlm")
        if (modelFile.isFile && modelFile.canRead() && modelFile.length() > 0) {
            capabilities += "local_model_file_present"
        }
        if (Build.VERSION.SDK_INT >= 26) capabilities += "android_keystore_available"
        if (Build.VERSION.SDK_INT >= 29) capabilities += "scoped_storage_available"
        if (Build.VERSION.SDK_INT >= 31) capabilities += "android12_permission_model"
        if (Build.VERSION.SDK_INT >= 35) capabilities += "android15_foreground_service_policy"
        // These are observations only; never infer OEM source availability from model properties.
        if (Build.MANUFACTURER.isNotBlank() && Build.MODEL.isNotBlank()) {
            capabilities += "device_identity_observed"
        }
        return GenesisCapabilitySnapshot(
            deviceFingerprint = "${Build.MANUFACTURER}:${Build.DEVICE}:${Build.VERSION.SDK_INT}",
            androidApi = Build.VERSION.SDK_INT,
            installedModules = installed,
            availableCapabilities = capabilities
        )
    }

    private fun persist(snapshot: GenesisCapabilitySnapshot, plan: GenesisDevelopmentPlan) {
        val root = JSONObject()
            .put("schema", 1)
            .put("generatedAt", plan.generatedAt.toString())
            .put("deviceFingerprint", snapshot.deviceFingerprint)
            .put("androidApi", snapshot.androidApi)
            .put("installedModules", JSONArray(snapshot.installedModules.toList()))
            .put("availableCapabilities", JSONArray(snapshot.availableCapabilities.toList()))
        val ordered = JSONArray()
        plan.orderedWork.forEach { item ->
            ordered.put(JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("priority", item.priority.name)
                .put("status", item.status.name)
                .put("dependencies", JSONArray(item.dependencies.toList()))
                .put("requiredEvidence", JSONArray(item.requiredEvidence.toList()))
                .put("ownerApprovalRequired", item.ownerApprovalRequired))
        }
        val blocked = JSONArray()
        plan.blockedWork.forEach { item ->
            blocked.put(JSONObject()
                .put("id", item.id)
                .put("title", item.title)
                .put("status", item.status.name)
                .put("requiredEvidence", JSONArray(item.requiredEvidence.toList())))
        }
        root.put("orderedWork", ordered).put("blockedWork", blocked)
        val plain = root.toString().toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val payload = cipher.iv + cipher.doFinal(plain)
        require(payload.size <= 2 * 1024 * 1024) { "MANA first-boot checkpoint exceeds size limit." }
        file.parentFile?.mkdirs()
        val temp = File(file.parentFile, file.name + ".tmp")
        FileOutputStream(temp).use { stream ->
            stream.write(payload)
            stream.fd.sync()
        }
        if (file.exists() && !file.delete()) {
            temp.delete()
            error("Unable to replace prior MANA first-boot checkpoint.")
        }
        if (!temp.renameTo(file)) {
            temp.delete()
            error("Unable to atomically publish MANA first-boot checkpoint.")
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(keyAlias, null) as? SecretKey
        if (existing != null) return existing
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
