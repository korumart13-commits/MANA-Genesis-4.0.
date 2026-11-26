package com.mana.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Guided, owner-controlled MANA AutoDev bridge. No root or arbitrary shell input. */
class ManaTermuxAgentActivity : Activity() {
    private enum class Task(val title: String, val command: String, val description: String) {
        STATUS("Проверить проект", "git status --short && git branch --show-current", "Показывает активную ветку и незакоммиченные изменения."),
        CHECK("Проверить ошибки изменений", "git diff --check", "Проверяет пробелы и конфликтные маркеры в изменённых файлах."),
        TEST("Запустить тесты MANA", "gradle -p os/core --no-daemon test && gradle -p engine/mana-brain --no-daemon test", "Запускает JVM-тесты ядра и MANA Brain. Нужны Java 17 и Gradle."),
        APK("Собрать приложение MANA", "gradle -p launcher --no-daemon :app:assembleDebug :app:testDebugUnitTest", "Собирает и тестирует APK. Нужны Java 17, Gradle и Android SDK."),
        DEVICE_AUDIT("Проверить телефон", "bash ./device/collect_rmx5566_identity.sh", "Собирает диагностику устройства без root и записи в системные разделы."),
        AUTODEV_ENGINE("Проверить изменение движка MANA", "python tools/mana_autodev.py --repo . --patch \"$HOME/.mana/autodev/incoming-engine.patch\" --track engine", "Проверяет патч движка в отдельной Git worktree, запускает тесты и создаёт локальную ветку-кандидат. Требуется заранее подготовленный патч и Python/Gradle."),
        AUTODEV_APPS("Проверить изменение приложений и модулей", "python tools/mana_autodev.py --repo . --patch \"$HOME/.mana/autodev/incoming-apps.patch\" --track apps", "Проверяет патч приложений в изолированной копии, собирает debug APK и запускает unit-тесты. Требуется Android SDK и заранее подготовленный патч."),
        AUTODEV_KERNEL("Проверить патч ядра MANA", "python tools/mana_autodev.py --repo . --patch \"$HOME/.mana/autodev/incoming-kernel.patch\" --track kernel", "Проверяет только review-only patchset для нового драйвера MANA. Не собирает и не прошивает ядро; точный OEM kernel source пока не подтверждён.")
    }

