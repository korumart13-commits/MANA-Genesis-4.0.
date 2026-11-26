package com.mana.launcher

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * MANA Device Intelligence: gathers public Android hardware/build facts and
 * generates a device-specific ROM porting blueprint. It does not flash or
 * write protected firmware partitions.
 */
class ManaDeviceIntelligenceActivity : Activity() {
    private val navy = Color.rgb(7, 20, 38)
    private val cardColor = Color.rgb(16, 43, 80)
    private val accent = Color.rgb(84, 200, 245)
    private val white = Color.rgb(234, 244, 255)
    private val muted = Color.rgb(145, 169, 197)
    private var latestReport: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(0, dp(3), 0, dp(5))
        }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = GradientDrawable().apply {
            setColor(cardColor)
            cornerRadius = dp(24).toFloat()
        }
        val lp = LinearLayout.LayoutParams(-1, -2)
        lp.bottomMargin = dp(12)
        layoutParams = lp
    }

    private fun action(title: String, onClick: () -> Unit) = TextView(this).apply {
        text = title
        textSize = 15f
        setTextColor(white)
        gravity = Gravity.CENTER
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = GradientDrawable().apply {
            setColor(Color.rgb(28, 94, 151))
            cornerRadius = dp(18).toFloat()
        }
        isClickable = true
        isFocusable = true
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) }
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(navy)
        }
        root.addView(text("Устройство", 31f, white, true))
        root.addView(text("MANA Device Intelligence", 14f, accent))
        root.addView(text("Сведения о телефоне, компонентах и подготовке персональной сборки MANA", 13f, muted))
        val report = collectDeviceFacts()
        latestReport = report
        root.addView(summaryCard(report))
        root.addView(sectionCard("Система и прошивка", report.getJSONObject("system")))
        root.addView(sectionCard("Процессор и память", report.getJSONObject("compute")))
        root.addView(sectionCard("Экран и питание", report.getJSONObject("display_power")))
        root.addView(sectionCard("Камеры, датчики и функции", report.getJSONObject("hardware")))
        root.addView(sectionCard("Хранилище", report.getJSONObject("storage")))
        root.addView(action("Обновить сведения о телефоне") { render(); toast("Данные обновлены.") })
        root.addView(action("Создать пакет адаптации прошивки MANA") { generatePortBlueprint(report) })
        root.addView(action("Поделиться полным отчётом") { shareReport(report) })
        root.addView(text(
            "Генератор создаёт индивидуальный набор исходных шаблонов и отчёт совместимости для дальнейшей сборки ROM. Сам APK не записывает boot/system/vendor/vbmeta и не заменяет штатную прошивку.",
            12f, muted
        ))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun summaryCard(report: JSONObject): LinearLayout = card().apply {
        addView(text(Build.MODEL.ifBlank { "Телефон Android" }, 22f, white, true))
        addView(text("${Build.MANUFACTURER} • Android ${Build.VERSION.RELEASE} • API ${Build.VERSION.SDK_INT}", 13f, accent))
        addView(text("Собрано: ${report.getString("collected_at")}", 11f, muted))
    }

    private fun sectionCard(title: String, values: JSONObject): LinearLayout = card().apply {
        addView(text(title, 18f, white, true))
        val keys = values.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            addView(text(key, 11f, muted))
            addView(text(values.optString(key, "не определено"), 13f, white))
        }
    }

    private fun collectDeviceFacts(): JSONObject {
        val root = JSONObject()
        root.put("collected_at", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", java.util.Locale.US).format(java.util.Date()))
        root.put("system", JSONObject().apply {
            put("manufacturer", Build.MANUFACTURER)
            put("brand", Build.BRAND)
            put("model", Build.MODEL)
            put("device", Build.DEVICE)
            put("product", Build.PRODUCT)
            put("hardware", Build.HARDWARE)
            put("board", Build.BOARD)
            put("bootloader", Build.BOOTLOADER)
            put("android_release", Build.VERSION.RELEASE)
            put("sdk", Build.VERSION.SDK_INT)
            put("security_patch", if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "n/a")
            put("build_display", Build.DISPLAY)
            put("build_id", Build.ID)
            put("fingerprint", Build.FINGERPRINT)
            put("tags", Build.TAGS)
            put("type", Build.TYPE)
            put("kernel", System.getProperty("os.version") ?: "unknown")
            put("supported_abis", Build.SUPPORTED_ABIS.joinToString(", "))
            put("supported_32_bit_abis", Build.SUPPORTED_32_BIT_ABIS.joinToString(", "))
            put("supported_64_bit_abis", Build.SUPPORTED_64_BIT_ABIS.joinToString(", "))
        })
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memory = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memory)
        val cores = Runtime.getRuntime().availableProcessors()
        root.put("compute", JSONObject().apply {
            put("cpu_cores_visible_to_app", cores)
            put("cpu_abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
            put("physical_ram_total_bytes", memory.totalMem)
            put("ram_available_bytes", memory.availMem)
            put("low_memory", memory.lowMemory)
            put("memory_threshold_bytes", memory.threshold)
            put("max_heap_bytes", Runtime.getRuntime().maxMemory())
        })
        val display = resources.displayMetrics
        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val screen = JSONObject()
        screen.put("resolution_px", "${display.widthPixels} x ${display.heightPixels}")
        screen.put("density_dpi", display.densityDpi)
        screen.put("density_scale", display.density)
        screen.put("refresh_rate_hz", runCatching {
            @Suppress("DEPRECATION")
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate
        }.getOrDefault(0f))
        screen.put("battery_percent", batteryPct)
        screen.put("battery_status", when(status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
            BatteryManager.BATTERY_STATUS_FULL -> "full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not charging"
            else -> "unknown"
        })
        root.put("display_power", screen)
        val sensors = (getSystemService(Context.SENSOR_SERVICE) as SensorManager).getSensorList(SensorManager.TYPE_ALL)
        val cameraNames = runCatching {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            manager.cameraIdList.map { id ->
                val chars = manager.getCameraCharacteristics(id)
                val facing = when(chars.get(CameraCharacteristics.LENS_FACING)) {
                    CameraCharacteristics.LENS_FACING_FRONT -> "front"
                    CameraCharacteristics.LENS_FACING_BACK -> "back"
                    CameraCharacteristics.LENS_FACING_EXTERNAL -> "external"
                    else -> "unknown"
                }
                "$id:$facing"
            }.joinToString(", ")
        }.getOrDefault("unavailable")
        val features = packageManager.systemAvailableFeatures.mapNotNull { it.name }.sorted()
        root.put("hardware", JSONObject().apply {
            put("camera_ids_and_facing", cameraNames)
            put("sensor_count", sensors.size)
            put("sensors", sensors.joinToString("; ") { "${it.name} [${it.vendor}]" })
            put("system_features", features.joinToString(", "))
            put("abis", Build.SUPPORTED_ABIS.joinToString(", "))
        })
        val data = Environment.getDataDirectory()
        val stat = StatFs(data.path)
        root.put("storage", JSONObject().apply {
            put("data_total_bytes", stat.totalBytes)
            put("data_available_bytes", stat.availableBytes)
            put("external_app_storage_available_bytes", filesDir.usableSpace)
            put("external_storage_state", Environment.getExternalStorageState())
        })
        return root
    }

    private fun generatePortBlueprint(report: JSONObject) {
        try {
            val dir = File(getExternalFilesDir(null) ?: filesDir, "MANA_Firmware_Generation")
            if (!dir.exists() && !dir.mkdirs()) error("Cannot create output directory")
            val zip = File(dir, "MANA_Device_Port_Blueprint.zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zip))).use { out ->
                addZip(out, "device-report.json", report.toString(2))
                addZip(out, "README_RU.txt", """
MANA GENESIS 4.0 — DEVICE PORT BLUEPRINT
Это индивидуально сгенерированный исходный пакет для анализа и разработки порта.
Он не является прошивкой и не предназначен для прямой установки.

Следующие данные необходимо подтвердить по исходникам производителя:
- device tree и точный SKU/регион;
- vendor blobs и HAL;
- kernel source/config и DTB/DTBO;
- VINTF/manifest compatibility;
- AVB/vbmeta signing and partition layout;
- OTA payload format and rollback indexes.

Нельзя подменять неизвестные значения предположениями. Все шаблоны ниже помечены как TODO.
""".trimIndent())
                addZip(out, "device/BoardConfig.mk.template", boardConfigTemplate(report))
                addZip(out, "device/device.mk.template", deviceMkTemplate(report))
                addZip(out, "device/AndroidProducts.mk.template", androidProductsTemplate(report))
                addZip(out, "device/README_DEVICE_TREE_TODO.md", deviceTreeTodo(report))
                addZip(out, "firmware/patch-plan.json", JSONObject().apply {
                    put("target", report.getJSONObject("system"))
                    put("status", "generated_blueprint_only")
                    put("binary_firmware_generated", false)
                    put("requires_vendor_sources", true)
                    put("requires_device_tree", true)
                    put("requires_oem_signing", true)
                }.toString(2))
            }
            Toast.makeText(this, "Пакет создан: ${zip.absolutePath}", Toast.LENGTH_LONG).show()
            val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", zip)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(share, "Сохранить или отправить пакет MANA"))
        } catch (e: Exception) {
            toast("Не удалось создать пакет: ${e.message}")
        }
    }

    private fun addZip(out: ZipOutputStream, name: String, value: String) {
        out.putNextEntry(ZipEntry(name))
        out.write(value.toByteArray(Charsets.UTF_8))
        out.closeEntry()
    }

    private fun boardConfigTemplate(report: JSONObject) = """
# GENERATED TEMPLATE — NOT FLASHABLE
# Device: ${report.getJSONObject("system").optString("device")}
# TODO: Replace every unknown value with verified OEM/source data.
TARGET_BOOTLOADER_BOARD_NAME := TODO_VERIFY_BOARD
TARGET_ARCH := arm64
TARGET_ARCH_VARIANT := TODO_VERIFY
TARGET_CPU_VARIANT := TODO_VERIFY
BOARD_KERNEL_IMAGE_NAME := Image
BOARD_BOOT_HEADER_VERSION := TODO_VERIFY
BOARD_VENDOR_BOOTIMAGE_PARTITION_SIZE := TODO_VERIFY
BOARD_BOOTIMAGE_PARTITION_SIZE := TODO_VERIFY
BOARD_SUPER_PARTITION_SIZE := TODO_VERIFY
BOARD_USES_RECOVERY_AS_BOOT := TODO_VERIFY
BOARD_AVB_ENABLE := true
""".trimIndent()

    private fun deviceMkTemplate(report: JSONObject) = """
# GENERATED TEMPLATE — NOT FLASHABLE
PRODUCT_DEVICE := ${report.getJSONObject("system").optString("device")}
PRODUCT_NAME := mana_${report.getJSONObject("system").optString("device")}
PRODUCT_BRAND := ${report.getJSONObject("system").optString("brand")}
PRODUCT_MODEL := ${report.getJSONObject("system").optString("model")}
# TODO: inherit exact OEM device/vendor makefiles only after source verification.
""".trimIndent()

    private fun androidProductsTemplate(report: JSONObject) = """
# GENERATED TEMPLATE — NOT FLASHABLE
PRODUCT_MAKEFILES := \$(LOCAL_DIR)/mana_${report.getJSONObject("system").optString("device")}.mk
COMMON_LUNCH_CHOICES := mana_${report.getJSONObject("system").optString("device")}-userdebug
""".trimIndent()

    private fun deviceTreeTodo(report: JSONObject) = """
# Device tree verification checklist
Target: ${report.getJSONObject("system").optString("manufacturer")} ${report.getJSONObject("system").optString("model")}
Fingerprint: ${report.getJSONObject("system").optString("fingerprint")}
Do not infer vendor blobs, partition sizes, kernel config, or AVB keys from model names.
Record the exact source URL, revision, license and SHA-256 for each imported component.
""".trimIndent()

    private fun shareReport(report: JSONObject) {
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_SUBJECT, "MANA — полный отчёт об устройстве")
            putExtra(Intent.EXTRA_TEXT, report.toString(2))
        }
        startActivity(Intent.createChooser(share, "Поделиться отчётом"))
    }

    private fun toast(value: String) = Toast.makeText(this, value, Toast.LENGTH_LONG).show()
}
