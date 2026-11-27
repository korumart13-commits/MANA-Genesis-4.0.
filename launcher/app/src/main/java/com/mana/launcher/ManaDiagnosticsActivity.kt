package com.mana.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.FileProvider
import java.io.File
import java.time.Instant

/** Owner-triggered diagnostic preview, export and Android Sharesheet handoff. */
class ManaDiagnosticsActivity : Activity() {
    private lateinit var report: ManaDiagnosticReport
    private lateinit var preview: TextView
    private val bg = 0xFF071426.toInt()
    private val fg = 0xFFEAF4FF.toInt()
    private val accent = 0xFF54C8F5.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        report = collectReport()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(bg)
        }
        root.addView(TextView(this).apply {
            text = "MANA • ДИАГНОСТИКА"
            textSize = 23f
            setTextColor(accent)
        })
        root.addView(TextView(this).apply {
            text = "Проверь отчёт перед отправкой. Секреты и личные данные исключаются."
            textSize = 14f
            setTextColor(fg)
            setPadding(0, 12, 0, 16)
        })
        preview = TextView(this).apply {
            text = report.toMarkdown()
            textSize = 13f
            setTextColor(fg)
            setTextIsSelectable(true)
        }
        root.addView(ScrollView(this).apply { addView(preview) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(button("Экспортировать JSON") { exportJson() })
        root.addView(button("Поделиться с ChatGPT") { shareReport() })
        root.addView(button("Обновить проверку") {
            report = collectReport()
            preview.text = report.toMarkdown()
        })
        setContentView(root)
    }

    private fun button(title: String, click: () -> Unit) = Button(this).apply {
        text = title
        setTextColor(fg)
        setOnClickListener { click() }
    }

    @Suppress("DEPRECATION")
    private fun collectReport(): ManaDiagnosticReport {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        val version = if (Build.VERSION.SDK_INT >= 28) packageInfo.longVersionCode.toString()
            else packageInfo.versionCode.toString()
        val permissionNames = listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.POST_NOTIFICATIONS,
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.BLUETOOTH_CONNECT
        )
        val permissions = permissionNames.associate { permission ->
            val state = if ((Build.VERSION.SDK_INT < 31 &&
                permission == android.Manifest.permission.BLUETOOTH_CONNECT) ||
                (Build.VERSION.SDK_INT < 33 &&
                permission == android.Manifest.permission.POST_NOTIFICATIONS)) {
                "NOT_APPLICABLE"
            } else if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                "GRANTED"
            } else "NOT_GRANTED_OR_NOT_REQUESTED"
            permission.substringAfterLast('.') to state
        }
        val modules = listOf(
            "mana_brain" to "Наличие APK не доказывает успешный inference; требуется runtime test.",
            "local_model_runtime" to "Загрузка модели и inference на этом устройстве не подтверждены этим отчётом.",
            "encrypted_memory" to "Проверка чтения/записи и восстановления после перезапуска не запускалась.",
            "voice" to "Проверка записи, распознавания и TTS на устройстве не запускалась.",
            "avatar" to "Проверка рендеринга и overlay на устройстве не запускалась.",
            "chatgpt_bridge" to "Проверяется сам экспорт; доставку отчёта в текущий чат нужно подтвердить вручную."
        ).map { (id, evidence) -> ManaDiagnosticModule(id, "NEEDS_DEVICE_TEST", evidence) }
        return ManaDiagnosticReport(
            generatedAtUtc = Instant.now().toString(),
            appVersion = version,
            androidApi = Build.VERSION.SDK_INT,
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            permissions = permissions,
            modules = modules,
            limitations = listOf(
                "Автоматическая отправка в текущий диалог ChatGPT не поддерживается Android-приложением.",
                "Этот отчёт не выполняет полный self-test каждого модуля; неподтверждённые функции помечены NEEDS_DEVICE_TEST.",
                "CI и физические тесты устройства в этом локальном отчёте не подтверждаются."
            )
        )
    }

    private fun writeExport(name: String, content: String): File {
        val directory = File(cacheDir, "mana-diagnostics").apply { mkdirs() }
        return File(directory, name).apply { writeText(content, Charsets.UTF_8) }
    }

    private fun shareReport() {
        runCatching {
            val file = writeExport("mana-diagnostic-report.md", report.toMarkdown())
            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/markdown"
                putExtra(Intent.EXTRA_SUBJECT, "MANA diagnostic report")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newUri(contentResolver, "MANA diagnostic report", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Передать отчёт"))
        }.onFailure { error ->
            preview.append("\n\nНе удалось открыть меню отправки: ${error.javaClass.simpleName}")
        }
    }

    private fun exportJson() {
        runCatching {
            val file = writeExport("mana-diagnostic-report.json", report.toJson())
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Экспортировать отчёт JSON"))
        }.onFailure { error ->
            preview.append("\n\nНе удалось экспортировать отчёт: ${error.javaClass.simpleName}")
        }
    }
}
