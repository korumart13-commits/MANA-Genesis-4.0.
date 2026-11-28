package com.mana.launcher

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * Official owner-initiated Gemma 4 E2B LiteRT-LM download.
 * Uses DownloadManager for resumable background transfer, then verifies the
 * publisher's SHA-256 before making the model selectable.
 */
class ManaModelCatalog(context: Context) {
    private val app = context.applicationContext
    private val manager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = app.getSharedPreferences("mana-model-catalog", Context.MODE_PRIVATE)
    private val directory = app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        ?: error("Android app-specific downloads storage is unavailable.")

    fun enqueueGemma4E2B(licenseAccepted: Boolean): Long {
        require(licenseAccepted) { "Owner must review and accept the Gemma terms before downloading this model." }
        prefs.edit().putBoolean(KEY_LICENSE_ACCEPTED, true).apply()
        val target = File(directory, MODEL_FILE)
        if (ModelArtifactIntegrity.matches(target, MODEL_SIZE, SHA256, directory)) {
            prefs.edit().putString(KEY_ACTIVE_PATH, target.canonicalPath).apply()
            return -1L
        }
        if (target.exists()) require(target.delete()) { "Не удалось удалить повреждённый предыдущий файл модели." }
        require(directory.usableSpace >= REQUIRED_FREE_BYTES) {
            "Для модели Gemma 4 E2B требуется около 2,59 ГБ и дополнительный запас свободного места."
        }
        val request = DownloadManager.Request(Uri.parse(MODEL_URL))
            .setTitle("MANA · Gemma 4 E2B")
            .setDescription("Официальная модель LiteRT-LM, 2,59 ГБ. Загрузка только через Wi-Fi.")
            .setMimeType("application/octet-stream")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, MODEL_FILE)
        val id = manager.enqueue(request)
        prefs.edit().putLong(KEY_DOWNLOAD_ID, id).apply()
        return id
    }

    fun savedModelPath(): String? = if (prefs.getBoolean(KEY_LICENSE_ACCEPTED, false)) prefs.getString(KEY_ACTIVE_PATH, null) else null

    /** Matches only this catalog's active DownloadManager request. */
    fun isTrackedDownload(downloadId: Long): Boolean =
        downloadId >= 0L && prefs.getLong(KEY_DOWNLOAD_ID, -1L) == downloadId

    fun verifiedSavedModel(): File? {
        if (!prefs.getBoolean(KEY_LICENSE_ACCEPTED, false)) return null
        val path = savedModelPath() ?: return null
        val file = File(path)
        if (!ModelArtifactIntegrity.matches(file, MODEL_SIZE, SHA256, directory)) {
            prefs.edit().remove(KEY_ACTIVE_PATH).apply()
            return null
        }
        return file.canonicalFile
    }

    /** Returns null while queued/running; throws for a failed or invalid completed download. */
    fun verifyCompletedDownload(): File? {
        val id = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id < 0L) {
            if (!prefs.getBoolean(KEY_LICENSE_ACCEPTED, false)) return null
            // A previously verified file can be selected again in this process without
            // requiring a second DownloadManager record.
            val existing = File(directory, MODEL_FILE)
            if (ModelArtifactIntegrity.matches(existing, MODEL_SIZE, SHA256, directory)) {
                prefs.edit().putString(KEY_ACTIVE_PATH, existing.canonicalPath).apply()
                return existing.canonicalFile
            }
            return null
        }
        val cursor = manager.query(DownloadManager.Query().setFilterById(id))
        val status: Int
        val reason: Int
        cursor.use {
            if (!it.moveToFirst()) return null
            status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
        }
        if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING ||
            status == DownloadManager.STATUS_PAUSED) return null
        if (status != DownloadManager.STATUS_SUCCESSFUL) {
            prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
            throw IllegalStateException("Загрузка модели не завершилась (код $reason). Проверь Wi-Fi и свободное место.")
        }
        val file = File(directory, MODEL_FILE)
        if (!ModelArtifactIntegrity.matches(file, MODEL_SIZE, SHA256, directory)) {
            file.delete()
            prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
            throw SecurityException("SHA-256 модели не совпал с официальным значением. Файл удалён.")
        }
        prefs.edit().putString(KEY_ACTIVE_PATH, file.canonicalPath).remove(KEY_DOWNLOAD_ID).apply()
        return file.canonicalFile
    }

    companion object {
        const val MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/21c646194210256750db12cfa1e4fdf22bd3ddf1/gemma-4-E2B-it.litertlm"
        const val MODEL_FILE = "gemma-4-E2B-it.litertlm"
        const val SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
        private val KEY_DOWNLOAD_ID = "gemma4_download_id"
        private val KEY_LICENSE_ACCEPTED = "gemma4_license_accepted"
        private val KEY_ACTIVE_PATH = "active_model_path"
        const val MODEL_SIZE = 2_588_147_712L
        private val REQUIRED_FREE_BYTES = 3_300_000_000L
    }
}
