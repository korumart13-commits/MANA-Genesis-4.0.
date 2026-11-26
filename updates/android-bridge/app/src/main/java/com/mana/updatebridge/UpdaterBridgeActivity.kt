package com.mana.updatebridge

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Opens the OEM updater UI only. It cannot install a firmware payload or grant
 * system privileges; a genuine OEM-signed OTA must still be offered by Realme.
 */
class UpdaterBridgeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHandoffScreen()
        window.decorView.postDelayed({ openRealmeUpdater() }, 350)
    }

    private fun showHandoffScreen() {
        val navy = 0xFF071426.toInt()
        val blue = 0xFF1765A8.toInt()
        val white = 0xFFEAF4FF.toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(36, 36, 36, 36)
            setBackgroundColor(navy)
        }
        root.addView(TextView(this).apply {
            text = "Realme MANA +1"
            textSize = 27f
            setTextColor(white)
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Открываем штатный раздел обновления Realme. Он покажет только обновления, которые Realme предлагает и разрешает установить для этого устройства."
            textSize = 16f
            setTextColor(0xFFB8CBE0.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 22, 0, 26)
        })
        root.addView(Button(this).apply {
            text = "Открыть обновление Realme"
            setBackgroundColor(blue)
            setTextColor(white)
            setOnClickListener { openRealmeUpdater() }
        })
        root.addView(TextView(this).apply {
            text = "Важно: это переход к штатному обновлятору, а не прошивка. MANA +1 пока не содержит OEM-подписанный системный образ; этот экран не утверждает, что MANA уже установлена."
            textSize = 12f
            setTextColor(0xFFFFCC80.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 28, 0, 0)
        })
        setContentView(root)
    }

    private fun openRealmeUpdater() {
        val nativeUpdate = Intent("android.settings.SYSTEM_UPDATE_SETTINGS")
        try {
            startActivity(nativeUpdate)
        } catch (_: ActivityNotFoundException) {
            openGeneralSettings()
        } catch (_: SecurityException) {
            openGeneralSettings()
        }
    }

    private fun openGeneralSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (error: Exception) {
            AlertDialog.Builder(this)
                .setTitle("Не удалось открыть настройки")
                .setMessage("Открой вручную: Настройки → О системе → Обновление ПО. " + (error.message ?: ""))
                .setPositiveButton("Понятно", null)
                .show()
        }
    }
}
