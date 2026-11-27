package com.mana.launcher

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/** Android-managed schedules; actual execution time is controlled by the OS. */
object ManaGenesisWorkScheduler {
    private const val INVENTORY_WORK = "mana.genesis.capability-inventory"
    private const val SOURCE_DISCOVERY_WORK = "mana.genesis.public-source-discovery"

    fun ensureScheduled(context: Context) {
        val manager = WorkManager.getInstance(context.applicationContext)
        val inventory = PeriodicWorkRequestBuilder<ManaGenesisInventoryWorker>(
            12, TimeUnit.HOURS
        ).build()
        manager.enqueueUniquePeriodicWork(
            INVENTORY_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            inventory
        )

        val network = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val discovery = OneTimeWorkRequestBuilder<ManaGenesisSourceDiscoveryWorker>()
            .setConstraints(network)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        manager.enqueueUniqueWork(
            SOURCE_DISCOVERY_WORK,
            ExistingWorkPolicy.KEEP,
            discovery
        )

        val periodicDiscovery = PeriodicWorkRequestBuilder<ManaGenesisSourceDiscoveryWorker>(
            24, TimeUnit.HOURS
        ).setConstraints(network).build()
        manager.enqueueUniquePeriodicWork(
            "$SOURCE_DISCOVERY_WORK.periodic",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicDiscovery
        )
    }
}
