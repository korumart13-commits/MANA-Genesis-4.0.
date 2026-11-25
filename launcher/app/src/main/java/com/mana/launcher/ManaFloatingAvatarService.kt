package com.mana.launcher

import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/**
 * User-enabled, draggable overlay bubble. It is intentionally non-focusable:
 * it never captures text or gestures intended for the foreground app.
 */
class ManaFloatingAvatarService : Service() {
    private lateinit var windowManager: WindowManager
    private var avatar: AvatarBubble? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                   else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(
            dp(64), dp(64), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START; x = dp(18); y = dp(180) }
        avatar = AvatarBubble().also { bubble ->
            try { windowManager.addView(bubble, params) } catch (_: SecurityException) { stopSelf() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        avatar?.let { try { windowManager.removeView(it) } catch (_: IllegalArgumentException) { } }
        avatar = null
        super.onDestroy()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private inner class AvatarBubble : View(this@ManaFloatingAvatarService) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val face = RectF()
        private var downX = 0f
        private var downY = 0f
        private var originX = 0
        private var originY = 0
        private var moved = false

        override fun onDraw(canvas: Canvas) {
            val c = width / 2f
            val r = minOf(width, height) * .46f
            paint.shader = android.graphics.RadialGradient(c, c, r,
                intArrayOf(Color.rgb(81, 190, 255), Color.rgb(15, 55, 105), Color.rgb(4, 12, 27)),
                null, android.graphics.Shader.TileMode.CLAMP)
            canvas.drawCircle(c, c, r, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = dp(2).toFloat()
            paint.color = Color.rgb(122, 211, 255)
            canvas.drawCircle(c, c, r * .96f, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(14, 28, 48)
            face.set(c-r*.42f, c-r*.48f, c+r*.42f, c+r*.48f)
            canvas.drawOval(face, paint)
            paint.color = Color.rgb(28, 55, 83)
            face.set(c-r*.48f, c-r*.56f, c+r*.48f, c+r*.12f)
            canvas.drawOval(face, paint)
            paint.color = Color.rgb(225, 243, 255)
            canvas.drawCircle(c-r*.15f, c-r*.03f, r*.035f, paint)
            canvas.drawCircle(c+r*.15f, c-r*.03f, r*.035f, paint)
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX; downY = event.rawY
                    originX = params.x; originY = params.y; moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX-downX; val dy = event.rawY-downY
                    if (abs(dx)+abs(dy) > dp(6)) moved = true
                    if (moved) {
                        params.x = (originX+dx).toInt().coerceAtLeast(0)
                        params.y = (originY+dy).toInt().coerceAtLeast(0)
                        try { windowManager.updateViewLayout(this, params) } catch (_: IllegalArgumentException) { }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        val open = Intent(this@ManaFloatingAvatarService, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(open)
                    }
                    performClick()
                    return true
                }
            }
            return true
        }

        override fun performClick(): Boolean { super.performClick(); return true }
    }
}
