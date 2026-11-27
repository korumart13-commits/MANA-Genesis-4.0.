package com.mana.launcher

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.mana.brain.EngineeringSourceFile
import com.mana.brain.EngineeringTask
import com.mana.brain.ManaEngineeringCopilot
import kotlinx.coroutines.launch

/** Owner-controlled GPT engineering workspace. It proposes diffs; it never applies them. */
class ManaEngineeringActivity : ComponentActivity() {
    private lateinit var provider: ManaCloudGateway
    private lateinit var github: ManaGitHubActionsConnector
    private lateinit var repositoryPaths: EditText
    private lateinit var objective: EditText
    private lateinit var status: TextView
    private lateinit var sourceSummary: TextView
    private lateinit var result: TextView
    private var selectedFiles: List<EngineeringSourceFile> = emptyList()

    private val sourcePicker = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> loadSelectedSources(uris) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        provider = ManaEngineProcessOwner.cloudGateway() ?: ManaCloudGateway(this, ManaAndroidKeyProvider())
        github = ManaGitHubActionsConnector(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(24))
            setBackgroundColor(0xFF071426.toInt())
        }
        root.addView(text("MANA Engineering Lab", 23, 0xFFFFFFFF.toInt(), true))
        root.addView(text(
            "GPT помогает проектировать изменения MANA. Выбранные файлы отправляются настроенному облачному провайдеру только после отдельного подтверждения. Результат — предложение diff для проверки, не автоматическое изменение проекта.",
            12, 0xFF9CB6D4.toInt()
        ))
        status = text("Провайдер: ${provider.selectedProvider()} • ${if (provider.isAvailable()) "готов" else "не настроен"}", 12, 0xFF69C6FF.toInt())
        root.addView(status)
        objective = EditText(this).apply {
            hint = "Что нужно разработать или исправить в MANA?"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CB6D4.toInt())
            minLines = 3
            maxLines = 6
        }
        root.addView(objective, LinearLayout.LayoutParams(-1, -2))
        root.addView(button("Выбрать исходные файлы") {
            sourcePicker.launch(arrayOf("text/*", "application/json", "application/xml"))
        })
        repositoryPaths = EditText(this).apply {
            hint = "Путь каждого файла в GitHub, по одному на строку, в порядке выбора"
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CB6D4.toInt())
            minLines = 2
            maxLines = 5
        }
        root.addView(repositoryPaths, LinearLayout.LayoutParams(-1, -2))
        root.addView(button("Применить пути GitHub к выбранным файлам") { applyRepositoryPaths() })
        sourceSummary = text("Исходники не выбраны. Без указания путей файлы остаются в docs/owner-context/.", 12, 0xFF9CB6D4.toInt())
        root.addView(sourceSummary)
        root.addView(button("Подключить GitHub Actions") { configureGitHub() })
        root.addView(button("Запустить CI и автоисправление (до 2 повторов)") { confirmAndRunEngineeringLoop() })
        root.addView(sourceSummary)
        root.addView(button("Попросить GPT подготовить патч") { confirmAndGenerate() })
        root.addView(button("Обновить статус облачного провайдера") {
            status.text = "Провайдер: ${provider.selectedProvider()} • ${if (provider.isAvailable()) "готов" else "не настроен"}"
        })
        root.addView(text("Предложение MANA", 18, 0xFFFFFFFF.toInt(), true))
        result = text("Здесь появятся описание, diff, тест-план и риски.", 12, 0xFFEAF4FF.toInt())
        root.addView(ScrollView(this).apply {
            addView(result)
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        })
        root.addView(button("Скопировать предложение") {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("MANA engineering proposal", result.text))
            status.text = "Предложение скопировано. Оно ещё не применено."
        })
        setContentView(root)
    }


    private fun applyRepositoryPaths() {
        if (selectedFiles.isEmpty()) { status.text = "Сначала выбери файлы."; return }
        val paths = repositoryPaths.text.toString().lines().map { it.trim() }.filter { it.isNotBlank() }
        if (paths.size != selectedFiles.size) {
            status.text = "Укажи ровно ${selectedFiles.size} путей — по одному на каждый выбранный файл."
            return
        }
        try {
            selectedFiles = selectedFiles.mapIndexed { index, file -> file.copy(path = paths[index]) }
            sourceSummary.text = "Подготовлено для GitHub:\n" + selectedFiles.joinToString("\n") { it.path }
            status.text = "Пути применены. Они будут проверены политикой MANA перед отправкой."
        } catch (error: Exception) { status.text = error.message ?: "Недопустимый путь." }
    }

    private fun configureGitHub() {
        val input = EditText(this).apply {
            hint = "Fine-grained GitHub token (Actions: read/write; Contents: read)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this)
            .setTitle("Подключить GitHub Actions")
            .setMessage("Создай fine-grained token только для репозитория ${ManaGitHubActionsConnector.REPOSITORY}. Дай Actions: read/write и Contents: read. Токен будет зашифрован Android Keystore и не попадёт в исходники или CI-логи.")
            .setView(input)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить") { _, _ ->
                try {
                    github.configure(input.text.toString())
                    status.text = "GitHub Actions подключён. Токен хранится в Android Keystore-backed vault."
                    input.setText("")
                } catch (error: Exception) { status.text = error.message ?: "Не удалось сохранить токен." }
            }.show()
    }

    private fun confirmAndRunEngineeringLoop() {
        val goal = objective.text.toString().trim()
        if (goal.isBlank() || selectedFiles.isEmpty()) { status.text = "Нужны описание задачи и выбранные исходники."; return }
        if (!github.isConfigured()) { status.text = "Сначала подключи GitHub Actions."; return }
        if (!provider.isAvailable()) { status.text = "Сначала настрой и включи облачный инженерный провайдер MANA."; return }
        val task = EngineeringTask(
            id = "mana-auto-${System.currentTimeMillis()}",
            objective = goal,
            targetDevice = "realme RMX5566RU / RE645F",
            targetBuild = "Android 16 / RMX5566_16.0.5.1310(EX01)",
            sourceFiles = selectedFiles,
            ownerApprovedContext = true,
            allowCloud = true
        )
        AlertDialog.Builder(this)
            .setTitle("Разрешить инженерный цикл MANA?")
            .setMessage("MANA отправит выбранные исходники и задачу в ${provider.selectedProvider()}, предложит diff, запустит GitHub Actions и прочитает логи при ошибке. Разрешено не более 2 автоматических повторов (3 запуска CI всего). Изменения выполняются только во временном CI checkout: без commit, merge, установки APK или прошивки. Облачный запрос может тарифицироваться.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Разрешить один цикл") { _, _ ->
                status.text = "MANA получает SHA ветки и запускает проверку…"
                lifecycleScope.launch {
                    try {
                        val outcome = github.run(provider, task, maxRepairAttempts = 2)
                        result.text = buildString {
                            appendLine(if (outcome.succeeded) "CI: SUCCESS" else "CI: NOT PASSED")
                            appendLine("Запусков: ${outcome.attempts.size} / максимум 3")
                            outcome.attempts.forEach { attempt ->
                                appendLine("\nПопытка ${attempt.attempt}: ${attempt.run.conclusion ?: attempt.run.status}")
                                appendLine(attempt.patchSummary)
                                appendLine(attempt.run.htmlUrl)
                                attempt.failureLogExcerpt?.let { appendLine("Логи ошибки:\n" + it) }
                            }
                            appendLine("\nПатчи не были закоммичены или установлены. CI проверял их в одноразовой среде.")
                        }
                        status.text = if (outcome.succeeded) "GitHub Actions успешно проверил кандидат. Никаких изменений в ветку не записано." else "Автоисправление остановлено после ограниченного числа попыток. Требуется разбор отчёта."
                    } catch (error: Exception) {
                        status.text = "Инженерный цикл не выполнен: ${error.message}"
                        result.text = "Нет подтверждённого успешного результата. Проверь права GitHub token, сеть и состояние workflow."
                    }
                }
            }.show()
    }

    private fun loadSelectedSources(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (uris.size > 12) {
            status.text = "Можно выбрать не более 12 файлов за один запрос."
            return
        }
        try {
            val loaded = uris.map { uri ->
                val name = queryDisplayName(uri).orEmpty()
                val safeName = name.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
                    .take(100).ifBlank { "source.txt" }
                val content = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    val out = StringBuilder()
                    val buffer = CharArray(4096)
                    while (true) {
                        val count = it.read(buffer)
                        if (count < 0) break
                        require(out.length + count <= EngineeringSourceFile.MAX_FILE_CHARS) {
                            "Файл $safeName превышает лимит 48 КБ."
                        }
                        out.append(buffer, 0, count)
                    }
                    out.toString()
                } ?: error("Не удалось прочитать $safeName.")
                EngineeringSourceFile("docs/owner-context/$safeName", content)
            }
            selectedFiles = loaded
            sourceSummary.text = "Выбрано файлов: ${loaded.size}\n" + loaded.joinToString("\n") { it.path }
            status.text = "Файлы прочитаны локально. Отправка ещё не выполнялась."
        } catch (error: Exception) {
            selectedFiles = emptyList()
            sourceSummary.text = "Не удалось подготовить файлы."
            status.text = error.message ?: "Ошибка чтения исходников."
        }
    }

    private fun confirmAndGenerate() {
        val goal = objective.text.toString().trim()
        if (goal.isBlank()) { status.text = "Сначала опиши задачу."; return }
        if (selectedFiles.isEmpty()) { status.text = "Сначала выбери исходные файлы."; return }
        if (!provider.isAvailable()) {
            status.text = "Настрой и включи облачный провайдер в главном меню MANA."
            return
        }
        val names = selectedFiles.joinToString("\n") { it.path }
        AlertDialog.Builder(this)
            .setTitle("Разрешить отправку исходников?")
            .setMessage(
                "Провайдер: ${provider.selectedProvider()}\nМодель: ${provider.model()}\n\nЗадача: $goal\n\nФайлы:\n$names\n\nИх содержимое и описание задачи будут отправлены провайдеру. Возможна тарификация. MANA получит только предложение diff; оно не будет автоматически применено."
            )
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Разрешить один запрос") { _, _ -> generateProposal(goal) }
            .show()
    }

    private fun generateProposal(goal: String) {
        status.text = "MANA отправляет подтверждённый инженерный запрос…"
        result.text = "Ожидание ответа провайдера."
        lifecycleScope.launch {
            try {
                val task = EngineeringTask(
                    id = "owner-${System.currentTimeMillis()}",
                    objective = goal,
                    targetDevice = "realme RMX5566RU / RE645F",
                    targetBuild = "Android 16 / RMX5566_16.0.5.1310(EX01)",
                    sourceFiles = selectedFiles,
                    ownerApprovedContext = true,
                    allowCloud = true
                )
                val proposal = ManaEngineeringCopilot(provider).propose(task)
                result.text = buildString {
                    appendLine("СТАТУС: ${proposal.status}")
                    appendLine("ПРОВАЙДЕР: ${proposal.providerId}")
                    appendLine("ОПИСАНИЕ:")
                    appendLine(proposal.summary)
                    appendLine("\nИЗМЕНЯЕМЫЕ ПУТИ:")
                    proposal.changedPaths.forEach { appendLine(it) }
                    appendLine("\nТЕСТ-ПЛАН:")
                    proposal.testPlan.forEach { appendLine("• $it") }
                    appendLine("\nРИСКИ:")
                    proposal.riskNotes.forEach { appendLine("• $it") }
                    appendLine("\nDIFF:")
                    appendLine(proposal.unifiedDiff)
                    appendLine("\nЭто предложение. Код не изменён и не запускался.")
                }
                status.text = "Предложение готово к ручной проверке. Автоматического применения нет."
            } catch (error: Exception) {
                status.text = "Инженерный запрос не выполнен: ${error.message}"
                result.text = "Нет принятого предложения. Повтори после проверки настроек."
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        return uri.lastPathSegment
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
        setPadding(0, dp(7), 0, dp(7))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
