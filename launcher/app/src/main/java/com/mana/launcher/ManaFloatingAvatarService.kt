package com.mana.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewTreeLifecycleOwner
import io.github.sceneview.SceneView
import io.github.sceneview.SurfaceType
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

/**
 * Persistent, owner-enabled floating glTF avatar. The model is rendered by SceneView/Filament;
 * the overlay never captures keyboard focus or injects input into the foreground app.
 */
class ManaFloatingAvatarService : LifecycleService() {
    private lateinit var windowManager: WindowManager
    private var avatarView: ComposeView? = null
    private lateinit var params: WindowManager.LayoutParams
    private var dragged = false

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
            dp(132), dp(236), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(120)
        }
        val avatarAsset = ManaAvatarAssetSelector.assetName(this)
        val compose = ComposeView(this).apply {
            ViewTreeLifecycleOwner.set(this, this@ManaFloatingAvatarService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val engine = rememberEngine()
                val modelLoader = rememberModelLoader(engine)
                val isSpeaking = ManaAvatarState.isSpeaking
                val expression = ManaAvatarState.expressionIndex
                val animationName = when { isSpeaking -> "Speaking"; expression == 1 -> "Happy"; expression == 2 -> "Playful"; else -> "Idle" }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color.Transparent)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { dragged = false },
                                onDragEnd = { if (dragged) snapToEdge() },
                                onDragCancel = { if (dragged) snapToEdge() },
                                onDrag = { change, amount ->
                                    dragged = true
                                    change.consume()
                                    params.x = (params.x + amount.x.toInt()).coerceAtLeast(0)
                                    params.y = (params.y + amount.y.toInt()).coerceAtLeast(0)
                                    try { avatarView?.let { windowManager.updateViewLayout(it, params) } }
                                    catch (_: IllegalArgumentException) { }
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { openAssistant() })
                        }
                ) {
                    SceneView(
                        modifier = Modifier.fillMaxSize(),
                        surfaceType = SurfaceType.TextureSurface,
                        engine = engine,
                        modelLoader = modelLoader,
                        isOpaque = false
                    ) {
                        rememberModelInstance(modelLoader, avatarAsset)?.let { instance ->
                            ModelNode(
                                modelInstance = instance,
                                scaleToUnits = 2.25f,
                                autoAnimate = false,
                                animationName = animationName,
                                animationLoop = true
                            )
                        }
                    }
                }
            }
        }
        avatarView = compose
        try {
            windowManager.addView(compose, params)
        } catch (_: SecurityException) {
            avatarView = null
            stopSelf()
        }
    }

    private fun snapToEdge() {
        val metrics = resources.displayMetrics
        val maxX = (metrics.widthPixels - params.width).coerceAtLeast(0)
        val maxY = (metrics.heightPixels - params.height).coerceAtLeast(0)
        params.x = if (params.x + params.width / 2 < metrics.widthPixels / 2) 0 else maxX
        params.y = params.y.coerceIn(0, maxY)
        try { avatarView?.let { windowManager.updateViewLayout(it, params) } }
        catch (_: IllegalArgumentException) { }
    }

    private fun openAssistant() {
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            getSharedPreferences("mana.settings", MODE_PRIVATE).edit()
                .putBoolean("floating_avatar_enabled", false).apply()
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
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, CHANNEL_ID)
            else @Suppress("DEPRECATION") Notification.Builder(this)
        val notification = builder.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("MANA is active")
            .setContentText("Плавающий анимированный 3D-аватар MANA")
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Остановить", stopPending)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onDestroy() {
        avatarView?.let {
            try {
                windowManager.removeView(it)
                it.disposeComposition()
            } catch (_: IllegalArgumentException) { }
        }
        avatarView = null
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "mana_floating_avatar"
        private const val NOTIFICATION_ID = 4109
        const val ACTION_STOP = "com.mana.launcher.action.STOP_FLOATING_AVATAR"
    }
}
