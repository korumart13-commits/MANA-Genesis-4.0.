package com.mana.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/** Owner-controlled wallpaper automation using on-device generated artwork only. */
class ManaWallpaperActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("mana.wallpapers", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(android.graphics.Color.rgb(7, 20, 38))
        }
        root.addView(TextView(this).apply {
            text = "MANA · ЖИВЫЕ ОБОИ"
            textSize = 23f
            setTextColor(android.graphics.Color.rgb(234, 244, 255))
        })
        root.addView(TextView(this).apply {
            text = "MANA создаёт абстрактные тёмно-синие обои прямо на телефоне. Автосмена включается только по твоему разрешению."
            textSize = 14f
            setTextColor(android.graphics.Color.rgb(145, 169, 197))
            setPadding(0, 20, 0, 24)
        })
        root.addView(Button(this).apply {
            text = "Сменить обои сейчас"
            setOnClickListener {
                runCatching { ManaWallpaperWorker.applyNext(this@ManaWallpaperActivity) }
                    .onSuccess { status("Обои MANA установлены.") }
                    .onFailure { status("Не удалось установить обои: ${it.message}") }
            }
        })
        root.addView(Button(this).apply {
            text = if (prefs.getBoolean("auto", false)) "Выключить автосмену" else "Включить автосмену раз в сутки"
            setOnClickListener {
                val enable = !prefs.getBoolean("auto", false)
                prefs.edit().putBoolean("auto", enable).apply()
                val work = WorkManager.getInstance(this@ManaWallpaperActivity)
                if (enable) {
                    val request = PeriodicWorkRequestBuilder<ManaWallpaperWorker>(24, TimeUnit.HOURS).build()
                    work.enqueueUniquePeriodicWork("mana-wallpaper-daily", ExistingPeriodicWorkPolicy.UPDATE, request)
                } else {
                    work.cancelUniqueWork("mana-wallpaper-daily")
                }
                recreate()
            }
        })
        root.addView(Button(this).apply {
            text = "Применять к: ${prefs.getString("target", "both")}"
            setOnClickListener {
                val options = arrayOf("Главный экран", "Экран блокировки", "Оба экрана")
                android.app.AlertDialog.Builder(this@ManaWallpaperActivity)
                    .setTitle("Куда устанавливать обои?")
                    .setItems(options) { _, index ->
                        prefs.edit().putString("target", listOf("home", "lock", "both")[index]).apply()
                        recreate()
                    }.show()
            }
        })
        root.addView(TextView(this).apply {
            text = if (prefs.getBoolean("auto", false)) "Автоматическая смена включена." else "Автоматическая смена выключена."
            textSize = 12f
            setTextColor(android.graphics.Color.rgb(84, 200, 245))
            setPadding(0, 18, 0, 12)
        })
        root.addView(Button(this).apply { text = "Назад"; setOnClickListener { finish() } })
        setContentView(root)
    }

    private fun status(message: String) {
        android.app.AlertDialog.Builder(this).setMessage(message).setPositiveButton("ОК", null).show()
    }
}

class ManaWallpaperWorker(context: android.content.Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result = runCatching { applyNext(applicationContext); Result.success() }
        .getOrElse { Result.retry() }

    companion object {
        fun applyNext(context: android.content.Context) {
            val prefs = context.getSharedPreferences("mana.wallpapers", android.content.Context.MODE_PRIVATE)
            val index = ((System.currentTimeMillis() / 86_400_000L) % 6).toInt()
            val palettes = listOf(
                intArrayOf(0xFF071426.toInt(), 0xFF1765A8.toInt()),
                intArrayOf(0xFF061A2C.toInt(), 0xFF164B70.toInt()),
                intArrayOf(0xFF101A38.toInt(), 0xFF315B91.toInt()),
                intArrayOf(0xFF071B30.toInt(), 0xFF176B83.toInt()),
                intArrayOf(0xFF0B1730.toInt(), 0xFF293F78.toInt()),
                intArrayOf(0xFF081B27.toInt(), 0xFF245A78.toInt())
            )
            val colors = palettes[index]
            val bitmap = Bitmap.createBitmap(1080, 2400, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.shader = LinearGradient(0f, 0f, 1080f, 2400f, colors[0], colors[1], Shader.TileMode.CLAMP)
            canvas.drawRect(0f, 0f, 1080f, 2400f, paint)
            paint.shader = null
            paint.color = 0x2254C8F5
            canvas.drawCircle(810f, 620f, 390f, paint)
            paint.color = 0x1AFFFFFF
            canvas.drawCircle(180f, 1760f, 520f, paint)
            val manager = WallpaperManager.getInstance(context)
            val target = prefs.getString("target", "both") ?: "both"
            if (Build.VERSION.SDK_INT >= 24) {
                val which = when (target) {
                    "home" -> WallpaperManager.FLAG_SYSTEM
                    "lock" -> WallpaperManager.FLAG_LOCK
                    else -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                manager.setBitmap(bitmap, null, true, which)
            } else {
                manager.setBitmap(bitmap)
            }
            bitmap.recycle()
        }
    }
}
