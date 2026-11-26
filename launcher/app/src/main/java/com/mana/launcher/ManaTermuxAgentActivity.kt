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
        DEVICE_AUDIT("Проверить телефон", "bash ./device/collect_rmx5566_identity.sh", "Собирает диагностику устройства без root и записи в системные разделы.")
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
        body.addView(text("AUTODEV  •  ПОМОЩНИК РАЗРАБОТКИ", 12f, accent, true).apply {
            setPadding(0, dp(2), 0, dp(18))
        })

        val installed = termuxInstalled()
        val granted = installed && runPermissionGranted()
        val status = card()
        status.addView(text("СОСТОЯНИЕ ПОДКЛЮЧЕНИЯ", 12f, accent, true))
        status.addView(text(
            if (!installed) "Termux пока не установлен"
            else if (!granted) "Termux найден — осталось подключить разрешение"
            else "Подключение готово — можно запускать проверки",
            17f, textColor, true
        ).apply { setPadding(0, dp(7), 0, dp(5)) })
        status.addView(text(
            if (!installed) "Установи Termux и открой его один раз. MANA объяснит следующий шаг."
            else if (!granted) "Android должен разрешить MANA запускать команды в Termux. Это подтверждается владельцем телефона."
            else "Выбирай задачу обычными словами. Перед запуском MANA покажет описание и команду.",
            13f, muted
        ))
        body.addView(status, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })

        body.addView(text("НАЧАТЬ РАБОТУ", 13f, accent, true).apply {
            setPadding(0, dp(4), 0, dp(7))
        })
        body.addView(action(if (!installed || !granted) "1. Подключить Termux" else "1. Проверить проект", true) {
            if (!installed) showTermuxMissing()
            else if (!granted) showTermuxConnection()
            else confirmTask(Task.STATUS)
        }, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(8) })
        body.addView(action("2. Проверить, протестировать или собрать APK") {
            if (!installed || !granted) showTermuxConnection() else chooseTask()
        }, LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(8) })
        body.addView(action("3. Пошаговая инструкция") { showSetup() },
            LinearLayout.LayoutParams(-1, dp(52)).apply { bottomMargin = dp(14) })

        val project = card()
        project.addView(text("ПАПКА ПРОЕКТА", 12f, accent, true))
        project.addView(text(projectDir(), 13f, textColor).apply { setPadding(0, dp(6), 0, dp(8)) })
        project.addView(action("Изменить папку проекта") { editProjectDir() })
        body.addView(project, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        body.addView(text(
            "Важно: MANA не получает root и не меняет системные разделы. Сборка полной прошивки требует отдельной AOSP-среды и проверенных файлов именно твоего устройства.",
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
