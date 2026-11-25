package com.mana.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Owner-controlled bridge to Termux.
 *
 * This first release deliberately exposes only fixed, non-root project checks/builds.
 * It does not accept arbitrary shell text, does not invoke su, and does not touch
 * Android system partitions. Every operation requires a visible confirmation.
 */
class ManaTermuxAgentActivity : Activity() {

    private enum class Task(val title: String, val command: String, val description: String) {
        STATUS("Проверить проект", "git status --short && git branch --show-current",
            "Показывает ветку и незакоммиченные изменения."),
        CHECK("Проверить изменения", "git diff --check",
            "Ищет ошибки пробелов и конфликтные маркеры в diff."),
        TEST("Запустить тесты", "bash ./gradlew test --no-daemon",
            "Запускает Gradle-тесты проекта."),
        APK("Собрать APK", "bash ./gradlew :launcher:app:assembleDebug --no-daemon",
            "Собирает отладочный APK компаньона; не устанавливает его автоматически.")
    }

    private val prefs by lazy { getSharedPreferences("mana_termux_agent", MODE_PRIVATE) }
    private val homePrefix = "/data/data/com.termux/files/home/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 48, 36, 32)
        }
        root.addView(TextView(this).apply {
            text = "MANA CODE AGENT"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Связь с Termux. Команды запускаются только после отдельного подтверждения."
            textSize = 15f
            setPadding(0, 18, 0, 24)
        })
        root.addView(TextView(this).apply {
            text = "Папка проекта: ${projectDir()}"
            textSize = 13f
            setPadding(0, 0, 0, 18)
        })
        val actions = listOf(
            "Настроить папку проекта" to ::editProjectDir,
            "Проверить подключение Termux" to ::checkTermux,
            "Открыть список безопасных операций" to ::chooseTask,
            "Инструкция по первому подключению" to ::showSetup
        )
        actions.forEach { (label, action) ->
            root.addView(android.widget.Button(this).apply {
                text = label
                setOnClickListener { action() }
            })
        }
        setContentView(root)
    }

    private fun projectDir(): String =
        prefs.getString("project_dir", homePrefix + "MANA-Genesis-4.0")!!

    private fun editProjectDir() {
        val input = EditText(this).apply {
            setSingleLine(true)
            setText(projectDir())
            hint = homePrefix + "MANA-Genesis-4.0"
        }
        AlertDialog.Builder(this)
            .setTitle("Папка проекта в Termux")
            .setMessage("Укажи путь внутри домашней папки Termux. Путь с .. и внешние каталоги запрещены.")
            .setView(input)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить") { _, _ ->
                val value = input.text.toString().trim().trimEnd('/')
                if (!value.startsWith(homePrefix) ||
                    value.contains("..") ||
                    !value.matches(Regex("/[A-Za-z0-9_./-]+"))
                ) {
                    Toast.makeText(this, "Разрешена только папка внутри Termux HOME", Toast.LENGTH_LONG).show()
                } else {
                    prefs.edit().putString("project_dir", value).apply()
                    showHome()
                }
            }.show()
    }

    private fun checkTermux() {
        val installed = try {
            packageManager.getPackageInfo("com.termux", PackageManager.GET_SERVICES)
            true
        } catch (_: Exception) { false }

        if (!installed) {
            AlertDialog.Builder(this)
                .setTitle("Termux не найден")
                .setMessage("Установи Termux из официального источника, открой его один раз и заверши первоначальную установку.")
                .setPositiveButton("Понятно", null)
                .show()
            return
        }

        val permission = checkSelfPermission("com.termux.permission.RUN_COMMAND") == PackageManager.PERMISSION_GRANTED
        AlertDialog.Builder(this)
            .setTitle("Подключение Termux")
            .setMessage(
                "Termux установлен. Разрешение RUN_COMMAND: " +
                    if (permission) "выдано." else "не выдано. Его нужно выдать вручную в настройках приложения MANA." +
                    "\n\nВ Termux также требуется allow-external-apps=true. Это отдельное подтверждение внутри Termux."
            )
            .setPositiveButton("Открыть настройки MANA") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName")))
            }
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun showSetup() {
        AlertDialog.Builder(this)
            .setTitle("Первое подключение")
            .setMessage(
                "1. Установи и открой Termux.\n" +
                "2. В Termux выполни: mkdir -p ~/.termux && printf '\\nallow-external-apps=true\\n' >> ~/.termux/termux.properties && termux-reload-settings\n" +
                "3. В настройках Android открой MANA → Разрешения → дополнительные разрешения → Run commands in Termux environment и разреши доступ.\n" +
                "4. Склонируй репозиторий в $HOME/MANA-Genesis-4.0 и настрой Gradle/Java в Termux.\n" +
                "5. Вернись сюда и сначала запусти проверку проекта.\n\n" +
                "MANA не получает root. Termux-команды выполняются с обычными правами Termux."
            )
            .setPositiveButton("Понятно", null)
            .show()
    }

    private fun chooseTask() {
        AlertDialog.Builder(this)
            .setTitle("Операция MANA")
            .setItems(Task.values().map { it.title }.toTypedArray()) { _, index ->
                confirmTask(Task.values()[index])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun confirmTask(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Подтвердить запуск?")
            .setMessage(
                "${task.title}\n\n${task.description}\n\n" +
                    "Рабочая папка: ${projectDir()}\n" +
                    "Команда: ${task.command}\n\n" +
                    "Команда будет видна в Termux. Никакие изменения системных разделов не выполняются."
            )
            .setNegativeButton("Не запускать", null)
            .setPositiveButton("Запустить в Termux") { _, _ -> runTask(task) }
            .show()
    }

    private fun runTask(task: Task) {
        if (checkSelfPermission("com.termux.permission.RUN_COMMAND") != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Сначала выдай MANA разрешение RUN_COMMAND в настройках Android", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
            return
        }
        val termux = Intent("com.termux.RUN_COMMAND").apply {
            component = ComponentName("com.termux", "com.termux.app.RunCommandService")
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", task.command))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", projectDir())
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
            putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            putExtra("com.termux.RUN_COMMAND_LABEL", "MANA: ${task.title}")
            putExtra("com.termux.RUN_COMMAND_DESCRIPTION", task.description)
        }
        try {
            startService(termux)
            Toast.makeText(this, "Команда передана Termux. Проверь терминальную сессию.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Не удалось запустить Termux: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
