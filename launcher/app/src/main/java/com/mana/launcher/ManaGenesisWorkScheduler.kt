package com.mana.launcher

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Schedules a conservative offline inventory refresh; Android controls execution windows. */
object ManaGenesisWorkScheduler {
    private const val INVENTORY_WORK = "mana.genesis.capability-inventory"

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<ManaGenesisInventoryWorker>(
            12, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            INVENTORY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
