package com.mana.launcher

import android.app.Activity
import android.app.AlertDialog
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Owner-facing capability hub. Every sensitive operation is user initiated.
 * It uses public Android intents and never reads private messenger databases,
 * silently records calls, or bypasses device security.
 */
class ManaLifeControlCenterActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mana.life.control", MODE_PRIVATE) }
    private val navy = Color.rgb(7, 20, 38)
    private val surface = Color.rgb(16, 43, 80)
    private val primary = Color.rgb(23, 101, 168)
    private val accent = Color.rgb(84, 200, 245)
    private val foreground = Color.rgb(234, 244, 255)
    private val muted = Color.rgb(145, 169, 197)
    private val requestOwnerAuth = 7401

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        render()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun label(value: String, size: Float = 14f, color: Int = foreground, bold: Boolean = false) =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(5), 0, dp(5))
        }

    private fun button(value: String, action: () -> Unit) = Button(this).apply {
        text = value
        isAllCaps = false
        setTextColor(foreground)
        setBackgroundColor(primary)
        setOnClickListener { action() }
    }

    private fun render() {
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(24))
            setBackgroundColor(navy)
        }
        body.addView(label("MANA CONTROL CENTER", 25f, foreground, true))
        body.addView(label("Управление личностью, камерой, сообщениями, звонками, играми и режимом сна.", 13f, muted))
        body.addView(section("Личность владельца"))
        body.addView(label(ownerStatus(), 13f, accent))
        body.addView(button("Подтвердить личность владельца") { verifyOwner() })
        body.addView(label("MANA не распознаёт владельца скрытно. Подтверждение выполняется через защиту самого Android — PIN, пароль, графический ключ или биометрию, если она настроена.", 12f, muted))

        body.addView(section("Камера"))
        body.addView(button("Открыть камеру / сделать снимок") { openCamera() })
        body.addView(label("Камера запускается через системное приложение. Доступ к закрытому фону или скрытая съёмка не используются.", 12f, muted))

        body.addView(section("Мессенджеры"))
        val message = EditText(this).apply {
            hint = "Текст сообщения для отправки через выбранное приложение"
            setTextColor(foreground)
            setHintTextColor(muted)
            minLines = 2
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        body.addView(message)
        body.addView(button("Поделиться текстом через приложение") {
            val value = message.text.toString().trim()
            if (value.isBlank()) toast("Сначала введи текст сообщения.")
            else {
                val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, value)
                startActivity(Intent.createChooser(share, "Отправить через…"))
            }
        })
        body.addView(button("Открыть SMS") {
            val value = message.text.toString()
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:")).putExtra("sms_body", value))
        })
        body.addView(label("MANA может открыть приложение и передать подготовленный текст. Чтение личных переписок и отправка от твоего имени без подтверждения не включены; для каждой платформы нужна официальная интеграция.", 12f, muted))

        body.addView(section("Звонки"))
        body.addView(button("Открыть телефон MANA") {
            startActivity(Intent(this, ManaDialerActivity::class.java))
        })
        body.addView(label("Полный интерфейс звонков требует добровольного назначения MANA приложением телефона. Автоответ и разговор с абонентом ограничены Android Telecom и не считаются проверенными без теста на устройстве.", 12f, muted))

        body.addView(section("Игры"))
        body.addView(label("Текущая игра: ${foregroundPackage() ?: "не определена (нужен доступ к статистике использования)"}", 13f, accent))
        body.addView(button("Разрешить обнаружение активной игры") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        body.addView(button("Открыть Realme Game Space") {
            if (!ManaRealmeGameSpaceBridge(this).open()) toast("Realme Game Space не найден на устройстве.")
        })
        body.addView(label("Сейчас доступно обнаружение активного приложения и открытие OEM Game Space. Управление персонажем и Minecraft-бот требуют отдельного игрового API и не включаются этим экраном.", 12f, muted))

        body.addView(section("Сон и ночной режим"))
        body.addView(label(if (prefs.getBoolean("sleep_mode", false)) "Ночной режим MANA включён." else "Ночной режим MANA выключен.", 13f, accent))
        body.addView(button("Включить / выключить ночной режим MANA") { toggleSleepMode() })
        body.addView(button("Настроить системный режим «Не беспокоить»") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        })
        body.addView(label("Переключатель сохраняет профиль MANA. Системный режим «Не беспокоить» меняется только после отдельной выдачи доступа и отдельного действия владельца. Автоматическое расписание сна ещё требует интеграции и тестирования.", 12f, muted))

        body.addView(section("Самоменеджмент и развитие"))
        body.addView(button("Открыть AutoDev / центр разработчика") {
            startActivity(Intent(this, ManaTermuxAgentActivity::class.java))
        })
        body.addView(button("Открыть адаптацию и обновления системы") {
            startActivity(Intent(this, ManaSystemAdaptationActivity::class.java))
        })
        body.addView(label("AutoDev готовит и тестирует изменения в изолированной среде. Прошивка Android/Realme допускается только через совместимую сборку, OEM-подпись и поддерживаемый путь установки; этот APK не перепрошивает защищённые разделы.", 12f, muted))

        val scroll = ScrollView(this).apply { addView(body) }
        setContentView(scroll)
    }

    private fun section(title: String) = label(title.uppercase(), 13f, accent, true).apply {
        setPadding(0, dp(18), 0, dp(5))
    }

    private fun ownerStatus(): String =
        if (prefs.getLong("owner_verified_at", 0L) > 0L) "Последнее подтверждение: ${android.text.format.DateFormat.format("dd.MM.yyyy HH:mm", prefs.getLong("owner_verified_at", 0L))}"
        else "Личность владельца ещё не подтверждена в MANA."

    private fun verifyOwner() {
        val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (!manager.isDeviceSecure) {
            AlertDialog.Builder(this)
                .setTitle("Защити устройство")
                .setMessage("Сначала настрой блокировку экрана Android: PIN, пароль или графический ключ. MANA не будет создавать собственный скрытый механизм распознавания.")
                .setPositiveButton("Открыть настройки") { _, _ -> startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
                .setNegativeButton("Отмена", null)
                .show()
            return
        }
        @Suppress("DEPRECATION")
        val intent = manager.createConfirmDeviceCredentialIntent("Подтверди владельца", "Подтверждение личности для MANA")
        if (intent == null) {
            toast("Android не предоставил экран подтверждения.")
            return
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, requestOwnerAuth)
    }

    @Deprecated("Android credential confirmation result")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == requestOwnerAuth && resultCode == RESULT_OK) {
            prefs.edit().putLong("owner_verified_at", System.currentTimeMillis()).apply()
            toast("Личность подтверждена средствами Android.")
            render()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) == null) {
            toast("На телефоне не найдено приложение камеры.")
            return
        }
        startActivity(Intent.createChooser(intent, "Камера"))
    }

    private fun foregroundPackage(): String? = runCatching {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val allowed = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        ) == android.app.AppOpsManager.MODE_ALLOWED
        if (!allowed) return null
        val manager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = manager.queryEvents(now - 10 * 60 * 1000L, now)
        val event = android.app.usage.UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) last = event.packageName
        }
        last
    }.getOrNull()

    private fun toggleSleepMode() {
        val enabled = !prefs.getBoolean("sleep_mode", false)
        prefs.edit().putBoolean("sleep_mode", enabled).apply()
        if (enabled) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.isNotificationPolicyAccessGranted) {
                runCatching { manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE) }
                    .onFailure { toast("Не удалось включить системный режим тишины.") }
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Нужен доступ «Не беспокоить»")
                    .setMessage("Профиль ночного режима MANA сохранён. Чтобы менять системную тишину, отдельно разреши доступ.")
                    .setPositiveButton("Настроить") { _, _ -> startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }
                    .setNegativeButton("Позже", null)
                    .show()
            }
        } else {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.isNotificationPolicyAccessGranted) {
                runCatching { manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL) }
            }
        }
        render()
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
