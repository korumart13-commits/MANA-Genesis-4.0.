package com.mana.launcher

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

/**
 * Owner-facing module registry.
 *
 * This first safe extension layer manages built-in capabilities and declarative
 * extension packs. It deliberately does not load arbitrary downloaded DEX/JAR
 * code or grant Android permissions by itself.
 */
class ManaExtensionCenterActivity : Activity() {
    private val background = Color.rgb(7, 20, 38)
    private val surface = Color.rgb(16, 43, 80)
    private val accent = Color.rgb(84, 200, 245)
    private val foreground = Color.rgb(234, 244, 255)
    private val muted = Color.rgb(145, 169, 197)
    private val prefs by lazy { getSharedPreferences("mana.extensions.v1", MODE_PRIVATE) }

    private data class Module(
        val id: String,
        val title: String,
        val description: String,
        val state: State
    )
    private enum class State { BUILT_IN, PLANNED }

    private val modules = listOf(
        Module("brain", "MANA Brain", "Единая маршрутизация запросов между локальным и разрешённым облачным ИИ.", State.BUILT_IN),
        Module("local_model", "Локальный движок", "LiteRT-LM: локальная модель, если она установлена и совместима.", State.BUILT_IN),
        Module("memory", "Локальная память", "Зашифрованные заметки и выборочный контекст.", State.BUILT_IN),
        Module("diary", "Дневник", "Локальные записи владельца.", State.BUILT_IN),
        Module("life_manager", "Планировщик жизни", "Задачи и напоминания.", State.BUILT_IN),
        Module("receptionist", "Smart Receptionist", "Сценарии обработки звонков; системные роли и разрешения задаются отдельно.", State.BUILT_IN),
        Module("avatar", "3D-аватар и голос", "Экранный аватар, голосовой ввод и озвучивание.", State.BUILT_IN),
        Module("adaptive", "Adaptive Intelligence", "Сбор и проверка знаний с сохранением локальных заметок.", State.BUILT_IN),
        Module("future_updates", "Future Updates", "Идеи и этапы будущих обновлений.", State.BUILT_IN),
        Module("autodev", "AutoDev", "Подготовка изменений и тестирование; установка системных изменений требует подтверждения.", State.BUILT_IN),
        Module("cloud", "Облачные модели", "Подключаемые OpenAI, Claude, Gemini и Grok с отдельным согласием на передачу запроса.", State.BUILT_IN),
        Module("integrations", "Yandex и VK", "Подключения через настроенные официальные интеграции.", State.BUILT_IN),
        Module("devices", "Часы, наушники и автомобиль", "Bluetooth-интеграции в пределах разрешений Android.", State.BUILT_IN),
        Module("device_tools", "Управление телефоном", "Только поддерживаемые Android API и выданные владельцем разрешения.", State.BUILT_IN),
        Module("wallpaper", "Автоматические обои", "Автовыбор и смена обоев — модуль ещё требует реализации.", State.PLANNED),
        Module("game_intelligence", "Game Intelligence", "Игровой помощник и обучение игровым сценариям — модуль ещё требует реализации.", State.PLANNED),
        Module("minecraft", "Minecraft Bridge", "Интеграция через официальный Script API — модуль ещё требует реализации.", State.PLANNED),
        Module("micro_universe", "MANA Micro-Universe", "Единое пространство мастерской, библиотеки знаний и игровых зон — модуль ещё требует реализации.", State.PLANNED),
        Module("sleep", "MANA Sleep", "Сценарии сна и пробуждения — модуль ещё требует реализации.", State.PLANNED)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
    }

    private fun render() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setBackgroundColor(background)
        }
        root.addView(TextView(this).apply {
            text = "MANA · ЦЕНТР МОДУЛЕЙ"
            textSize = 22f
            setTextColor(foreground)
            setPadding(0, 0, 0, dp(8))
        })
        root.addView(TextView(this).apply {
            text = "Здесь можно включать встроенные функции. Будущие расширения будут проходить проверку подписи, совместимости и разрешений; этот центр не запускает произвольный код из интернета."
            textSize = 13f
            setTextColor(muted)
            setPadding(0, 0, 0, dp(16))
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        modules.forEach { module ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), dp(12), dp(14), dp(12))
                setBackgroundColor(surface)
            }
            card.addView(TextView(this).apply {
                text = module.title
                textSize = 16f
                setTextColor(foreground)
            })
            card.addView(TextView(this).apply {
                text = module.description
                textSize = 12f
                setTextColor(muted)
                setPadding(0, dp(4), 0, dp(8))
            })
            val row = LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                orientation = LinearLayout.HORIZONTAL
            }
            val enabled = prefs.getBoolean(module.id, module.state == State.BUILT_IN)
            row.addView(TextView(this).apply {
                text = when (module.state) {
                    State.BUILT_IN -> if (enabled) "Включён" else "Выключен"
                    State.PLANNED -> "Ожидает реализации"
                }
                textSize = 12f
                setTextColor(if (module.state == State.BUILT_IN && enabled) accent else muted)
            }, LinearLayout.LayoutParams(0, dp(40), 1f))
            val action = Button(this).apply {
                text = if (module.state == State.PLANNED) "Подробнее" else if (enabled) "Выключить" else "Включить"
                isEnabled = module.state == State.BUILT_IN
                setOnClickListener {
                    prefs.edit().putBoolean(module.id, !prefs.getBoolean(module.id, true)).apply()
                    render()
                }
            }
            row.addView(action)
            if (module.state == State.PLANNED) {
                action.isEnabled = true
                action.text = "Статус"
                action.setOnClickListener {
                    AlertDialog.Builder(this@ManaExtensionCenterActivity)
                        .setTitle(module.title)
                        .setMessage("Этот модуль сохранён в плане MANA Genesis 4.0, но его рабочая реализация ещё не включена в сборку. Он не будет показан как готовая функция.")
                        .setPositiveButton("Понятно", null)
                        .show()
                }
            }
            card.addView(row)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.bottomMargin = dp(10)
            list.addView(card, params)
        }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(scroll, LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        root.addView(Button(this).apply {
            text = "Закрыть"
            setOnClickListener { finish() }
        })
        setContentView(root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
