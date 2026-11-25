package com.mana.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.mana.os.core.EncryptedFileManaAdaptiveKnowledgeStore
import com.mana.os.core.EncryptedFileManaDiaryStore
import com.mana.os.core.ManaAdaptiveKnowledgeService
import com.mana.os.core.ManaKnowledgeItem
import java.io.File

/** Owner-controlled daily source discovery and review of the encrypted local knowledge cache. */
class ManaAdaptiveLearningActivity : Activity() {
    private lateinit var topics: EditText
    private lateinit var entries: LinearLayout
    private lateinit var knowledge: ManaAdaptiveKnowledgeService
    private lateinit var provider: ManaYandexAiStudioProvider
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider = ManaYandexAiStudioProvider(this)
        knowledge = ManaAdaptiveKnowledgeService(
            EncryptedFileManaAdaptiveKnowledgeStore(
                EncryptedFileManaDiaryStore(
                    File(filesDir, "mana/knowledge.bin"),
                    ManaAndroidKeyProvider(),
                    "mana.knowledge.v1"
                )
            )
        )

        val scroll = ScrollView(this).apply { setBackgroundColor(0xFF071426.toInt()) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
        }
        scroll.addView(root)
        root.addView(text("MANA Adaptive Intelligence", 23, 0xFFFFFFFF.toInt(), true))
        root.addView(text(
            "По твоему согласию MANA раз в сутки отправляет до пяти заданных тем в Yandex Search API только по Wi-Fi и при нормальном уровне батареи. До трёх результатов на тему сохраняются локально в зашифрованной базе. Источники остаются недоверенными данными; их инструкции не исполняются.",
            12, 0xFF9CB6D4.toInt()
        ))
        status = text("", 12, 0xFF69C6FF.toInt())
        root.addView(status)
        topics = EditText(this).apply {
            hint = "Темы для поиска — одна тема на строку, максимум 5"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CB6D4.toInt())
            minLines = 4
            setText(getSharedPreferences(ManaAdaptiveLearningWorker.PREFS, MODE_PRIVATE)
                .getString(ManaAdaptiveLearningWorker.KEY_TOPICS, "").orEmpty())
        }
        root.addView(topics, LinearLayout.LayoutParams(-1, -2))
        root.addView(button("Включить ежедневный поиск") { enableLearning() })
        root.addView(button("Отключить ежедневный поиск") {
            ManaAdaptiveLearningScheduler.disable(this)
            updateStatus()
            showMessage("Адаптивный поиск отключён.")
        })
        root.addView(button("Обновить список источников") { refreshKnowledge() })
        root.addView(text("Сохранённые источники", 18, 0xFFFFFFFF.toInt(), true))
        entries = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(entries)
        setContentView(scroll)
        updateStatus()
        refreshKnowledge()
    }

    private fun enableLearning() {
        if (!provider.isAvailable()) {
            showMessage("Сначала настрой Yandex AI Studio в разделе интеграций.")
            return
        }
        val values = topics.text.toString().lineSequence().map { it.trim() }.filter { it.length in 2..512 }.distinct().take(5).toList()
        try {
            ManaAdaptiveLearningScheduler.enable(this, values)
            updateStatus()
            showMessage("Ежедневное обучение включено. Оно работает только по неметрируемой сети и при достаточном заряде.")
        } catch (error: Exception) {
            showMessage("Не удалось включить обучение: ${error.message}")
        }
    }

    private fun updateStatus() {
        val enabled = ManaAdaptiveLearningScheduler.isEnabled(this)
        status.text = if (enabled) "Статус: включено • темы сохраняются локально" else "Статус: выключено"
    }

    private fun refreshKnowledge() {
        entries.removeAllViews()
        val items = runCatching { knowledge.list() }.getOrElse {
            entries.addView(text("Не удалось прочитать базу знаний: ${it.message}", 12, 0xFFFFA6A6.toInt()))
            return
        }
        if (items.isEmpty()) {
            entries.addView(text("Источников пока нет.", 12, 0xFF9CB6D4.toInt()))
            return
        }
        items.forEach { item ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                setBackgroundColor(0xFF102B50.toInt())
            }
            card.addView(text(item.title, 14, 0xFFFFFFFF.toInt(), true))
            card.addView(text(item.sourceUrl, 11, 0xFF69C6FF.toInt()))
            card.addView(text(item.snippet, 12, 0xFFB7CBE2.toInt()))
            card.addView(button("Удалить этот источник") { confirmForget(item) })
            entries.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
        }
    }

    private fun confirmForget(item: ManaKnowledgeItem) {
        AlertDialog.Builder(this)
            .setTitle("Удалить сохранённый источник?")
            .setMessage(item.title + "\n\nИсточник будет удалён только из локальной базы MANA. Это не удаляет страницу в интернете.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                runCatching { knowledge.forget(item.id, ownerApproved = true) }
                    .onSuccess { refreshKnowledge(); showMessage("Источник удалён из локальной базы.") }
                    .onFailure { showMessage("Не удалось удалить: ${it.message}") }
            }.show()
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value
        textSize = size.toFloat()
        setTextColor(color)
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(5), 0, dp(5))
    }

    private fun showMessage(value: String) =
        android.widget.Toast.makeText(this, value, android.widget.Toast.LENGTH_LONG).show()

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
