package com.mana.launcher

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mana.brain.BrainRequest
import com.mana.brain.ExperimentOutcome
import com.mana.brain.InferenceProvider
import com.mana.brain.JsonFileManaSkillStore
import com.mana.brain.JsonlResearchJournal
import com.mana.brain.ManaAutonomyEngine
import com.mana.brain.ManaBrain
import com.mana.brain.ResearchExperiment
import com.mana.brain.ResearchGoal
import com.mana.brain.ResearchJournalEntry
import com.mana.brain.ResearchPlanner
import com.mana.brain.ResearchRunner
import com.mana.brain.ResearchStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android host for bounded, local-only MANA research cycles.
 * It never sends research prompts to cloud providers and never executes generated code.
 */
class ManaAutonomyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return Result.success()

        val model = File(applicationContext.filesDir, "mana-models/selected-model.litertlm")
        if (!model.isFile) {
            saveStatus(prefs, "Ожидается локальная модель .litertlm. Фоновое исследование не запускалось.")
            return Result.success()
        }

        val runtime = LocalModelRuntime(File(applicationContext.cacheDir, "mana-autonomy-litert-cache"))
        return try {
            loadModel(runtime, model)
            val local = object : InferenceProvider {
                override val id: String = "litertlm-local"
                override fun isAvailable(): Boolean = runtime.isReady()
                override suspend fun generate(prompt: String): String = generateLocal(runtime, prompt)
            }
            val brain = ManaBrain(local)
            val planner = object : ResearchPlanner {
                override suspend fun propose(goal: ResearchGoal): ResearchExperiment {
                    val prompt = """
                        Ты — планировщик автономного исследования MANA. Составь один небольшой проверяемый
                        эксперимент ТОЛЬКО для внутренней проверки плана. Не используй интернет, файлы вне
                        каталога приложения, Android API, команды, сеть, отправку данных или внешние действия.
                        Метод обязан быть ровно LOCAL_POLICY_SELF_CHECK. Верни только JSON-объект с полями
                        hypothesis, method, successCriteria. Все значения — короткие строки на русском.
                        Цель: ${goal.description}
                        ID цели: ${goal.id}
                    """.trimIndent()
                    val raw = brain.respond(BrainRequest(prompt, allowCloud = false, sensitive = true)).text
                    val start = raw.indexOf('{')
                    val end = raw.lastIndexOf('}')
                    require(start >= 0 && end > start) { "Локальная модель не вернула JSON плана." }
                    val json = JSONObject(raw.substring(start, end + 1))
                    return ResearchExperiment(
                        id = "local-check-${System.currentTimeMillis()}",
                        goalId = goal.id,
                        hypothesis = json.getString("hypothesis").take(500),
                        method = json.getString("method").take(100),
                        successCriteria = json.getString("successCriteria").take(500)
                    )
                }

                override suspend fun reflect(
                    goal: ResearchGoal,
                    experiment: ResearchExperiment,
                    outcome: ExperimentOutcome
                ): String {
                    val prompt = """
                        Кратко оцени результат внутренней проверки MANA. Не делай внешних действий,
                        не добавляй неподтверждённых фактов. Укажи, что именно проверено и что осталось
                        непроверенным. Цель: ${goal.description}
                        Гипотеза: ${experiment.hypothesis}
                        Наблюдение: ${outcome.observation}
                        Доказательства: ${outcome.evidence.joinToString("; ")}
                    """.trimIndent()
                    return brain.respond(BrainRequest(prompt, allowCloud = false, sensitive = true)).text.take(1200)
                }
            }
            val runner = ResearchRunner { experiment ->
                val forbidden = Regex(
                    "https?://|\\b(shell|exec|command|network|internet|device|sms|call|payment|upload|delete)\\b",
                    RegexOption.IGNORE_CASE
                )
                val passed = experiment.method == "LOCAL_POLICY_SELF_CHECK" &&
                    experiment.hypothesis.isNotBlank() &&
                    experiment.successCriteria.isNotBlank() &&
                    !forbidden.containsMatchIn(experiment.method + " " + experiment.hypothesis + " " + experiment.successCriteria)
                ExperimentOutcome(
                    success = passed,
                    verified = passed,
                    observation = if (passed)
                        "План прошёл детерминированную проверку локальной политики. Это подтверждает только структуру и границы плана, не истинность внешних фактов."
                    else
                        "План отклонён: метод не соответствует локальной проверке или содержит запрещённое внешнее действие.",
                    evidence = listOf("mana-local-policy-verifier:v1", "no-network-or-device-execution")
                )
            }
            val journal = JsonlResearchJournal(File(applicationContext.filesDir, "mana/research/research.jsonl").toPath())
            // Initialize the persistent skill store in private app storage for the skill-evolution stage.
            JsonFileManaSkillStore(File(applicationContext.filesDir, "mana/research/skills").toPath()).all()
            val goalText = prefs.getString(KEY_GOAL, DEFAULT_GOAL).orEmpty().ifBlank { DEFAULT_GOAL }
            val goal = ResearchGoal("owner-research-goal", goalText.take(1000), priority = 1)
            val report = ManaAutonomyEngine(planner, runner, journal).cycle(goal)
            saveStatus(prefs, "${report.status}: ${report.summary}")
            Result.success()
        } catch (error: Exception) {
            saveStatus(prefs, "Ошибка локального исследовательского цикла: ${error.message ?: error.javaClass.simpleName}")
            Result.retry()
        } finally {
            runtime.shutdown()
        }
    }

    private suspend fun loadModel(runtime: LocalModelRuntime, file: File) =
        suspendCancellableCoroutine<Unit> { continuation ->
            runtime.load(file) { result ->
                result.fold(
                    onSuccess = { if (continuation.isActive) continuation.resume(Unit) },
                    onFailure = { if (continuation.isActive) continuation.resumeWithException(it) }
                )
            }
        }

    private suspend fun generateLocal(runtime: LocalModelRuntime, prompt: String): String =
        suspendCancellableCoroutine { continuation ->
            val output = StringBuilder()
            runtime.generate(
                prompt,
                onChunk = { chunk -> output.append(chunk) },
                callback = { result ->
                    result.fold(
                        onSuccess = { if (continuation.isActive) continuation.resume(output.toString()) },
                        onFailure = { if (continuation.isActive) continuation.resumeWithException(it) }
                    )
                }
            )
        }

    private fun saveStatus(prefs: android.content.SharedPreferences, value: String) {
        prefs.edit().putString(KEY_STATUS, value)
            .putLong(KEY_LAST_RUN, System.currentTimeMillis()).apply()
    }

    companion object {
        const val PREFS = "mana.autonomy"
        const val KEY_ENABLED = "enabled"
        const val KEY_GOAL = "goal"
        const val KEY_STATUS = "last_status"
        const val KEY_LAST_RUN = "last_run"
        const val DEFAULT_GOAL = "Проверять внутренние исследовательские планы MANA и находить небольшие улучшения без внешних действий."
        private const val PERIODIC = "mana-autonomy-research-periodic"
        private const val INITIAL = "mana-autonomy-research-initial"

        fun enable(context: Context, goal: String) {
            require(goal.isNotBlank()) { "Укажи цель исследования." }
            val app = context.applicationContext
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_ENABLED, true).putString(KEY_GOAL, goal.trim().take(1000)).apply()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()
            WorkManager.getInstance(app).enqueueUniquePeriodicWork(
                PERIODIC,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ManaAutonomyWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
            )
            WorkManager.getInstance(app).enqueueUniqueWork(
                INITIAL,
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<ManaAutonomyWorker>()
                    .setConstraints(constraints)
                    .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                    .build()
            )
        }

        fun disable(context: Context) {
            val app = context.applicationContext
            app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, false).apply()
            WorkManager.getInstance(app).cancelUniqueWork(PERIODIC)
            WorkManager.getInstance(app).cancelUniqueWork(INITIAL)
        }

        fun isEnabled(context: Context): Boolean =
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false)
    }
}

