package com.mana.launcher

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.security.MessageDigest

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
        if (target.isFile && target.length() == MODEL_SIZE && sha256(target) == SHA256) {
            prefs.edit().putString(KEY_ACTIVE_PATH, target.absolutePath).apply()
            saveVerifiedMetadata(target)
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

    /** Downloads the high-capacity model only after explicit owner consent. */
    fun enqueueGemma4E4B(licenseAccepted: Boolean): Long {
        require(licenseAccepted) { "Owner must review the Gemma terms before downloading this model." }
        prefs.edit().putBoolean(KEY_E4B_LICENSE_ACCEPTED, true).apply()
        val target = File(directory, E4B_MODEL_FILE)
        if (target.isFile && target.length() == E4B_MODEL_SIZE && sha256(target).equals(E4B_SHA256, true)) {
            prefs.edit().putString(KEY_E4B_ACTIVE_PATH, target.absolutePath).apply()
            return -1L
        }
        if (target.exists()) require(target.delete()) { "Не удалось удалить повреждённую модель E4B." }
        require(directory.usableSpace >= E4B_MODEL_SIZE + 500_000_000L) {
            "Для Gemma 4 E4B требуется не менее 4,16 ГБ свободного места."
        }
        val request = DownloadManager.Request(Uri.parse(E4B_MODEL_URL))
            .setTitle("MANA · Gemma 4 E4B")
            .setDescription("Официальная локальная модель LiteRT-LM, 3,66 ГБ. Загрузка только по Wi-Fi.")
            .setMimeType("application/octet-stream")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(false)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, E4B_MODEL_FILE)
        val id = manager.enqueue(request)
        prefs.edit().putLong(KEY_E4B_DOWNLOAD_ID, id).apply()
        return id
    }

    /** Copies the E4B model embedded in the full APK into app-specific storage, then verifies it. */
    fun installBundledGemma4E4B(licenseAccepted: Boolean): File {
        require(licenseAccepted) { "Owner must review the Gemma terms before importing this bundled model." }
        prefs.edit().putBoolean(KEY_E4B_LICENSE_ACCEPTED, true).apply()
        val target = File(directory, E4B_MODEL_FILE)
        if (target.isFile && target.length() == E4B_MODEL_SIZE &&
            sha256(target).equals(E4B_SHA256, ignoreCase = true)) {
            prefs.edit().putString(KEY_E4B_ACTIVE_PATH, target.absolutePath).apply()
            return target
        }
        require(directory.usableSpace >= E4B_MODEL_SIZE + 500_000_000L) {
            "Для распаковки встроенной Gemma 4 E4B требуется около 4,16 ГБ свободного места."
        }
        val temporary = File(directory, "$E4B_MODEL_FILE.importing")
        if (temporary.exists()) require(temporary.delete()) { "Не удалось очистить незавершённый импорт модели." }
        try {
            app.assets.open(E4B_MODEL_FILE).buffered().use { input ->
                java.io.FileOutputStream(temporary).buffered().use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                    output.flush()
                }
            }
            require(temporary.isFile && temporary.length() == E4B_MODEL_SIZE) {
                "Размер встроенной Gemma 4 E4B не совпал с каталогом."
            }
            require(sha256(temporary).equals(E4B_SHA256, ignoreCase = true)) {
                "SHA-256 встроенной Gemma 4 E4B не совпал. Импорт отменён."
            }
            if (target.exists()) require(target.delete()) { "Не удалось заменить предыдущую модель E4B." }
            require(temporary.renameTo(target)) { "Не удалось сохранить проверенную модель E4B." }
            prefs.edit().putString(KEY_E4B_ACTIVE_PATH, target.absolutePath).apply()
            return target
        } catch (error: Exception) {
            temporary.delete()
            throw error
        }
    }

    /** Returns null while the E4B download is still running; verifies exact size and SHA-256. */
    fun verifyCompletedGemma4E4BDownload(): File? {
        if (!prefs.getBoolean(KEY_E4B_LICENSE_ACCEPTED, false)) return null
        val target = File(directory, E4B_MODEL_FILE)
        val id = prefs.getLong(KEY_E4B_DOWNLOAD_ID, -1L)
        if (id < 0L) {
            if (target.isFile && target.length() == E4B_MODEL_SIZE && sha256(target).equals(E4B_SHA256, true)) return target
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
        if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PAUSED) return null
        if (status != DownloadManager.STATUS_SUCCESSFUL) {
            prefs.edit().remove(KEY_E4B_DOWNLOAD_ID).apply()
            throw IllegalStateException("Загрузка Gemma 4 E4B не завершилась (код $reason). Проверь Wi-Fi и свободное место.")
        }
        require(target.isFile && target.length() == E4B_MODEL_SIZE) { "Размер Gemma 4 E4B не совпал с ожидаемым." }
        if (!sha256(target).equals(E4B_SHA256, true)) {
            target.delete()
            prefs.edit().remove(KEY_E4B_DOWNLOAD_ID).apply()
            throw SecurityException("SHA-256 Gemma 4 E4B не совпал. Файл удалён.")
        }
        prefs.edit().putString(KEY_E4B_ACTIVE_PATH, target.absolutePath).remove(KEY_E4B_DOWNLOAD_ID).apply()
        return target
    }

    fun verifiedSavedModel(): File? {
        if (!prefs.getBoolean(KEY_LICENSE_ACCEPTED, false)) return null
        val path = savedModelPath() ?: return null
        val file = File(path)
        if (!file.isFile || file.length() != MODEL_SIZE) {
            prefs.edit().remove(KEY_ACTIVE_PATH).remove(KEY_VERIFIED_LENGTH).remove(KEY_VERIFIED_MTIME).apply()
            return null
        }
        val cached = prefs.getLong(KEY_VERIFIED_LENGTH, -1L) == file.length() &&
            prefs.getLong(KEY_VERIFIED_MTIME, -1L) == file.lastModified()
        if (!cached && !sha256(file).equals(SHA256, ignoreCase = true)) {
            prefs.edit().remove(KEY_ACTIVE_PATH).remove(KEY_VERIFIED_LENGTH).remove(KEY_VERIFIED_MTIME).apply()
            return null
        }
        if (!cached) saveVerifiedMetadata(file)
        return file
    }

    /** Returns null while queued/running; throws for a failed or invalid completed download. */
    fun verifyCompletedDownload(): File? {
        val id = prefs.getLong(KEY_DOWNLOAD_ID, -1L)
        if (id < 0L) {
            if (!prefs.getBoolean(KEY_LICENSE_ACCEPTED, false)) return null
            // A previously verified file can be selected again in this process without
            // requiring a second DownloadManager record.
            val existing = File(directory, MODEL_FILE)
            if (existing.isFile && existing.length() == MODEL_SIZE &&
                sha256(existing).equals(SHA256, ignoreCase = true)) {
                prefs.edit().putString(KEY_ACTIVE_PATH, existing.absolutePath).apply()
                saveVerifiedMetadata(existing)
                return existing
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
        require(file.isFile && file.length() == MODEL_SIZE) {
            "Размер загруженной модели не совпадает с ожидаемым диапазоном."
        }
        val actual = sha256(file)
        if (!actual.equals(SHA256, ignoreCase = true)) {
            file.delete()
            prefs.edit().remove(KEY_DOWNLOAD_ID).apply()
            throw SecurityException("SHA-256 модели не совпал с официальным значением. Файл удалён.")
        }
        prefs.edit().putString(KEY_ACTIVE_PATH, file.absolutePath).remove(KEY_DOWNLOAD_ID).apply()
        saveVerifiedMetadata(file)
        return file
    }

    private fun saveVerifiedMetadata(file: File) {
        prefs.edit().putLong(KEY_VERIFIED_LENGTH, file.length())
            .putLong(KEY_VERIFIED_MTIME, file.lastModified()).apply()
    }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
    }

    companion object {
        const val MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/21c646194210256750db12cfa1e4fdf22bd3ddf1/gemma-4-E2B-it.litertlm"
        const val MODEL_FILE = "gemma-4-E2B-it.litertlm"
        const val SHA256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c"
        private val KEY_DOWNLOAD_ID = "gemma4_download_id"
        private val KEY_LICENSE_ACCEPTED = "gemma4_license_accepted"
        private val KEY_ACTIVE_PATH = "active_model_path"
        private val KEY_VERIFIED_LENGTH = "verified_model_length"
        private val KEY_VERIFIED_MTIME = "verified_model_mtime"
        const val MODEL_SIZE = 2_588_147_712L
        private val REQUIRED_FREE_BYTES = 3_300_000_000L
        const val E4B_MODEL_URL = "https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm/resolve/2eee7ac325f20eb8c9ac1d0e972f7c84663062da/gemma-4-E4B-it.litertlm"
        const val E4B_MODEL_FILE = "gemma-4-E4B-it.litertlm"
        const val E4B_MODEL_SIZE = 3_659_530_240L
        const val E4B_SHA256 = "0b2a8980ce155fd97673d8e820b4d29d9c7d99b8fa6806f425d969b145bd52e0"
        private const val KEY_E4B_DOWNLOAD_ID = "gemma4_e4b_download_id"
        private const val KEY_E4B_LICENSE_ACCEPTED = "gemma4_e4b_license_accepted"
        private const val KEY_E4B_ACTIVE_PATH = "gemma4_e4b_active_path"
    }
}