    private val prefs by lazy { getSharedPreferences("mana_termux_agent", MODE_PRIVATE) }
    private val homePrefix = "/data/data/com.termux/files/home/"
    private val navy = Color.rgb(7, 20, 38)
    private val surface = Color.rgb(16, 43, 80)
    private val primary = Color.rgb(23, 101, 168)
    private val accent = Color.rgb(84, 200, 245)
    private val textColor = Color.rgb(234, 244, 255)
    private val muted = Color.rgb(145, 169, 197)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = navy
        showHome()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            this.text = value
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
        setBackgroundColor(surface)
    }

    private fun action(title: String, important: Boolean = false, run: () -> Unit) =
        Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 14f
            setTextColor(textColor)
            backgroundTintList = ColorStateList.valueOf(if (important) primary else surface)
            setOnClickListener { run() }
        }

    private fun termuxInstalled(): Boolean = try {
        packageManager.getPackageInfo("com.termux", PackageManager.GET_SERVICES)
        true
    } catch (_: Exception) { false }

    private fun runPermissionGranted(): Boolean =
        checkSelfPermission("com.termux.permission.RUN_COMMAND") == PackageManager.PERMISSION_GRANTED

    private fun projectDir(): String =
        prefs.getString("project_dir", homePrefix + "MANA-Genesis-4.0")!!

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(18))
            setBackgroundColor(navy)
        }
        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        body.addView(text("MANA", 32f, textColor, true))
        body.addView(text("AUTODEV", 12f, accent, true).apply {
            setPadding(0, dp(2), 0, dp(18))
        })

        val installed = termuxInstalled()
        val granted = installed && runPermissionGranted()
        val status = card()
        status.addView(text("СРЕДА РАЗРАБОТКИ", 12f, accent, true))
        status.addView(text(
            if (!installed) "Нужно подключить Termux"
            else if (!granted) "Termux найден — ожидается разрешение Android"
            else "Среда подключена",
            18f, textColor, true
        ).apply { setPadding(0, dp(7), 0, dp(5)) })
        status.addView(text(
            if (!installed) "Termux нужен для безопасного запуска сборки и проверок на телефоне."
            else if (!granted) "Открой настройки MANA и разреши запуск команд. Затем включи внешний запуск в Termux."
            else "Напиши задачу обычными словами. MANA сама выберет подходящую безопасную операцию.",
            13f, muted
        ))
        body.addView(status, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })

        body.addView(text("ЧТО СДЕЛАТЬ С ПРОЕКТОМ?", 13f, accent, true))
        val request = EditText(this).apply {
            setTextColor(textColor)
            setHintTextColor(muted)
            textSize = 15f
            minLines = 2
            maxLines = 4
            gravity = android.view.Gravity.TOP
            setPadding(dp(14), dp(13), dp(14), dp(13))
            hint = "Например: проверь проект, запусти тесты или собери APK"
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(surface)
                cornerRadius = dp(18).toFloat()
                setStroke(dp(1), primary)
            }
        }
        body.addView(request, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9) })
        body.addView(action("Выполнить задачу", true) {
            val query = request.text.toString().trim().lowercase()
            val task = when {
                query.contains("ядр") || query.contains("драйвер") -> Task.AUTODEV_KERNEL
                query.contains("движ") || query.contains("self") || query.contains("саморазв") -> Task.AUTODEV_ENGINE
                query.contains("прилож") || query.contains("модул") -> Task.AUTODEV_APPS
                query.contains("собер") || query.contains("apk") -> Task.APK
                query.contains("тест") -> Task.TEST
                query.contains("ошиб") || query.contains("diff") -> Task.CHECK
                query.contains("телефон") || query.contains("устройств") || query.contains("диагност") -> Task.DEVICE_AUDIT
                query.contains("проект") || query.contains("статус") || query.contains("ветк") || query.contains("проверь") -> Task.STATUS
                else -> null
            }
            if (task == null) {
                Toast.makeText(this, "Попробуй: «проверь проект», «запусти тесты», «собери APK» или «проверь телефон».", Toast.LENGTH_LONG).show()
            } else if (!installed) showTermuxMissing()
            else if (!granted) showTermuxConnection()
            else confirmTask(task)
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { topMargin = dp(12) })

        body.addView(text(
            "Доступно: проверки проекта, тесты, сборка APK и проверка подготовленных патчей движка, приложений и kernel-track. Патчи должны находиться в ~/.mana/autodev/; каждый кандидат проверяется отдельно.",
            12f, muted
        ).apply { setPadding(0, dp(12), 0, dp(16)) })
        body.addView(text("ПАПКА ПРОЕКТА", 11f, accent, true))
        body.addView(text(projectDir(), 12f, textColor).apply { setPadding(0, dp(5), 0, dp(5)) })
        body.addView(text("Изменить папку проекта", 13f, accent).apply {
            setPadding(0, dp(8), 0, dp(12))
            setOnClickListener { editProjectDir() }
        })
        body.addView(text("Инструкция по подключению Termux", 13f, accent).apply {
            setPadding(0, dp(8), 0, dp(12))
            setOnClickListener { showSetup() }
        })
        body.addView(text(
            "MANA не получает root и не записывает данные в системные разделы. Сборка полной прошивки требует отдельной AOSP-среды и проверенных файлов устройства.",
            11f, muted
        ))
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(action("Назад в MANA") { finish() })
        setContentView(root)
    }

    private fun editProjectDir() {
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(projectDir())
            hint = homePrefix + "MANA-Genesis-4.0"
        }
        AlertDialog.Builder(this)
            .setTitle("Папка проекта")
            .setMessage("Укажи путь внутри домашней папки Termux.")
            .setView(input)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить") { _, _ ->
                val value = input.text.toString().trim().trimEnd('/')
                if (!value.startsWith(homePrefix) || value.contains("..") ||
                    !value.matches(Regex("/[A-Za-z0-9_./-]+"))) {
                    Toast.makeText(this, "Нужен путь внутри Termux HOME", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("project_dir", value).apply()
                    showHome()
                }
            }.show()
    }

    private fun showTermuxMissing() {
        AlertDialog.Builder(this)
            .setTitle("Нужен Termux")
            .setMessage("MANA AutoDev использует Termux как среду выполнения команд. Установи Termux из официального источника, открой его один раз и вернись сюда.")
            .setPositiveButton("Открыть страницу Termux") { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/termux/termux-app/releases")))
            }
            .setNegativeButton("Позже", null)
            .show()
    }

    private fun showTermuxConnection() {
        if (!termuxInstalled()) { showTermuxMissing(); return }
        AlertDialog.Builder(this)
            .setTitle("Подключение Termux")
            .setMessage("1. Открой настройки разрешений MANA и включи RUN_COMMAND, если Android показывает этот пункт.\n\n2. В Termux включи allow-external-apps=true в файле ~/.termux/termux.properties и перезапусти Termux.\n\n3. Вернись сюда и проверь подключение. Эти системные подтверждения выполняет владелец телефона.")
            .setPositiveButton("Настройки MANA") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + packageName)))
            }
            .setNeutralButton("Открыть Termux") { _, _ ->
                packageManager.getLaunchIntentForPackage("com.termux")?.let(::startActivity)
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showSetup() {
        val command = "mkdir -p ~/.termux; touch ~/.termux/termux.properties; grep -qxF 'allow-external-apps=true' ~/.termux/termux.properties || printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties; termux-reload-settings"
        AlertDialog.Builder(this)
            .setTitle("Первое подключение — по шагам")
            .setMessage("1. Установи и один раз открой Termux.\n2. Нажми «Скопировать команду» и вставь её в Termux.\n3. В Android открой настройки MANA и разреши RUN_COMMAND, если система предлагает этот доступ.\n4. Вернись в MANA и проверь подключение.")
            .setPositiveButton("Скопировать команду") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("MANA Termux setup", command))
                Toast.makeText(this, "Команда скопирована. Открой Termux и вставь её.", Toast.LENGTH_LONG).show()
                packageManager.getLaunchIntentForPackage("com.termux")?.let(::startActivity)
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun chooseTask() {
        AlertDialog.Builder(this)
            .setTitle("Что сделать?")
            .setItems(Task.values().map { it.title }.toTypedArray()) { _, index ->
                confirmTask(Task.values()[index])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmTask(task: Task) {
        AlertDialog.Builder(this)
            .setTitle(task.title)
            .setMessage(task.description + "\n\nПапка: " + projectDir() +
                "\n\nКоманда: " + task.command + "\n\nЗапустить в Termux?")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Запустить") { _, _ -> runTask(task) }
            .show()
    }

    private fun runTask(task: Task) {
        if (!termuxInstalled() || !runPermissionGranted()) {
            showTermuxConnection()
            return
        }
        val command = Intent("com.termux.RUN_COMMAND").apply {
            component = ComponentName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", task.command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", projectDir())
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            putExtra("com.termux.RUN_COMMAND_COMMAND_LABEL", "MANA AutoDev: " + task.title)
            putExtra("com.termux.RUN_COMMAND_COMMAND_DESCRIPTION", task.description)
        }
        try {
            startService(command)
            Toast.makeText(this, "Задача передана Termux. Результат появится в терминале.", Toast.LENGTH_LONG).show()
        } catch (error: Exception) {
            Toast.makeText(this, "Не удалось запустить задачу: " + error.message, Toast.LENGTH_LONG).show()
        }
    }
}
