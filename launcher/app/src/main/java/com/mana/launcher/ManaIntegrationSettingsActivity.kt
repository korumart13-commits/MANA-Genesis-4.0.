package com.mana.launcher

import androidx.activity.ComponentActivity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Owner-facing setup for official Yandex AI Studio and read-only VK connectors. */
class ManaIntegrationSettingsActivity : ComponentActivity() {
    private val backgroundArgb = Color.rgb(4, 7, 14)
    private val surfaceArgb = Color.rgb(13, 32, 59)
    private val accentArgb = Color.rgb(90, 190, 255)
    private val foregroundArgb = Color.rgb(238, 246, 255)
    private val mutedArgb = Color.rgb(150, 174, 198)
    private lateinit var yandex: ManaYandexAiStudioProvider
    private lateinit var vk: ManaVkReadOnlyConnector
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        yandex = ManaYandexAiStudioProvider(this)
        vk = ManaVkReadOnlyConnector(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(this@ManaIntegrationSettingsActivity.backgroundArgb)
        }
        root.addView(text("Интеграции MANA", 25, foregroundArgb, true))
        root.addView(text("Ключи хранятся локально в AES-GCM через Android Keystore. Облачные запросы не запускаются автоматически.", 13, mutedArgb))
        status = text("Yandex: ${if (yandex.isAvailable()) "подключён" else "не настроен"} • VK: ${if (vk.isConfigured()) "подключён" else "не настроен"}", 12, accentArgb)
        root.addView(status)

        root.addView(section("Yandex AI Studio"))
        root.addView(text("Официальный API Yandex Cloud. Запросы могут тарифицироваться по условиям твоего аккаунта.", 12, mutedArgb))
        val yandexKey = field("Секретный API-ключ Yandex", password = true)
        val folder = field("ID каталога Yandex Cloud")
        val model = field("Model ID (пример: yandexgpt/latest)", initial = "yandexgpt/latest")
        root.addView(yandexKey); root.addView(folder); root.addView(model)
        root.addView(button("Сохранить и включить Yandex") {
            try {
                yandex.configure(yandexKey.text.toString(), folder.text.toString().trim(), model.text.toString().trim(), true)
                yandexKey.setText("")
                refreshStatus("Yandex AI Studio настроен. Для использования в MANA Brain потребуется отдельное подтверждение запроса.")
            } catch (error: Exception) { refreshStatus("Ошибка настройки Yandex: ${error.message}") }
        })
        root.addView(button("Проверить Yandex (один оплачиваемый запрос)") {
            if (!yandex.isAvailable()) { refreshStatus("Сначала настрой Yandex AI Studio."); return@button }
            AlertDialog.Builder(this)
                .setTitle("Отправить тестовый запрос?")
                .setMessage("Yandex получит текст «Ответь одним словом: готово». Запрос может тарифицироваться.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Отправить") { _, _ ->
                    this@ManaIntegrationSettingsActivity.lifecycleScope.launch {
                        try { refreshStatus("Yandex ответил: " + yandex.generate("Ответь одним словом: готово.").take(240)) }
                        catch (error: Exception) { refreshStatus("Ошибка Yandex: ${error.message}") }
                    }
                }.show()
        })
        root.addView(button("Выключить Yandex") { yandex.disable(); refreshStatus("Yandex выключен.") })
        root.addView(button("Удалить ключ Yandex") {
            confirmRemove("Удалить сохранённые учётные данные Yandex?") {
                yandex.removeCredentials(); refreshStatus("Учётные данные Yandex удалены.")
            }
        })
        root.addView(button("Официальная документация Yandex") {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://yandex.cloud/en/docs/ai-studio/api-ref/authentication")))
        })

        root.addView(section("VK — только чтение"))
        root.addView(text("Поддерживаются только users.get и wall.get. MANA не публикует записи, не ставит реакции и не отправляет сообщения.", 12, mutedArgb))
        val vkToken = field("VK access token", password = true)
        root.addView(vkToken)
        root.addView(button("Подключить VK и прочитать профиль") {
            try {
                vk.configureAccessToken(vkToken.text.toString())
                vkToken.setText("")
                this@ManaIntegrationSettingsActivity.lifecycleScope.launch {
                    try {
                        val profile = vk.readProfile()
                        refreshStatus("VK подключён: ${profile.firstName} ${profile.lastName} (ID ${profile.id}).")
                    } catch (error: Exception) { refreshStatus("Токен сохранён, но VK API вернул ошибку: ${error.message}") }
                }
            } catch (error: Exception) { refreshStatus("Ошибка подключения VK: ${error.message}") }
        })
        root.addView(button("Прочитать последние записи своей стены") {
            if (!vk.isConfigured()) { refreshStatus("Сначала подключи VK."); return@button }
            this@ManaIntegrationSettingsActivity.lifecycleScope.launch {
                try {
                    val posts = vk.readOwnWall(5)
                    refreshStatus(if (posts.isEmpty()) "VK: записей нет." else posts.joinToString("\n") { it.text.take(100) })
                } catch (error: Exception) { refreshStatus("Ошибка чтения VK: ${error.message}") }
            }
        })
        root.addView(button("Отключить и удалить токен VK") {
            confirmRemove("Удалить сохранённый токен VK?") {
                vk.removeCredentials(); refreshStatus("Токен VK удалён.")
            }
        })
        root.addView(button("Закрыть") { finish() })

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll)
    }

    private fun field(hintText: String, password: Boolean = false, initial: String = "") =
        EditText(this).apply {
            hint = hintText
            setText(initial)
            setTextColor(this@ManaIntegrationSettingsActivity.foregroundArgb)
            setHintTextColor(this@ManaIntegrationSettingsActivity.mutedArgb)
            inputType = if (password) InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                else InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundColor(this@ManaIntegrationSettingsActivity.surfaceArgb)
            layoutParams = LinearLayout.LayoutParams(-1, dp(48)).apply { topMargin = dp(7) }
        }

    private fun section(value: String) = text(value, 18, this@ManaIntegrationSettingsActivity.accentArgb, true).apply {
        setPadding(0, dp(22), 0, dp(5))
    }

    private fun text(value: String, size: Int, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        setTextColor(this@ManaIntegrationSettingsActivity.foregroundArgb)
        setBackgroundColor(this@ManaIntegrationSettingsActivity.surfaceArgb)
        gravity = Gravity.CENTER
        setOnClickListener { action() }
        layoutParams = LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(7) }
    }

    private fun confirmRemove(message: String, action: () -> Unit) {
        AlertDialog.Builder(this).setTitle("Подтверждение")
            .setMessage(message).setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ -> action() }.show()
    }

    private fun refreshStatus(message: String) {
        runOnUiThread {
            status.text = message
            status.invalidate()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
