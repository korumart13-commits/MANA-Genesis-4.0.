package com.mana.launcher

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mana.os.core.EncryptedFileManaAdaptiveKnowledgeStore
import com.mana.os.core.EncryptedFileManaDiaryStore
import com.mana.os.core.ManaAdaptiveKnowledgeService
import java.io.File
import java.util.concurrent.TimeUnit

/** Runs only after explicit owner opt-in; searches only owner-entered topics on unmetered Wi-Fi. */
class ManaAdaptiveLearningWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_ENABLED, false)) return Result.success()
        val topics = prefs.getString(KEY_TOPICS, "").orEmpty()
            .lineSequence().map { it.trim() }.filter { it.length in 2..400 }.distinct().take(5).toList()
        if (topics.isEmpty()) {
            saveStatus(prefs, "Добавь хотя бы одну тему для поиска.", 0)
            return Result.success()
        }
        val provider = ManaYandexAiStudioProvider(applicationContext)
        if (!provider.isAvailable()) {
            saveStatus(prefs, "Yandex AI Studio не настроена или отключена.", 0)
            return Result.failure()
        }
        val search = ManaYandexSearchApi(provider)
        val knowledge = ManaAdaptiveKnowledgeService(
            EncryptedFileManaAdaptiveKnowledgeStore(
                EncryptedFileManaDiaryStore(
                    File(applicationContext.filesDir, "mana/knowledge.bin"),
                    ManaAndroidKeyProvider(),
                    "mana.knowledge.v1"
                )
            )
        )
        return try {
            var saved = 0
            for (topic in topics) {
                if (isStopped || !prefs.getBoolean(KEY_ENABLED, false)) return Result.success()
                val results = search.search(topic, 3)
                if (isStopped || !prefs.getBoolean(KEY_ENABLED, false)) return Result.success()
                results.forEach { result ->
                    if (isStopped || !prefs.getBoolean(KEY_ENABLED, false)) return Result.success()
                    knowledge.save(result.title, result.url, result.snippet.ifBlank { result.title }, ownerApproved = true)
                    saved++
                }
            }
            saveStatus(prefs, "Поиск завершён. Сохранено результатов: $saved.", saved)
            Result.success()
        } catch (error: Exception) {
            val message = error.message.orEmpty()
            val accessDenied = message.contains("HTTP 401") || message.contains("HTTP 403")
            saveStatus(
                prefs,
                if (accessDenied) "Yandex отказал в доступе. Проверь API-ключ, каталог и роль search-api.webSearch.user."
                else "Ошибка сети или Yandex Search API. Повторная попытка будет выполнена автоматически.",
                0
            )
            if (accessDenied) Result.failure() else Result.retry()
        }
    }

    private fun saveStatus(prefs: android.content.SharedPreferences, status: String, saved: Int) {
        prefs.edit()
            .putString(KEY_LAST_STATUS, status)
            .putLong(KEY_LAST_RUN, System.currentTimeMillis())
            .putInt(KEY_LAST_SAVED, saved)
            .apply()
    }

    companion object {
        const val PREFS = "mana.adaptive.learning"
        const val KEY_ENABLED = "enabled"
        const val KEY_TOPICS = "topics"
        const val KEY_LAST_STATUS = "last_status"
        const val KEY_LAST_RUN = "last_run"
        const val KEY_LAST_SAVED = "last_saved"
    }
}

object ManaAdaptiveLearningScheduler {
    fun enable(context: Context, topics: List<String>) {
        val normalized = topics.map { it.trim() }.filter { it.length in 2..400 }.distinct().take(5)
        require(normalized.isNotEmpty()) { "Add at least one learning topic." }
        context.getSharedPreferences(ManaAdaptiveLearningWorker.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ManaAdaptiveLearningWorker.KEY_ENABLED, true)
            .putString(ManaAdaptiveLearningWorker.KEY_TOPICS, normalized.joinToString("\n")).apply()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<ManaAdaptiveLearningWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request
        )
        val initialRun = OneTimeWorkRequestBuilder<ManaAdaptiveLearningWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            INITIAL_WORK, ExistingWorkPolicy.KEEP, initialRun
        )
    }

    fun disable(context: Context) {
        context.getSharedPreferences(ManaAdaptiveLearningWorker.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ManaAdaptiveLearningWorker.KEY_ENABLED, false).apply()
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(INITIAL_WORK)
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(ManaAdaptiveLearningWorker.PREFS, Context.MODE_PRIVATE)
            .getBoolean(ManaAdaptiveLearningWorker.KEY_ENABLED, false)

    private const val UNIQUE_WORK = "mana-adaptive-yandex-search"
    private const val INITIAL_WORK = "mana-adaptive-yandex-search-initial"
}
