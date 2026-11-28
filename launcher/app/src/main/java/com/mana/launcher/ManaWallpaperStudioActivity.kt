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
            text = "MANA • ОБОИ ЭКРАНА БЛОКИРОВКИ"
            textSize = 21f
            setTextColor(accent)
        })
        root.addView(TextView(this).apply {
            text = "Мана сама создаёт новую иллюстрацию и обновляет только экран блокировки. Рабочий стол лаунчера не затрагивается. Генерация работает локально, без интернета."
            textSize = 14f
            setTextColor(foreground)
            setPadding(0, 12, 0, 16)
        })
        val ram = ManaResourceGovernor.snapshot(this)
        root.addView(TextView(this).apply {
            text = "РАСХОД ПАМЯТИ • доступно ${ram.availableRamMb} МБ из ${ram.totalRamMb} МБ • приложение ${ram.appPssMb} МБ"
            textSize = 12f
            setTextColor(0xFF9EDCFF.toInt())
            setPadding(0, 0, 0, 16)
        })
        root.addView(button("Создать новую обложку сейчас") { applyLockWallpaper() })
        root.addView(button(if (isRotationEnabled()) "Остановить автосмену" else "Запустить автосмену каждые 15 минут") {
            toggleRotation()
            recreate()
        })
        root.addView(button("Изменить частоту • сейчас ${rotationMinutes()} мин") {
            val next = when (rotationMinutes()) { 15 -> 30; 30 -> 60; 60 -> 180; else -> 15 }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_INTERVAL, next).apply()
            if (isRotationEnabled()) scheduleRotation()
            android.widget.Toast.makeText(this, "Новая частота: каждые $next минут", android.widget.Toast.LENGTH_SHORT).show()
            recreate()
        })
        root.addView(TextView(this).apply {
            text = "Частота: 15 / 30 / 60 / 180 минут. Android ограничивает точное время фоновой работы и может задерживать обновление в режиме энергосбережения. Иллюстрации создаются процедурным офлайн-генератором MANA; это пока не нейросетевая генерация и не кадр из 3D-сцены."
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

    private fun rotationMinutes(): Long =
        getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_INTERVAL, 15).toLong().coerceIn(15, 180)

    private fun scheduleRotation() {
        val interval = rotationMinutes()
        val request = PeriodicWorkRequestBuilder<ManaWallpaperWorker>(interval, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ManaWallpaperWorker.UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request
        )
    }

    private fun toggleRotation() {
        val work = WorkManager.getInstance(this)
        if (isRotationEnabled()) {
            work.cancelUniqueWork(ManaWallpaperWorker.UNIQUE_WORK)
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_ROTATION, false).apply()
            status("Автосмена обоев блокировки выключена.")
        } else {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(KEY_ROTATION, true)
                .putInt(KEY_TARGET_FLAGS, WallpaperManager.FLAG_LOCK)
                .apply()
            applyLockWallpaper()
            scheduleRotation()
            status("Мана будет создавать новые обои блокировки каждые ${rotationMinutes()} минут (насколько позволит Android).")
        }
    }

    private fun applyLockWallpaper() {
        try {
            val interval = rotationMinutes() * 60_000L
            val bitmap = ManaOfflineWallpaperRenderer.render(
                resources.displayMetrics.widthPixels.coerceAtLeast(720),
                resources.displayMetrics.heightPixels.coerceAtLeast(1280),
                (System.currentTimeMillis() / interval).toInt()
            )
            WallpaperManager.getInstance(this).setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
            bitmap.recycle()
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putInt(KEY_TARGET_FLAGS, WallpaperManager.FLAG_LOCK).apply()
            status("Новая обложка Маны установлена только на экран блокировки.")
        } catch (error: Exception) {
            status("Не удалось обновить экран блокировки: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    private fun status(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    companion object {
        const val PREFS = "mana.wallpaper"
        const val KEY_ROTATION = "lockscreen_rotation"
        const val KEY_INTERVAL = "lockscreen_interval_minutes"
        const val KEY_TARGET_FLAGS = "target_flags"
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
