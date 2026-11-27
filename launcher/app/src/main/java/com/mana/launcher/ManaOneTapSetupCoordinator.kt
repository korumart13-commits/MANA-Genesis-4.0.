package com.mana.launcher

import android.content.Context
import android.os.Build
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

/**
 * Idempotent one-action bootstrap for the portable MANA APK.
 *
 * It performs every setup step available to an ordinary app without user interaction:
 * device profiling, encrypted first-boot checkpointing, persistent WorkManager
 * scheduling, and a private compatibility report. Android-owned permissions,
 * assistant-role selection, overlay access, Device Owner, root and firmware access
 * are never fabricated or silently granted.
 */
class ManaOneTapSetupCoordinator(context: Context) {
    private val app = context.applicationContext
    private val reportFile = File(app.filesDir, "mana/setup-report.json")
    private val preferences = app.getSharedPreferences("mana.one_tap_setup", Context.MODE_PRIVATE)

    data class Report(
        val deviceName: String,
        val androidVersion: String,
        val apiLevel: Int,
        val isTargetDevice: Boolean,
        val completed: List<String>,
        val ownerActions: List<String>,
        val unavailable: List<String>,
        val reportPath: String
    ) {
        fun toReadableText(): String = buildString {
            appendLine("MANA GENESIS 4.0 — ПЕРВИЧНАЯ НАСТРОЙКА")
            appendLine("$deviceName • Android $androidVersion • API $apiLevel")
            appendLine("Целевой профиль RMX5566RU: ${if (isTargetDevice) "ПОДТВЕРЖДЁН" else "НЕ ПОДТВЕРЖДЁН"}")
            appendLine()
            appendLine("ВЫПОЛНЕНО АВТОМАТИЧЕСКИ")
            completed.forEach { appendLine("✓ $it") }
            if (ownerActions.isNotEmpty()) {
                appendLine()
                appendLine("ТРЕБУЕТСЯ ДЕЙСТВИЕ ВЛАДЕЛЬЦА")
                ownerActions.forEach { appendLine("• $it") }
            }
            if (unavailable.isNotEmpty()) {
                appendLine()
                appendLine("ОГРАНИЧЕНИЯ ЭТОГО APK")
                unavailable.forEach { appendLine("• $it") }
            }
            appendLine()
            append("Подробный отчёт сохранён в приватной папке MANA.")
        }
    }

    @Synchronized
    fun run(): Report {
        val completed = mutableListOf<String>()
        val ownerActions = mutableListOf<String>()
        val unavailable = mutableListOf<String>()

        val profile = ManaUniversalDeviceProfiler.inspect(app)
        val targetMatch = isExactRmx5566Target()
        completed += "Автоматически определены производитель, модель, версия Android, ABI, память, хранилище и аппаратные возможности."
        if (targetMatch) {
            completed += "Подтверждён целевой аппарат realme RMX5566RU / RE645F на Android 16."
        } else {
            unavailable += "Этот релиз предназначен только для realme RMX5566RU / RE645F на Android 16 (Snapdragon 685 / SM6225). Настройка целевых функций остановлена."
        }

        if (targetMatch) {
            runCatching {
                ManaGenesisFirstBootService(app).refresh()
                completed += "Создан или обновлён зашифрованный снимок состояния MANA Genesis."
            }.onFailure { error ->
                unavailable += "Снимок первого запуска: ${error.javaClass.simpleName}: ${error.message ?: "ошибка"}."
            }

            runCatching {
                ManaGenesisWorkScheduler.ensureScheduled(app)
                completed += "Фоновые задачи MANA зарегистрированы через WorkManager; фактическое время запуска регулирует Android."
            }.onFailure { error ->
                unavailable += "Планировщик фоновых задач: ${error.javaClass.simpleName}: ${error.message ?: "ошибка"}."
            }
        }

        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(app)) {
            ownerActions += "Разрешить плавающий аватар поверх других приложений."
        }
        if (app.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ownerActions += "Разрешить микрофон для голосового общения."
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            app.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ownerActions += "Разрешить уведомления MANA."
        }
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = app.getSystemService(android.app.role.RoleManager::class.java)
            if (roleManager?.isRoleAvailable(android.app.role.RoleManager.ROLE_ASSISTANT) == true &&
                !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_ASSISTANT)) {
                ownerActions += "При желании назначить MANA цифровым ассистентом в системном диалоге."
            }
        }

        profile.capabilities.filter {
            it.status == "UNSUPPORTED_FOR_ORDINARY_APK"
        }.forEach { capability ->
            unavailable += "${capability.id}: ${capability.reason}."
        }

        val reportJson = JSONObject()
            .put("schemaVersion", 1)
            .put("generatedAtEpochMs", System.currentTimeMillis())
            .put("manufacturer", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("device", Build.DEVICE)
            .put("product", Build.PRODUCT)
            .put("androidRelease", Build.VERSION.RELEASE)
            .put("apiLevel", Build.VERSION.SDK_INT)
            .put("targetDevice", "realme RMX5566RU / RE645F")
            .put("targetDeviceMatch", targetMatch)
            .put("profile", JSONObject(profile.toJson()))
            .put("completed", JSONArray(completed))
            .put("ownerActions", JSONArray(ownerActions))
            .put("unavailable", JSONArray(unavailable))

        publishReport(reportJson.toString(2))
        preferences.edit()
            .putBoolean("bootstrap_attempted", true)
            .putLong("last_bootstrap_epoch_ms", System.currentTimeMillis())
            .putString("last_device_fingerprint", Build.FINGERPRINT)
            .apply()

        return Report(
            deviceName = listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Android device" },
            androidVersion = Build.VERSION.RELEASE ?: "unknown",
            apiLevel = Build.VERSION.SDK_INT,
            isTargetDevice = targetMatch,
            completed = completed,
            ownerActions = ownerActions,
            unavailable = unavailable,
            reportPath = reportFile.absolutePath
        )
    }

    private fun isExactRmx5566Target(): Boolean {
        val manufacturerMatches = Build.MANUFACTURER.equals("realme", ignoreCase = true)
        val modelMatches = Build.MODEL.contains("RMX5566", ignoreCase = true)
        val deviceMatches = Build.DEVICE.equals("RE645F", ignoreCase = true)
        val productMatches = Build.PRODUCT.equals("RE645F", ignoreCase = true)
        val androidMatches = Build.VERSION.SDK_INT == 36
        val fingerprintMatches = Build.FINGERPRINT.startsWith("realme/RMX5566RU/RE645F:16/")
        return manufacturerMatches && modelMatches && deviceMatches &&
            productMatches && androidMatches && fingerprintMatches
    }

    private fun publishReport(contents: String) {
        val parent = reportFile.parentFile ?: error("MANA private directory is unavailable")
        check(parent.exists() || parent.mkdirs()) { "Cannot create MANA private directory" }
        val temp = File(parent, reportFile.name + ".tmp")
        FileOutputStream(temp).use { stream ->
            stream.write(contents.toByteArray(StandardCharsets.UTF_8))
            stream.fd.sync()
        }
        if (reportFile.exists() && !reportFile.delete()) {
            temp.delete()
            error("Cannot replace previous MANA setup report")
        }
        if (!temp.renameTo(reportFile)) {
            temp.delete()
            error("Cannot publish MANA setup report")
        }
    }
}
