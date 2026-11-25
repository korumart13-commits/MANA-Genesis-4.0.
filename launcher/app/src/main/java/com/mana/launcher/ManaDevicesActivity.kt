package com.mana.launcher

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Shows already-paired Bluetooth devices; pairing and connection remain in Android Settings. */
class ManaDevicesActivity : Activity() {
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(24))
            setBackgroundColor(0xFF071426.toInt())
        }
        root.addView(text("MANA DEVICES", 24, 0xFFFFFFFF.toInt(), true))
        root.addView(text("Часы, наушники и автомобильные Bluetooth-устройства. MANA показывает только уже сопряжённые устройства; поиск и подключение выполняются в системных настройках.", 12, 0xFF9CB6D4.toInt()))
        root.addView(button("Обновить список") { refresh() })
        root.addView(button("Открыть Bluetooth настройки") { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) })
        root.addView(button("Открыть Wi-Fi настройки") { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) })
        setContentView(ScrollView(this).apply { addView(root) })
        refresh()
    }

    private fun refresh() {
        while (root.childCount > 5) root.removeViewAt(5)
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            root.addView(text("Для чтения списка сопряжённых устройств требуется разрешение Nearby devices.", 13, 0xFFFFC978.toInt()))
            root.addView(button("Разрешить доступ к Bluetooth") {
                requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_BLUETOOTH)
            })
            return
        }
        try {
            val manager = getSystemService(BluetoothManager::class.java)
            val adapter = manager?.adapter
            if (adapter == null) {
                root.addView(text("Bluetooth на устройстве недоступен.", 13, 0xFF9CB6D4.toInt()))
                return
            }
            val devices = adapter.bondedDevices.orEmpty().sortedBy { it.name.orEmpty().lowercase() }
            if (devices.isEmpty()) {
                root.addView(text("Сопряжённых устройств пока нет. Добавь часы, наушники или автомобиль через системные настройки Bluetooth.", 13, 0xFF9CB6D4.toInt()))
                return
            }
            root.addView(text("Сопряжённые устройства: ${devices.size}", 16, 0xFF69C6FF.toInt(), true))
            devices.forEach { device ->
                val kind = when (device.bluetoothClass?.majorDeviceClass) {
                    BluetoothClass.Device.Major.AUDIO_VIDEO -> "Аудио / наушники / автомобиль"
                    BluetoothClass.Device.Major.PERIPHERAL -> "Периферия / часы"
                    else -> "Bluetooth-устройство"
                }
                root.addView(text("${device.name ?: "Без имени"}\n$kind", 14, 0xFFFFFFFF.toInt(), true))
            }
        } catch (_: SecurityException) {
            root.addView(text("Android не разрешил читать список устройств. Проверь Nearby devices.", 13, 0xFFFFA6A6.toInt()))
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH) refresh()
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, dp(8))
        }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        gravity = Gravity.CENTER
        setOnClickListener { action() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object { private const val REQUEST_BLUETOOTH = 7201 }
}
