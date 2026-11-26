package com.mana.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

/**
 * Read-only firmware identity and official-update handoff.
 * Never downloads, patches, or writes OEM firmware partitions.
 */
class ManaSystemAdaptationActivity : Activity() {
    private val bg = Color.rgb(7, 20, 38)
    private val panel = Color.rgb(16, 43, 80)
    private val accent = Color.rgb(84, 200, 245)
    private val text = Color.rgb(234, 244, 255)
    private val muted = Color.rgb(145, 169, 197)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(24))
            setBackgroundColor(bg)
        }
        root.addView(label("АДАПТАЦИЯ MANA", 25, text, true))
        root.addView(label("Проверка системы и поиск совместимого обновления", 13, muted, false).apply {
            setPadding(0, dp(5), 0, dp(18))
        })

        val identity = listOf(
            "Производитель" to Build.MANUFACTURER,
            "Бренд" to Build.BRAND,
            "Модель" to Build.MODEL,
            "Устройство" to Build.DEVICE,
            "Продукт" to Build.PRODUCT,
            "Версия Android" to Build.VERSION.RELEASE,
            "Уровень API" to Build.VERSION.SDK_INT.toString(),
            "Номер сборки" to Build.DISPLAY,
            "Fingerprint" to Build.FINGERPRINT,
            "Ядро" to (System.getProperty("os.version") ?: "не определено"),
            "ABI" to Build.SUPPORTED_ABIS.joinToString(", ")
        )
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = GradientDrawable().apply {
                setColor(panel)
                cornerRadius = dp(18).toFloat()
            }
        }
        identity.forEach { (name, value) ->
            card.addView(label(name, 11, muted, false))
            card.addView(label(value.ifBlank { "не определено" }, 13, text, true).apply {
                setPadding(0, dp(2), 0, dp(9))
            })
        }
        root.addView(card)

        root.addView(label(
            "MANA сверяет идентификаторы устройства и направляет к официальному обновлению Realme. " +
                "После проверки совместимости она сможет настроить собственную оболочку и пользовательские модули. " +
                "Изменение защищённых разделов Realme из этого приложения не выполняется.",
            13, muted, false
        ).apply { setPadding(0, dp(16), 0, dp(14)) })

        root.addView(action("Проверить обновление Realme") { openSystemUpdater() })
        root.addView(action("Открыть поддержку и обновления Realme") {
            openUrl("https://www.realme.com/global/support/software-update")
        })
        root.addView(action("Проверить совместимость MANA") {
            val report = identity.joinToString("\n") { (key, value) -> "$key: $value" }
            android.content.ClipData.newPlainText("MANA device compatibility", report)
            val share = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "MANA — отчёт совместимости устройства")
                putExtra(Intent.EXTRA_TEXT, report)
            }
            startActivity(Intent.createChooser(share, "Отправить отчёт"))
        })
        root.addView(action("Назад") { finish() })
        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    private fun openSystemUpdater() {
        val intents = listOf(
            Intent("android.settings.SYSTEM_UPDATE_SETTINGS"),
            Intent(Settings.ACTION_SETTINGS)
        )
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
                // Try the general settings screen if the OEM update page is not exported.
            } catch (_: SecurityException) {
                // Same safe fallback; no privileged action is attempted.
            }
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            // The screen remains usable if no browser is installed.
        }
    }

    private fun action(title: String, run: () -> Unit) = TextView(this).apply {
        text = title
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(dp(14), dp(15), dp(14), dp(15))
        background = GradientDrawable().apply {
            setColor(Color.rgb(23, 101, 168))
            cornerRadius = dp(15).toFloat()
        }
        isClickable = true
        isFocusable = true
        setOnClickListener { run() }
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(9)
        layoutParams = params
    }

    private fun label(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
