package com.mana.launcher

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Local paired-device dashboard. Pairing and network changes are delegated to
 * Android system settings; no hidden scanning, MAC-address export or vehicle
 * control commands are performed.
 */
class ManaDeviceEcosystemActivity : Activity() {
    private val background = 0xFF04070E.toInt()
    private val panel = 0xFF0D203B.toInt()
    private val foreground = 0xFFEEF6FF.toInt()
    private val muted = 0xFF9AB0C9.toInt()
    private val accent = 0xFF48B4FF.toInt()
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background
        window.navigationBarColor = background
        val scroll = ScrollView(this).apply { setBackgroundColor(background) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(24))
        }
        scroll.addView(content)
        setContentView(scroll)
        renderHeader()
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_CONNECT)
        } else {
            renderDevices()
        }
    }

    private fun renderHeader() {
        content.addView(text("MANA DEVICES", 28, foreground, true))
        content.addView(text("Устройства и подключения", 14, accent, true))
        content.addView(text(
            "MANA показывает уже сопряжённые Bluetooth-устройства. Поиск новых устройств и изменение сетевых подключений выполняются штатными средствами Android.",
            12, muted, false
        ))
        content.addView(action("Обновить список") { renderDevices() })
        content.addView(action("Открыть настройки Bluetooth") {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        })
        content.addView(action("Открыть настройки Wi-Fi") {
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        })
    }

    private fun renderDevices() {
        if (!::content.isInitialized) return
        while (content.childCount > 5) content.removeViewAt(5)
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = manager.adapter
        if (adapter == null) {
            content.addView(text("Bluetooth не поддерживается этим устройством.", 14, muted, false))
            return
        }
        if (!adapter.isEnabled) {
            content.addView(text("Bluetooth выключен. Включи его в системных настройках.", 14, muted, false))
            return
        }
        val devices = try {
            adapter.bondedDevices.orEmpty().sortedBy { safeName(it).lowercase() }
        } catch (_: SecurityException) {
            content.addView(text("Android не предоставил доступ к сопряжённым устройствам.", 14, muted, false))
            return
        }
        content.addView(text("СОПРЯЖЁННЫЕ УСТРОЙСТВА · ${devices.size}", 13, accent, true))
        if (devices.isEmpty()) {
            content.addView(text("Пока нет сопряжённых устройств. Добавь часы, наушники или автомобиль через настройки Bluetooth.", 13, muted, false))
        } else {
            devices.forEach { device ->
                val name = safeName(device).ifBlank { "Bluetooth-устройство" }
                val category = classify(device)
                val kind = when (device.type) {
                    BluetoothDevice.DEVICE_TYPE_LE -> "Bluetooth LE"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Bluetooth Classic + LE"
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Bluetooth Classic"
                    else -> "Bluetooth"
                }
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(14), dp(12), dp(14), dp(12))
                    setBackgroundColor(panel)
                }
                card.addView(text(name, 15, foreground, true))
                card.addView(text("$category · $kind · Сопряжено", 11, muted, false))
                card.setOnClickListener { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
                val params = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) }
                content.addView(card, params)
            }
        }
        content.addView(text(
            "Часы и наушники управляются через совместимые Android-профили. Управление автомобилем возможно только через официальный API производителя или разрешённый интерфейс автомобиля; универсальные команды MANA не отправляет.",
            11, muted, false
        ))
    }

    private fun safeName(device: BluetoothDevice): String =
        try { device.name.orEmpty() } catch (_: SecurityException) { "" }

    private fun classify(device: BluetoothDevice): String {
        val clazz = try { device.bluetoothClass } catch (_: SecurityException) { null }
        return when (clazz?.deviceClass) {
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> "Наушники / гарнитура"
            BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> "Автомобильная аудиосистема"
            BluetoothClass.Device.WEARABLE_WRIST_WATCH,
            BluetoothClass.Device.WEARABLE_GLASSES -> "Носимое устройство"
            else -> if (clazz?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO) "Аудиоустройство" else "Другое устройство"
        }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(6), 0, dp(6))
    }

    private fun action(value: String, onClick: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        setTextColor(foreground)
        setBackgroundColor(panel)
        gravity = Gravity.CENTER
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(7) }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CONNECT) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) renderDevices()
            else content.addView(text("Доступ Bluetooth не предоставлен. MANA не может читать список сопряжённых устройств.", 12, muted, false))
        }
    }

    companion object { private const val REQUEST_CONNECT = 4115 }
}
