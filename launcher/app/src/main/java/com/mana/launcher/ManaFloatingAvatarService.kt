package com.mana.launcher

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
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
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(
            dp(112), dp(156), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(12)
            y = dp(150)
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
            }
        ).also { view ->
            try { windowManager.addView(view, params) } catch (_: SecurityException) { stopSelf() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
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
}
