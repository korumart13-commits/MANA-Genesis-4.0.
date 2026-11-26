package com.mana.updatebridge

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Transparent first-run permission flow for MANA.
 *
 * Requests only the notification runtime permission because this bridge
 * currently posts setup/status notifications. It does not request unrelated
 * sensitive access, change firmware, unlock the bootloader, or reboot silently.
 */
class UpdaterBridgeActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var action: Button

    private val navy = 0xFF071426.toInt()
    private val blue = 0xFF1765A8.toInt()
    private val white = 0xFFEAF4FF.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScreen()
        refreshPermissionState()
    }

    private fun showScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 36, 36, 36)
            setBackgroundColor(navy)
        }
        root.addView(TextView(this).apply {
            text = "MANA Genesis 4.0"
            textSize = 28f
            setTextColor(white)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Первоначальная настройка"
            textSize = 18f
            setTextColor(0xFF54C8F5.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 8)
        })
        status = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFB8CBE0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 22)
        }
        root.addView(status)
        action = Button(this).apply {
            setBackgroundColor(blue)
            setTextColor(white)
            setOnClickListener { requestNextPermission() }
        }
        root.addView(action)
        setContentView(root)
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun refreshPermissionState() {
        if (hasNotificationPermission()) {
            status.text = "Уведомления MANA: разрешены. Другие специальные доступы этому этапу не требуются."
            action.text = "Продолжить"
            action.setOnClickListener { showRestartInformation() }
        } else {
            status.text = "Шаг 1 из 1. Разрешение на уведомления нужно для сообщений о ходе настройки. Android покажет системное окно подтверждения."
            action.text = "Запросить разрешение"
            action.setOnClickListener { requestNextPermission() }
        }
    }

    private fun requestNextPermission() {
        if (Build.VERSION.SDK_INT >= 33 && !hasNotificationPermission()) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        } else {
            showRestartInformation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) {
            refreshPermissionState()
            if (!hasNotificationPermission()) {
                AlertDialog.Builder(this)
                    .setTitle("Разрешение не предоставлено")
                    .setMessage("Можно продолжить без уведомлений. Разрешение можно включить позже в настройках Android.")
                    .setPositiveButton("Понятно", null)
                    .show()
            }
        }
    }

    private fun showRestartInformation() {
        AlertDialog.Builder(this)
            .setTitle("Первый этап завершён")
            .setMessage(
                "Запрошенные на этом этапе разрешения обработаны. Это приложение пока не изменяет системную прошивку и не устанавливает драйверы. " +
                    "Если хочешь перезапустить телефон, сохрани работу и выбери «Перезагрузка» в системном меню питания."
            )
            .setNegativeButton("Позже", null)
            .setPositiveButton("Понятно", null)
            .show()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 4101
    }
}
