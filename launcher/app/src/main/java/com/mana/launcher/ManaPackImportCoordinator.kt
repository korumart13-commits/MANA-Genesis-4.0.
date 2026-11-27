package com.mana.launcher

import android.content.Context
import android.net.Uri
import android.os.Build
import com.mana.os.core.ManaModuleInstaller
import com.mana.os.core.ManaPackVerifier
import java.io.File
import java.io.FileOutputStream
import java.security.PublicKey

/**
 * Android bridge for importing a user-selected .manapack into MANA's private module store.
 * The caller must supply the already trusted release key and a device identity produced by
 * the read-only compatibility probe. It never requests root or installs APKs silently.
 */
class ManaPackImportCoordinator(private val context: Context) {
    fun importAndApply(
        uri: Uri,
        trustedReleaseKey: PublicKey,
        verifiedDeviceIdentity: String
    ): List<String> {
        require(verifiedDeviceIdentity.isNotBlank()) { "Device identity has not been verified." }
        val importDir = File(context.cacheDir, "mana-pack-import").apply {
            require(exists() || mkdirs()) { "Cannot create import cache." }
        }
        val source = File.createTempFile("incoming-", ".manapack", importDir)
        val stageRoot = File(context.filesDir, "mana/packs/staging").apply {
            require(exists() || mkdirs()) { "Cannot create pack staging directory." }
        }
        var stage: File? = null
        try {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Android could not open the selected .manapack file.")
            input.use { stream ->
                FileOutputStream(source).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    while (true) {
                        val count = stream.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= MAX_PACKAGE_BYTES) { ".manapack exceeds the 4 GiB limit." }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }

            val verified = ManaPackVerifier(
                trustedKey = trustedReleaseKey,
                device = verifiedDeviceIdentity,
                osVersion = BuildConfig.VERSION_NAME,
                apiLevel = Build.VERSION.SDK_INT,
                maxTotalPayloadBytes = MAX_PACKAGE_BYTES
            ).verify(source, stageRoot)
            stage = verified.stagingDirectory
            val moduleRoot = File(context.filesDir, "mana/modules")
            val installed = moduleRoot.listFiles()?.filter { it.isDirectory }?.map { it.name }?.toSet().orEmpty()
            return ManaModuleInstaller(moduleRoot).apply(verified, installed)
        } finally {
            source.delete()
            stage?.deleteRecursively()
        }
    }

    companion object {
        private const val MAX_PACKAGE_BYTES = 4L * 1024 * 1024 * 1024
    }
}
