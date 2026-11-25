package com.mana.launcher

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager

/**
 * Owner-enabled floating 3D avatar. Android still requires the user to grant
 * the special "display over other apps" permission in Settings.
 */
class ManaFloatingAvatarService : Service() {
    private lateinit var windowManager: WindowManager
    private var avatar: ManaAvatar3DView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        startPersistentNotification()
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(
            dp(124), dp(220), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(120)
        }
        avatar = ManaAvatar3DView(
            this,
            onDrag = { dx, dy ->
                params.x = (params.x + dx.toInt()).coerceAtLeast(0)
                params.y = (params.y + dy.toInt()).coerceAtLeast(0)
                try { avatar?.let { windowManager.updateViewLayout(it, params) } } catch (_: IllegalArgumentException) { }
            },
            onTap = {
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            },
            onDragEnd = {
                val metrics = resources.displayMetrics
                val maxX = (metrics.widthPixels - params.width).coerceAtLeast(0)
                val maxY = (metrics.heightPixels - params.height).coerceAtLeast(0)
                params.x = if (params.x + params.width / 2 < metrics.widthPixels / 2) 0 else maxX
                params.y = params.y.coerceIn(0, maxY)
                try { avatar?.let { windowManager.updateViewLayout(it, params) } } catch (_: IllegalArgumentException) { }
            }
        ).also { view ->
            try { windowManager.addView(view, params) } catch (_: SecurityException) { stopSelf() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun startPersistentNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "MANA floating avatar", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val stopIntent = Intent(this, ManaFloatingAvatarService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 4109, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openPending = PendingIntent.getActivity(
            this, 4110,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(this)
        }
        val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MANA is active")
            .setContentText("Плавающий 3D-аватар работает поверх приложений")
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPending)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        avatar?.let {
            try {
                it.onPause()
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) { }
        }
        avatar = null
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "mana_floating_avatar"
        private const val NOTIFICATION_ID = 4109
        const val ACTION_STOP = "com.mana.launcher.action.STOP_FLOATING_AVATAR"
    }
}
