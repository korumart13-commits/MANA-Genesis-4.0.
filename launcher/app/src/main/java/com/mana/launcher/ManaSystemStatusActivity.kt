package com.mana.launcher

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.mana.os.core.ManaEngineHealth
import java.io.File

/**
 * Read-only diagnostics screen. It reports observed app-process state and never
 * changes keys, encrypted stores, permissions, model files, or system settings.
 */
class ManaSystemStatusActivity : Activity() {
    private val backgroundColor = Color.rgb(4, 7, 14)
    private val panelColor = Color.rgb(10, 22, 42)
    private val primaryText = Color.rgb(238, 246, 255)
    private val secondaryText = Color.rgb(139, 163, 190)
    private val accent = Color.rgb(105, 198, 255)
    private lateinit var rows: LinearLayout
    private var report: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = backgroundColor
        window.navigationBarColor = backgroundColor
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(18))
            setBackgroundColor(backgroundColor)
        }
        root.addView(text("MANA • СОСТОЯНИЕ СИСТЕМЫ", 21, primaryText, true))
        root.addView(text("Диагностика только для чтения. Ничего не ремонтирует и не изменяет автоматически.", 12, secondaryText, false).apply {
            setPadding(0, dp(7), 0, dp(14))
        })
        val scroll = ScrollView(this)
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(rows)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply { gravity = Gravity.CENTER }
        controls.addView(button("Обновить") { refreshReport() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        controls.addView(button("Копировать отчёт") { copyReport() }, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(controls)
        root.addView(button("Назад") { finish() }, LinearLayout.LayoutParams(-1, dp(44)))
        setContentView(root)
        refreshReport()
    }

    private fun refreshReport() {
        rows.removeAllViews()
        val runtime = ManaEngineProcessOwner.engineRuntime()
        val health: ManaEngineHealth? = runCatching { runtime?.health() }.getOrNull()
        val modelReady = runCatching { ManaEngineProcessOwner.localModelRuntime()?.isReady() == true }.getOrDefault(false)
        val memoryFile = File(filesDir, "mana/memory.bin")
        val worldFile = File(filesDir, "mana/microworld.bin")
        val recoveryDir = File(filesDir, "mana/recovery")
        val recoveryFiles = recoveryDir.listFiles()?.filter { it.isFile }.orEmpty()
        val keystoreProvider = runCatching { ManaAndroidKeyProvider(); true }.getOrDefault(false)

        val entries = mutableListOf<Pair<String, String>>()
        entries += "MANA Engine" to when {
            health == null -> "Не запущен или состояние недоступно"
            health.state.name == "READY" -> "Работает"
            else -> "Ограниченный режим: ${health.state}"
        }
        entries += "Супервизор ОС" to (health?.systemPhase?.name ?: "Нет данных")
        entries += "Последняя ошибка" to (health?.lastFailure ?: "Нет зарегистрированной ошибки")
        entries += "Локальная модель" to if (modelReady) "Загружена и готова" else "Не загружена / недоступна"
        entries += "Зашифрованная память" to when {
            health == null -> "Не проверена: Engine недоступен"
            else -> "Хранилище открыто; элементов памяти: ${health.activeMemoryItems}"
        }
        entries += "Файл памяти" to fileState(memoryFile)
        entries += "Microworld" to when {
            health == null -> "Не проверен: Engine недоступен"
            else -> "Доступен; ревизия ${health.microworldRevision}"
        }
        entries += "Файл Microworld" to fileState(worldFile)
        entries += "Android Keystore" to if (keystoreProvider) "Провайдер доступен; ключ проверяется при открытии хранилища" else "Провайдер недоступен"
        entries += "Копии восстановления" to if (recoveryFiles.isEmpty()) "Не обнаружены" else "${recoveryFiles.size} файл(ов): ${recoveryFiles.joinToString { it.name }}"
        entries += "Устройство" to "${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.SDK_INT}"
        val backlog = runCatching { ManaGenesisFirstBootService(this).readCheckpoint() }.getOrNull()
        entries += "План саморазвития" to when {
            backlog == null -> "Контрольная точка ещё не создана или недоступна"
            else -> {
                val ordered = backlog.optJSONArray("orderedWork")?.length() ?: 0
                val blocked = backlog.optJSONArray("blockedWork")?.length() ?: 0
                "Ожидают выполнения: $ordered • заблокированы до получения исходников/доказательств: $blocked"
            }
        }
        entries += "Приложение" to "MANA Genesis • диагностика приложения, не статус прошивки"

        report = entries.joinToString("\n") { "${it.first}: ${it.second}" }
        entries.forEach { (name, value) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(panelColor, 16)
            }
            card.addView(text(name, 12, accent, true))
            card.addView(text(value, 14, primaryText, false).apply { setPadding(0, dp(4), 0, 0) })
            rows.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        }
    }

    private fun fileState(file: File): String = when {
        !file.exists() -> "Пока не создан"
        !file.isFile -> "Необычный тип объекта"
        else -> "Есть • ${file.length()} байт"
    }

    private fun copyReport() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MANA system status", report))
        android.widget.Toast.makeText(this, "Отчёт скопирован", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun button(value: String, run: () -> Unit) = TextView(this).apply {
        text = value
        gravity = Gravity.CENTER
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(accent)
        setOnClickListener { run() }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.rgb(22, 57, 94))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
