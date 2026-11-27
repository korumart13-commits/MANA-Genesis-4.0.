package com.mana.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.WindowManager
import android.hardware.Sensor
import android.hardware.SensorManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Runtime device discovery for ordinary Android installations.
 *
 * Only public Android APIs and app-accessible values are queried. A detected
 * capability is not treated as permission or privileged/system access.
 */
data class ManaDeviceFact(
    val key: String,
    val value: String,
    val status: String
)

data class ManaFeatureCapability(
    val id: String,
    val status: String,
    val reason: String
)

data class ManaUniversalDeviceProfile(
    val schemaVersion: Int,
    val generatedAtUptimeMs: Long,
    val facts: List<ManaDeviceFact>,
    val capabilities: List<ManaFeatureCapability>
) {
    fun toJson(): String {
        val root = JSONObject()
            .put("schemaVersion", schemaVersion)
            .put("generatedAtUptimeMs", generatedAtUptimeMs)
            .put("manufacturer", Build.MANUFACTURER)
            .put("model", Build.MODEL)
            .put("androidRelease", Build.VERSION.RELEASE)
            .put("apiLevel", Build.VERSION.SDK_INT)
        root.put("facts", JSONArray().apply {
            facts.forEach { put(JSONObject().put("key", it.key).put("value", it.value).put("status", it.status)) }
        })
        root.put("capabilities", JSONArray().apply {
            capabilities.forEach {
                put(JSONObject().put("id", it.id).put("status", it.status).put("reason", it.reason))
            }
        })
        return root.toString(2)
    }

    fun toReadableReport(): String = buildString {
        appendLine("MANA UNIVERSAL DEVICE AUDIT v$schemaVersion")
        facts.forEach { appendLine("${it.key}: ${it.value} [${it.status}]") }
        appendLine()
        appendLine("CAPABILITIES")
        capabilities.forEach { appendLine("${it.id}: ${it.status} — ${it.reason}") }
    }
}

