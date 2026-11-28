package com.mana.launcher

import android.app.Activity
import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Offline wallpaper studio. Artwork is generated locally from original vector-like
 * shapes; it does not pretend to run a diffusion model or capture the live 3D scene.
 */
class ManaWallpaperStudioActivity : Activity() {
    private val background = 0xFF071426.toInt()
    private val foreground = 0xFFEAF4FF.toInt()
    private val accent = 0xFF54C8F5.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 28, 24, 24)
            setBackgroundColor(background)
        }
        root.addView(TextView(this).apply {
            text = "MANA • ЖИВЫЕ ОБОИ"
            textSize = 23f
            setTextColor(accent)
        })
        root.addView(TextView(this).apply {
            text = "Мана создаёт новые оригинальные изображения офлайн и может менять их по расписанию. Это локальная иллюстрация, не снимок 3D-модели и не генерация нейросетью."
            textSize = 14f
            setTextColor(foreground)
            setPadding(0, 12, 0, 12)
        })
        val ram = ManaResourceGovernor.snapshot(this)
        root.addView(TextView(this).apply {
            text = "ОПЕРАТИВНАЯ ПАМЯТЬ\\nВсего: ${ram.totalRamMb} МБ • доступно: ${ram.availableRamMb} МБ\\nПамять приложения: ${ram.appPssMb} МБ\\nПрофиль: ${ram.recommendedProfile}"
            textSize = 13f
            setTextColor(0xFF9EDCFF.toInt())
            setPadding(0, 0, 0, 18)
        })
        root.addView(button("Создать и поставить на главный экран") { applyWallpaper(WallpaperManager.FLAG_SYSTEM) })
        root.addView(button("Создать и поставить на экран блокировки") { applyWallpaper(WallpaperManager.FLAG_LOCK) })
        root.addView(button("Поставить на оба экрана") {
            applyWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
        })
        root.addView(button(if (isRotationEnabled()) "Выключить ежедневную смену" else "Включить ежедневную смену") {
            toggleRotation()
            recreate()
        })
        root.addView(TextView(this).apply {
            text = "Автосмена: один раз в сутки (Android может отложить фоновую задачу ради батареи). Работает без сети. Для полноценной генерации фотореалистичных 3D-кадров понадобится отдельная локальная модель и интеграция Godot."
            textSize = 12f
            setTextColor(0xFF9BB2CC.toInt())
            setPadding(0, 18, 0, 0)
        })
        setContentView(root)
    }

    private fun button(title: String, click: () -> Unit) = Button(this).apply {
        text = title
        setOnClickListener { click() }
    }

    private fun isRotationEnabled(): Boolean =
        getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_ROTATION, false)

    private fun toggleRotation() {
        val work = WorkManager.getInstance(this)
        if (isRotationEnabled()) {
            work.cancelUniqueWork(ManaWallpaperWorker.UNIQUE_WORK)
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ROTATION, false).apply()
            status("Ежедневная смена обоев выключена.")
        } else {
            val request = PeriodicWorkRequestBuilder<ManaWallpaperWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(24, TimeUnit.HOURS)
                .build()
            work.enqueueUniquePeriodicWork(ManaWallpaperWorker.UNIQUE_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ROTATION, true).apply()
            status("Ежедневная смена включена. Первое обновление запланировано примерно через сутки.")
        }
    }

    private fun applyWallpaper(flags: Int) {
        try {
            val bitmap = ManaOfflineWallpaperRenderer.render(
                resources.displayMetrics.widthPixels.coerceAtLeast(720),
                resources.displayMetrics.heightPixels.coerceAtLeast(1280),
                System.currentTimeMillis().toInt()
            )
            WallpaperManager.getInstance(this).setBitmap(bitmap, null, true, flags)
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt("target_flags", flags).apply()
            bitmap.recycle()
            status("Обои Маны установлены.")
        } catch (error: Exception) {
            status("Не удалось установить обои: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun status(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    companion object {
        const val PREFS = "mana.wallpaper"
        const val KEY_ROTATION = "daily_rotation"
    }
}

internal object ManaOfflineWallpaperRenderer {
    fun render(width: Int, height: Int, seed: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val random = Random(seed)
        val palettes = arrayOf(
            intArrayOf(0xFF071426.toInt(), 0xFF172D55.toInt(), 0xFF392B62.toInt()),
            intArrayOf(0xFF061A2B.toInt(), 0xFF123C5A.toInt(), 0xFF302C5B.toInt()),
            intArrayOf(0xFF101426.toInt(), 0xFF2A315B.toInt(), 0xFF4A315A.toInt())
        )
        val palette = palettes[random.nextInt(palettes.size)]
        paint.shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), palette, null, Shader.TileMode.CLAMP)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Soft atmospheric lights and stars.
        repeat(34) {
            paint.color = Color.argb(random.nextInt(35, 115), 120, 190, 255)
            val radius = random.nextFloat() * width * 0.012f + 2f
            val x = random.nextFloat() * width
            val y = random.nextFloat() * height * 0.65f
            canvas.drawCircle(x, y, radius, paint)
        }
        paint.color = 0x332D8BCB
        canvas.drawCircle(width * 0.78f, height * 0.22f, width * 0.28f, paint)

        val cx = width * 0.5f
        val headY = height * 0.43f
        val headW = width * 0.245f
        val headH = height * 0.19f

        // Original long-haired anime-inspired MANA portrait, built locally.
        paint.color = 0xFF090E1B.toInt()
        val hair = Path().apply {
            moveTo(cx - headW * 0.82f, headY - headH * 0.35f)
            cubicTo(cx - headW * 1.15f, headY - headH * 1.15f, cx + headW * 0.95f, headY - headH * 1.25f, cx + headW * 0.83f, headY - headH * 0.28f)
            cubicTo(cx + headW * 1.2f, headY + headH * 0.9f, cx + headW * 1.08f, headY + headH * 2.8f, cx + headW * 0.62f, headY + headH * 3.2f)
            lineTo(cx - headW * 0.75f, headY + headH * 3.1f)
            cubicTo(cx - headW * 1.18f, headY + headH * 1.6f, cx - headW * 1.16f, headY + headH * 0.35f, cx - headW * 0.82f, headY - headH * 0.35f)
            close()
        }
        canvas.drawPath(hair, paint)

        // Shoulders and dark outfit.
        paint.color = 0xFF101A2D.toInt()
        canvas.drawRoundRect(cx - headW * 1.65f, headY + headH * 1.65f, cx + headW * 1.65f, height * 0.91f, headW * 0.55f, headW * 0.55f, paint)
        paint.color = 0xFF253E69.toInt()
        canvas.drawRoundRect(cx - headW * 0.12f, headY + headH * 1.7f, cx + headW * 0.12f, height * 0.88f, 12f, 12f, paint)

        // Face.
        paint.color = 0xFFF0D7D5.toInt()
        canvas.drawOval(cx - headW * 0.72f, headY - headH * 0.48f, cx + headW * 0.72f, headY + headH * 1.15f, paint)
        // Bangs.
        paint.color = 0xFF080D19.toInt()
        val bangs = Path().apply {
            moveTo(cx - headW * 0.78f, headY - headH * 0.18f)
            cubicTo(cx - headW * 0.3f, headY - headH * 1.1f, cx + headW * 0.6f, headY - headH * 0.85f, cx + headW * 0.78f, headY - headH * 0.08f)
            lineTo(cx + headW * 0.34f, headY + headH * 0.34f)
            lineTo(cx + headW * 0.12f, headY - headH * 0.04f)
            lineTo(cx - headW * 0.18f, headY + headH * 0.35f)
            lineTo(cx - headW * 0.39f, headY + headH * 0.05f)
            close()
        }
        canvas.drawPath(bangs, paint)

        // Blue-violet eyes and small highlights.
        paint.color = 0xFF6258C8.toInt()
        canvas.drawOval(cx - headW * 0.49f, headY + headH * 0.35f, cx - headW * 0.13f, headY + headH * 0.64f, paint)
        canvas.drawOval(cx + headW * 0.13f, headY + headH * 0.35f, cx + headW * 0.49f, headY + headH * 0.64f, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx - headW * 0.28f, headY + headH * 0.43f, headW * 0.045f, paint)
        canvas.drawCircle(cx + headW * 0.34f, headY + headH * 0.43f, headW * 0.045f, paint)
        paint.color = 0xFFB76D83.toInt()
        paint.strokeWidth = (width * 0.0025f).coerceAtLeast(2f)
        paint.style = Paint.Style.STROKE
        canvas.drawArc(cx - headW * 0.18f, headY + headH * 0.72f, cx + headW * 0.18f, headY + headH * 0.91f, 15f, 150f, false, paint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF54C8F5.toInt()
        canvas.drawCircle(cx + headW * 0.92f, headY - headH * 0.2f, width * 0.018f, paint)
        return bitmap
    }
}