/** Owner-facing controls and transparent status for the local autonomy worker. */
class ManaAutonomyActivity : Activity() {
    private lateinit var goal: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(0xFF071426.toInt())
        }
        fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(6), 0, dp(6))
        }
        root.addView(text("MANA Autonomous Research", 22f, 0xFFFFFFFF.toInt(), true))
        root.addView(text(
            "MANA выполняет ограниченные внутренние циклы на локальной модели. Облачная отправка, сеть, команды и управление устройством в этом модуле отключены. Проверка подтверждает только локальную структуру плана, а не истинность внешних фактов.",
            13f, 0xFFB7CBE2.toInt()
        ))
        goal = EditText(this).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9CB6D4.toInt())
            setText(getSharedPreferences(ManaAutonomyWorker.PREFS, MODE_PRIVATE)
                .getString(ManaAutonomyWorker.KEY_GOAL, ManaAutonomyWorker.DEFAULT_GOAL))
            minLines = 3
        }
        root.addView(goal, LinearLayout.LayoutParams(-1, -2))
        status = text("", 13f, 0xFF69C6FF.toInt())
        root.addView(status)
        root.addView(button("Включить автономные циклы") {
            runCatching { ManaAutonomyWorker.enable(this, goal.text.toString()) }
                .onSuccess { refresh(); toast("Автономные циклы включены. Работа выполняется только локально.") }
                .onFailure { toast("Не удалось включить: ${it.message}") }
        })
        root.addView(button("Остановить автономные циклы") {
            ManaAutonomyWorker.disable(this)
            refresh()
            toast("Автономные циклы остановлены.")
        })
        root.addView(button("Обновить статус") { refresh() })
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        val prefs = getSharedPreferences(ManaAutonomyWorker.PREFS, MODE_PRIVATE)
        val enabled = prefs.getBoolean(ManaAutonomyWorker.KEY_ENABLED, false)
        val last = prefs.getLong(ManaAutonomyWorker.KEY_LAST_RUN, 0L)
        val date = if (last > 0L) java.text.DateFormat.getDateTimeInstance().format(java.util.Date(last)) else "ещё не запускался"
        status.text = "Статус: ${if (enabled) "включено" else "выключено"}\nПоследняя попытка: $date\n${prefs.getString(ManaAutonomyWorker.KEY_STATUS, "Нет отчёта.").orEmpty()}"
    }

    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun toast(value: String) = android.widget.Toast.makeText(this, value, android.widget.Toast.LENGTH_LONG).show()
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