object ManaUniversalDeviceProfiler {
    fun inspect(context: Context): ManaUniversalDeviceProfile {
        val app = context.applicationContext
        val pm = app.packageManager
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = ActivityManager.MemoryInfo().also { runCatching { am?.getMemoryInfo(it) } }
        val stat = runCatching { StatFs(Environment.getDataDirectory().absolutePath) }.getOrNull()
        val totalRam = memory.totalMem.coerceAtLeast(0L)
        val freeStorage = stat?.availableBytes ?: -1L
        val totalStorage = stat?.totalBytes ?: -1L
        val display = DisplayMetrics()
        runCatching {
            @Suppress("DEPRECATION")
            (app.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(display)
        }
        val sensorManager = app.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensorCount = runCatching { sensorManager?.getSensorList(Sensor.TYPE_ALL)?.size ?: 0 }.getOrDefault(0)
        val battery = runCatching {
            app.registerReceiver(null, IntentFilterCompat.batteryChanged())
        }.getOrNull()
        val batteryLevel = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (batteryLevel >= 0 && batteryScale > 0) "${(batteryLevel * 100) / batteryScale}%" else "NOT_ACCESSIBLE"
        val isLowRam = runCatching { am?.isLowRamDevice == true }.getOrDefault(false)
        val abis = Build.SUPPORTED_ABIS?.joinToString(",") ?: "UNKNOWN"
        val features = mapOf(
            "camera" to PackageManager.FEATURE_CAMERA_ANY,
            "microphone" to PackageManager.FEATURE_MICROPHONE,
            "bluetooth" to PackageManager.FEATURE_BLUETOOTH,
            "bluetooth_le" to PackageManager.FEATURE_BLUETOOTH_LE,
            "wifi" to PackageManager.FEATURE_WIFI,
            "nfc" to PackageManager.FEATURE_NFC,
            "gps" to PackageManager.FEATURE_LOCATION_GPS,
            "telephony" to PackageManager.FEATURE_TELEPHONY,
            "fingerprint" to PackageManager.FEATURE_FINGERPRINT,
            "vulkan" to PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL
        )
        val detected = features.mapValues { (_, feature) ->
            runCatching { pm.hasSystemFeature(feature) }.getOrDefault(false)
        }
        val arm64 = Build.SUPPORTED_ABIS?.any { it.equals("arm64-v8a", true) } == true
        val ramGiB = totalRam / (1024.0 * 1024.0 * 1024.0)
        val freeGiB = if (freeStorage >= 0) freeStorage / (1024.0 * 1024.0 * 1024.0) else -1.0
        val localInference = when {
            !arm64 -> "AVAILABLE_WITH_LIMITATIONS"
            totalRam <= 0L || freeStorage < 0L -> "UNKNOWN"
            ramGiB < 4.0 || freeGiB < 3.0 -> "AVAILABLE_WITH_LIMITATIONS"
            else -> "POTENTIAL_REQUIRES_RUNTIME_MODEL_TEST"
        }
        val capabilities = listOf(
            ManaFeatureCapability("launcher", "AVAILABLE", "Обычное приложение Android; доступно без системных привилегий"),
            ManaFeatureCapability("device_audit", "AVAILABLE", "Используются публичные API; недоступные поля помечаются отдельно"),
            ManaFeatureCapability("local_ai", localInference, "ABI=${abis}; RAM≈${"%.1f".format(Locale.US, ramGiB)} GiB; свободное хранилище≈${if (freeGiB >= 0) "%.1f".format(Locale.US, freeGiB) else "unknown"} GiB; реальный запуск модели требует отдельного теста"),
            ManaFeatureCapability("voice_input", if (detected["microphone"] == true) "AVAILABLE_WITH_PERMISSION" else "UNSUPPORTED", if (detected["microphone"] == true) "Требуется разрешение RECORD_AUDIO и совместимый speech service" else "Микрофон не обнаружен через PackageManager"),
            ManaFeatureCapability("camera", if (detected["camera"] == true) "AVAILABLE_WITH_PERMISSION" else "UNSUPPORTED", "Требуется разрешение камеры и проверка конкретного API"),
            ManaFeatureCapability("bluetooth_devices", if (detected["bluetooth"] == true) "AVAILABLE_WITH_PERMISSION" else "UNSUPPORTED", "На Android 12+ требуется BLUETOOTH_CONNECT для обнаружения/подключения"),
            ManaFeatureCapability("background_work", "AVAILABLE_WITH_LIMITATIONS", "Android и OEM могут откладывать или останавливать фоновые задачи"),
            ManaFeatureCapability("system_ui_replacement", "UNSUPPORTED_FOR_ORDINARY_APK", "Требуется системная подпись/интеграция в конкретную прошивку"),
            ManaFeatureCapability("firmware_flashing", "UNSUPPORTED_FOR_ORDINARY_APK", "Не выполняется; нужны точные OEM-образы, проверенная подпись и процедура восстановления")
        )
        val facts = listOf(
            ManaDeviceFact("manufacturer", Build.MANUFACTURER.ifBlank { "UNKNOWN" }, "DETECTED"),
            ManaDeviceFact("model", Build.MODEL.ifBlank { "UNKNOWN" }, "DETECTED"),
            ManaDeviceFact("device", Build.DEVICE.ifBlank { "UNKNOWN" }, "DETECTED"),
            ManaDeviceFact("product", Build.PRODUCT.ifBlank { "UNKNOWN" }, "DETECTED"),
            ManaDeviceFact("android_release", Build.VERSION.RELEASE ?: "UNKNOWN", "DETECTED"),
            ManaDeviceFact("api_level", Build.VERSION.SDK_INT.toString(), "DETECTED"),
            ManaDeviceFact("build_fingerprint", Build.FINGERPRINT ?: "UNKNOWN", "DETECTED"),
            ManaDeviceFact("supported_abis", abis, "DETECTED"),
            ManaDeviceFact("ram_total_bytes", if (totalRam > 0) totalRam.toString() else "NOT_ACCESSIBLE", if (totalRam > 0) "DETECTED" else "NOT_ACCESSIBLE"),
            ManaDeviceFact("ram_available_bytes", if (memory.availMem > 0) memory.availMem.toString() else "NOT_ACCESSIBLE", if (memory.availMem > 0) "DETECTED" else "NOT_ACCESSIBLE"),
            ManaDeviceFact("low_ram_device", isLowRam.toString(), "DETECTED"),
            ManaDeviceFact("storage_total_bytes", if (totalStorage >= 0) totalStorage.toString() else "NOT_ACCESSIBLE", if (totalStorage >= 0) "DETECTED" else "NOT_ACCESSIBLE"),
            ManaDeviceFact("storage_available_bytes", if (freeStorage >= 0) freeStorage.toString() else "NOT_ACCESSIBLE", if (freeStorage >= 0) "DETECTED" else "NOT_ACCESSIBLE"),
            ManaDeviceFact("display_pixels", "${display.widthPixels}x${display.heightPixels}", if (display.widthPixels > 0) "DETECTED" else "UNKNOWN"),
            ManaDeviceFact("display_density_dpi", display.densityDpi.toString(), if (display.densityDpi > 0) "DETECTED" else "UNKNOWN"),
            ManaDeviceFact("sensor_count", sensorCount.toString(), "DETECTED"),
            ManaDeviceFact("battery_percent", batteryPercent, if (batteryPercent != "NOT_ACCESSIBLE") "DETECTED" else "NOT_ACCESSIBLE"),
            ManaDeviceFact("hardware_features", detected.entries.joinToString(",") { "${it.key}=${it.value}" }, "DETECTED")
        )
        return ManaUniversalDeviceProfile(1, SystemClock.elapsedRealtime(), facts, capabilities)
    }
}

/** Isolated helper to keep battery discovery compatible with API 26+. */
private object IntentFilterCompat {
    fun batteryChanged(): android.content.IntentFilter =
        android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
}
