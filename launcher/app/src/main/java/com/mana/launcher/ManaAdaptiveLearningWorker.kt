package com.mana.launcher

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
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
            .lineSequence().map { it.trim() }.filter { it.length in 2..512 }.distinct().take(5).toList()
        if (topics.isEmpty()) return Result.success()
        val provider = ManaYandexAiStudioProvider(applicationContext)
        if (!provider.isAvailable()) return Result.failure()
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
            for (topic in topics) {
                val results = search.search(topic, 3)
                results.forEach { result ->
                    knowledge.save(result.title, result.url, result.snippet.ifBlank { result.title }, ownerApproved = true)
                }
            }
            Result.success()
        } catch (error: Exception) {
            val message = error.message.orEmpty()
            if (message.contains("HTTP 401") || message.contains("HTTP 403")) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val PREFS = "mana.adaptive.learning"
        const val KEY_ENABLED = "enabled"
        const val KEY_TOPICS = "topics"
    }
}

object ManaAdaptiveLearningScheduler {
    fun enable(context: Context, topics: List<String>) {
        val normalized = topics.map { it.trim() }.filter { it.length in 2..512 }.distinct().take(5)
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
    }

    fun disable(context: Context) {
        context.getSharedPreferences(ManaAdaptiveLearningWorker.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(ManaAdaptiveLearningWorker.KEY_ENABLED, false).apply()
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(ManaAdaptiveLearningWorker.PREFS, Context.MODE_PRIVATE)
            .getBoolean(ManaAdaptiveLearningWorker.KEY_ENABLED, false)

    private const val UNIQUE_WORK = "mana-adaptive-yandex-search"
}
