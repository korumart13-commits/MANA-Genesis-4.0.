package com.mana.launcher

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * MANA's immersive in-app lock-screen experience.
 * Android's real device credential screen remains the only authentication authority.
 */
class ManaLockScreenActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var battery: TextView

    private val clockTick = object : Runnable {
        override fun run() {
            if (!isFinishing) {
                val now = Date()
                clock.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
                date.text = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now)
                battery.text = batteryLabel()
                handler.postDelayed(this, 30_000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        if (Build.VERSION.SDK_INT >= 29) {
            window.navigationBarColor = Color.rgb(4, 13, 31)
            window.statusBarColor = Color.rgb(4, 13, 31)
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
        }
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(clockTick)
        clockTick.run()
    }

    override fun onPause() {
        handler.removeCallbacks(clockTick)
        super.onPause()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(26), dp(34), dp(26), dp(28))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(4, 13, 31), Color.rgb(7, 32, 72), Color.rgb(9, 21, 48))
            )
        }

        val top = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        top.addView(text("MANA  •  GENESIS 4.0", 12, CYAN, true), LinearLayout.LayoutParams(0, -2, 1f))
        battery = text("", 12, SOFT, true)
        top.addView(battery)
        root.addView(top, LinearLayout.LayoutParams(-1, dp(36)))

        val spacerTop = View(this)
        root.addView(spacerTop, LinearLayout.LayoutParams(1, 0, 0.8f))

        val avatarHalo = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.argb(24, 84, 200, 245))
                setStroke(dp(1), Color.argb(150, 84, 200, 245))
            }
        }
        val avatar = TextView(this).apply {
            text = "M"
            gravity = Gravity.CENTER
            textSize = 66f
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(19, 88, 148), Color.rgb(7, 28, 62))).apply {
                shape = GradientDrawable.OVAL
                setStroke(dp(2), CYAN)
            }
        }
        avatarHalo.addView(avatar, LinearLayout.LayoutParams(dp(142), dp(142)))
        root.addView(avatarHalo, LinearLayout.LayoutParams(dp(174), dp(174)))

        root.addView(text("MANA", 16, CYAN, true).apply {
            letterSpacing = 0.22f
            setPadding(0, dp(18), 0, dp(4))
        })
        clock = text("--:--", 76, Color.WHITE, false).apply {
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = -0.04f
        }
        root.addView(clock)
        date = text("", 15, SOFT, false).apply {
            setPadding(0, dp(4), 0, dp(22))
        }
        root.addView(date)
        root.addView(text("КОНСТАНТА ОМЕГА  Ω", 12, CYAN, true).apply {
            setPadding(0, dp(8), 0, dp(8))
        })
        root.addView(text("«Мечтай. Исследуй. Создавай.»", 14, Color.WHITE, false))

        val spacerBottom = View(this)
        root.addView(spacerBottom, LinearLayout.LayoutParams(1, 0, 1f))

        val unlock = TextView(this).apply {
            text = "⌑   РАЗБЛОКИРОВАТЬ MANA"
            gravity = Gravity.CENTER
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                intArrayOf(Color.rgb(18, 92, 155), Color.rgb(27, 119, 181))).apply {
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), Color.rgb(84, 200, 245))
            }
            setOnClickListener { requestAndroidCredential() }
        }
        root.addView(unlock, LinearLayout.LayoutParams(-1, dp(58)))

        root.addView(text("Защита устройства остаётся под контролем Android", 10, SOFT, false).apply {
            setPadding(0, dp(14), 0, 0)
        })
        setContentView(root)
    }

    private fun requestAndroidCredential() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isDeviceSecure) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Настрой защита телефона")
                .setMessage("Для безопасного доступа установи PIN, пароль или графический ключ в настройках Android. MANA не запрашивает и не хранит твой PIN.")
                .setNegativeButton("Позже", null)
                .setPositiveButton("Открыть настройки") { _, _ ->
                    startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))
                }
                .show()
            return
        }
        val confirmation = keyguard.createConfirmDeviceCredentialIntent(
            "Разблокировка MANA",
            "Подтверди личность системным способом Android"
        )
        if (confirmation != null) {
            @Suppress("DEPRECATION")
            startActivityForResult(confirmation, REQUEST_CREDENTIAL)
        } else {
            android.app.AlertDialog.Builder(this)
                .setMessage("Системное подтверждение временно недоступно. Используй штатную блокировку телефона.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    @Deprecated("Android credential result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CREDENTIAL && resultCode == RESULT_OK) finish()
    }

    private fun batteryLabel(): String {
        val manager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (level in 0..100) "▰  $level%" else "MANA"
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val REQUEST_CREDENTIAL = 7401
        private val CYAN = Color.rgb(84, 200, 245)
        private val SOFT = Color.rgb(145, 169, 197)
    }
}
