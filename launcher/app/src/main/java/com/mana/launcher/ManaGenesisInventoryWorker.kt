package com.mana.launcher

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Periodic, offline-only inventory refresh; no network, patching or installation. */
class ManaGenesisInventoryWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        ManaGenesisFirstBootService(applicationContext).refresh()
        Result.success()
    } catch (error: Exception) {
        android.util.Log.e("MANA", "Genesis inventory refresh failed", error)
        if (runAttemptCount < 3) Result.retry() else Result.failure()
    }
}
