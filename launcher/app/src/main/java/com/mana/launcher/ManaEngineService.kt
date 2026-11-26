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
 * Keeps the already initialized MANA Engine alive independently of MainActivity.
 * This is an app-level foreground service, not a privileged Android system daemon.
 */
class ManaEngineService : Service() {
    inner class LocalBinder : Binder() {
        fun engineRuntime(): ManaEngineAndroidRuntime? = ManaEngineProcessOwner.engineRuntime()
        fun isReady(): Boolean = ManaEngineProcessOwner.isRunning()
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ManaEngineProcessOwner.isInstalled()) {
            try {
                ManaEngineProcessOwner.initializeFromService(this, ManaAndroidKeyProvider())
            } catch (error: Exception) {
                android.util.Log.e("MANA", "Unable to initialize MANA Engine in service", error)
                stopSelf(startId)
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = localBinder

    override fun onDestroy() {
        ManaEngineProcessOwner.shutdown()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "mana_engine_runtime"
        private const val NOTIFICATION_ID = 4120
    }
}
