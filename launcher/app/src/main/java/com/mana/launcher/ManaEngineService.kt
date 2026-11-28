package com.mana.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Binder

/**
 * Owns MANA Engine initialization and exposes a narrow local Binder API to app clients.
 * This is an app-level foreground service, not a privileged Android system daemon.
 */
class ManaEngineService : Service() {
    inner class LocalBinder : Binder() {
        fun isInitialized(): Boolean = ManaEngineProcessOwner.isPrepared() && ManaEngineProcessOwner.isInstalled()
        fun isReady(): Boolean = ManaEngineProcessOwner.isRunning()
        fun localModelRuntime(): LocalModelRuntime = checkNotNull(ManaEngineProcessOwner.localModelRuntime())
        fun memoryService(): com.mana.os.core.ManaMemoryService? = ManaEngineProcessOwner.memoryService()
        fun cloudGateway(): ManaCloudGateway = checkNotNull(ManaEngineProcessOwner.cloudGateway())
        fun yandexProvider(): ManaYandexAiStudioProvider = checkNotNull(ManaEngineProcessOwner.yandexProvider())
        suspend fun think(
            text: String,
            allowCloud: Boolean = false,
            sensitive: Boolean = false,
            preferredCloudProvider: String? = null,
            includePersonalMemory: Boolean = true,
            memoryQuery: String = text
        ) = checkNotNull(ManaEngineProcessOwner.engineRuntime()) { "MANA Engine is not initialized" }.think(
            text, allowCloud, sensitive, preferredCloudProvider, includePersonalMemory, memoryQuery
        )
    }

    private val localBinder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MANA Engine", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val openPending = PendingIntent.getActivity(
            this,
            4120,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        })
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MANA Engine активен")
            .setContentText("Центральный движок работает в фоновой службе приложения")
            .setContentIntent(openPending)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        try {
            ManaEngineProcessOwner.acquire(this, ManaAndroidKeyProvider())
            startGenesisBootstrapJobs()
        } catch (error: Exception) {
            android.util.Log.e("MANA", "Unable to initialize MANA Engine in service", error)
            stopSelf()
        }
    }

    /**
     * Connects the already-implemented first-boot inventory and discovery workers
     * to the real Engine startup path. Checkpoint failure must not prevent scheduling.
     */
    private fun startGenesisBootstrapJobs() {
        ManaEngineProcessOwner.ensureSavedModelLoaded(this) { result ->
            result.onSuccess { modelName ->
                android.util.Log.i("MANA-Model", "Previously verified local model restored: $modelName")
            }.onFailure { error ->
                android.util.Log.w("MANA-Model", "Saved local model could not be restored", error)
            }
        }

        runCatching {
            val plan = ManaGenesisFirstBootService(this).refresh()
            android.util.Log.i(
                "MANA-Genesis",
                "First-boot checkpoint saved: ${plan.orderedWork.size} queued, ${plan.blockedWork.size} blocked."
            )
        }.onFailure { error ->
            android.util.Log.w("MANA-Genesis", "First-boot checkpoint could not be saved", error)
        }

        runCatching {
            ManaGenesisWorkScheduler.ensureScheduled(this)
        }.onFailure { error ->
            android.util.Log.w("MANA-Genesis", "Background inventory/source discovery could not be scheduled", error)
        }
    }

    override fun onTrimMemory(level: Int) {
        ManaEngineProcessOwner.localModelRuntime()?.onTrimMemory(level)
        super.onTrimMemory(level)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onBind(intent: Intent?): IBinder = localBinder

    override fun onDestroy() {
        ManaEngineProcessOwner.release()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "mana_engine_runtime"
        private const val NOTIFICATION_ID = 4120
    }
}
