package com.mana.updatebridge

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MANA Genesis launcher prototype.
 * This is a user-selected HOME app, not a firmware installer or privileged system UI.
 * Quick controls open Android settings where the OS restricts direct toggles.
 */
class UpdaterBridgeActivity : Activity() {
    private val navy = Color.rgb(7, 20, 38)
    private val panel = Color.rgb(16, 43, 80)
    private val blue = Color.rgb(23, 101, 168)
    private val cyan = Color.rgb(84, 200, 245)
    private val white = Color.rgb(234, 244, 255)
    private var startY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        window.decorView.systemUiVisibility = 0
        showHome()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun button(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(white)
            setBackgroundColor(blue)
            setOnClickListener { action() }
        }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(16))
            setBackgroundColor(navy)
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) startY = event.y
                if (event.action == MotionEvent.ACTION_UP && event.y - startY > dp(90)) {
                    showQuickPanel()
                    true
                } else false
            }
        }

        root.addView(text("MANA", 34f, cyan, true))
        root.addView(text("GENESIS 4.0  •  YOUR PHONE, YOUR WORLD", 10f, 0xFF91A9C5.toInt(), true))
        root.addView(text(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 14f, 0xFFB8CBE0.toInt()).apply {
            setPadding(0, dp(18), 0, dp(2))
        })
        root.addView(text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()), 42f, white, true))
        root.addView(text("Добро пожаловать домой", 16f, 0xFFB8CBE0.toInt()).apply {
            setPadding(0, 0, 0, dp(18))
        })

        val quick = button("⌁  Быстрые настройки") { showQuickPanel() }
        root.addView(quick, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)).apply {
            bottomMargin = dp(18)
        })

        root.addView(text("ПРИЛОЖЕНИЯ", 12f, cyan, true).apply {
            setPadding(0, 0, 0, dp(10))
        })
        val scroll = android.widget.ScrollView(this)
        val grid = GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = false
        }
        val apps = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }

        apps.take(40).forEach { info ->
            val tile = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(10), dp(3), dp(10))
                setOnClickListener {
                    val launch = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launch != null) startActivity(launch)
                }
            }
            val icon = android.widget.ImageView(this).apply {
                setImageDrawable(info.loadIcon(packageManager))
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
            }
            tile.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))
            tile.addView(text(info.loadLabel(packageManager).toString(), 10f, white).apply {
                gravity = Gravity.CENTER
                maxLines = 2
                setPadding(0, dp(5), 0, 0)
            })
            grid.addView(tile, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(92)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
        scroll.addView(grid)
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        bottom.addView(button("Настроить MANA") { showSetup() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        bottom.addView(button("Сделать главной") { requestHomeRole() }, LinearLayout.LayoutParams(0, dp(48), 1f).apply {
            leftMargin = dp(8)
        })
        root.addView(bottom)
        setContentView(root)
    }

    private fun showQuickPanel() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            setBackgroundColor(panel)
        }
        content.addView(text("MANA • БЫСТРЫЕ НАСТРОЙКИ", 18f, cyan, true))
        content.addView(text("Переключатели открывают системные настройки Android там, где прямое управление ограничено.", 12f, white).apply {
            setPadding(0, dp(8), 0, dp(12))
        })
        val rows = listOf(
            "Wi-Fi" to Settings.ACTION_WIFI_SETTINGS,
            "Bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
            "Мобильная сеть и точка доступа" to Settings.ACTION_WIRELESS_SETTINGS,
            "Звук" to Settings.ACTION_SOUND_SETTINGS,
            "Экран и яркость" to Settings.ACTION_DISPLAY_SETTINGS,
            "Режим полёта" to Settings.ACTION_AIRPLANE_MODE_SETTINGS
        )
        rows.forEach { (label, action) ->
            content.addView(button(label) { openSettings(action) }, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46)
            ).apply { bottomMargin = dp(6) })
        }
        content.addView(text("Яркость экрана", 14f, white, true).apply { setPadding(0, dp(10), 0, dp(4)) })
        val brightness = SeekBar(this).apply { max = 255 }
        val canWrite = Settings.System.canWrite(this)
        if (canWrite) {
            brightness.progress = runCatching { Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS) }.getOrDefault(128)
            brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) runCatching { Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value.coerceAtLeast(1)) }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        } else {
            brightness.isEnabled = false
        }
        content.addView(brightness)
        if (!canWrite) content.addView(button("Разрешить управление яркостью") {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName"))
            startActivity(intent)
        })
        AlertDialog.Builder(this).setView(content).setNegativeButton("Закрыть", null).show()
    }

    private fun showSetup() {
        val message = buildString {
            append("Настройка MANA\n\n")
            append("• Уведомления: ")
            append(if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) "разрешены" else "не разрешены")
            append("\n• Роль главного экрана: ")
            append(if (isDefaultHome()) "MANA выбрана" else "можно выбрать MANA")
            append("\n• Управление яркостью: ")
            append(if (Settings.System.canWrite(this@UpdaterBridgeActivity)) "разрешено" else "не предоставлено")
            append("\n\nСистемные разрешения Android показывает сам. Можно отказать и продолжить пользоваться телефоном.")
        }
        val builder = AlertDialog.Builder(this)
            .setTitle("Первоначальная настройка MANA")
            .setMessage(message)
            .setNegativeButton("Закрыть", null)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            builder.setPositiveButton("Разрешить уведомления") { _, _ ->
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4101)
            }
        } else {
            builder.setPositiveButton("Выбрать MANA главной") { _, _ -> requestHomeRole() }
        }
        builder.show()
    }

    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME) && !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME), 4102)
            } else {
                Toast.makeText(this, "MANA уже выбрана главным экраном", Toast.LENGTH_SHORT).show()
            }
        } else {
            startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
        }
    }

    private fun isDefaultHome(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    private fun openSettings(action: String) {
        runCatching { startActivity(Intent(action)) }
            .onFailure { Toast.makeText(this, "Этот раздел настроек недоступен на устройстве", Toast.LENGTH_SHORT).show() }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4101) showSetup()
    }
}
