package com.mana.launcher

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.max

class MainActivity : Activity() {
    private val bg = Color.rgb(4, 7, 14)
    private val panel = Color.rgb(10, 22, 42)
    private val panelRaised = Color.rgb(13, 32, 59)
    private val accent = Color.rgb(52, 163, 255)
    private val accentSoft = Color.rgb(105, 198, 255)
    private val textColor = Color.rgb(238, 246, 255)
    private val muted = Color.rgb(139, 163, 190)

    private lateinit var rootFrame: FrameLayout
    private lateinit var page: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var search: EditText
    private lateinit var avatarButton: AvatarView
    private var avatarPanel: View? = null
    private var showingDrawer = false
    private var apps: List<ResolveInfo> = emptyList()
    private val voiceRequestCode = 4107

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.setBackgroundColor(bg)
        loadApps()
        buildShell()
        showHome()
    }

    override fun onResume() {
        super.onResume()
        loadApps()
        if (::content.isInitialized && !showingDrawer) showHome()
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase(Locale.getDefault()) }
    }

    private fun buildShell() {
        rootFrame = FrameLayout(this).apply { setBackgroundColor(bg) }
        page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(26), dp(20), dp(12))
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, dp(18))
        }
        header.addView(label("MANA", 35, textColor, true))
        header.addView(label("GENESIS 4.0  •  ТВОЁ ПРОСТРАНСТВО", 10, accentSoft, true))
        page.addView(header)

        search = EditText(this).apply {
            hint = "Поиск приложений"
            setHintTextColor(muted)
            setTextColor(textColor)
            textSize = 15f
            singleLine = true
            setPadding(dp(17), 0, dp(17), 0)
            background = rounded(panelRaised, 19)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (showingDrawer) renderApps(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        page.addView(search, LinearLayout.LayoutParams(-1, dp(52)))

        val scroll = ScrollView(this).apply { clipToPadding = false }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(17), 0, dp(18))
        }
        scroll.addView(content)
        page.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(2))
            background = rounded(panel, 18)
        }
        nav.addView(action("Главная") { showHome() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        nav.addView(action("Приложения") { showDrawer() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        page.addView(nav, LinearLayout.LayoutParams(-1, dp(50)))

        rootFrame.addView(page, FrameLayout.LayoutParams(-1, -1))
        avatarButton = AvatarView(this).apply {
            contentDescription = "Открыть аватар MANA"
            elevation = dp(14).toFloat()
            setOnClickListener { toggleAvatarPanel() }
            setOnTouchListener(AvatarDragListener())
        }
        rootFrame.addView(avatarButton, FrameLayout.LayoutParams(dp(66), dp(66), Gravity.END or Gravity.BOTTOM).apply {
            rightMargin = dp(18)
            bottomMargin = dp(76)
        })
        setContentView(rootFrame)
    }

    private fun showHome() {
        showingDrawer = false
        search.setText("")
        content.removeAllViews()
        content.addView(label("Добро пожаловать", 24, textColor, true))
        content.addView(label("Я рядом. Нажми на мой аватар, чтобы открыть MANA.", 13, muted, false))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(panelRaised, 22)
        }
        card.addView(label("MANA ASSISTANT", 11, accentSoft, true))
        card.addView(label("Твой телефон. Твои правила.", 19, textColor, true))
        card.addView(label("Локальный режим предусмотрен; подключение модели проверяется отдельно.", 12, muted, false))
        val p = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(19) }
        content.addView(card, p)

        content.addView(label("Избранное", 18, textColor, true).apply {
            setPadding(0, dp(23), 0, dp(9))
        })
        val favorites = apps.filter {
            val n = it.loadLabel(packageManager).toString().lowercase(Locale.getDefault())
            listOf("phone", "camera", "settings", "chrome", "messages", "телефон", "камера", "настройки", "сообщ").any(n::contains)
        }.take(8)
        renderGrid(favorites)
        content.addView(label("Полный список приложений — во вкладке «Приложения».", 12, muted, false).apply {
            setPadding(0, dp(17), 0, 0)
        })
    }

    private fun showDrawer() {
        showingDrawer = true
        search.setText("")
        renderApps("")
    }

    private fun renderApps(filter: String) {
        content.removeAllViews()
        content.addView(label("Все приложения", 23, textColor, true))
        content.addView(label("${apps.size} приложений", 12, muted, false).apply {
            setPadding(0, 0, 0, dp(12))
        })
        val filtered = apps.filter { it.loadLabel(packageManager).toString().contains(filter, true) }
        renderGrid(filtered)
        if (filtered.isEmpty()) content.addView(label("Ничего не найдено", 14, muted, false))
    }

    private fun renderGrid(items: List<ResolveInfo>) {
        val grid = GridLayout(this).apply { columnCount = 4; useDefaultMargins = false }
        items.forEach { info ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(8), dp(3), dp(10))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    packageManager.getLaunchIntentForPackage(info.activityInfo.packageName)?.let(::startActivity)
                }
            }
            val iconFrame = FrameLayout(this).apply {
                background = rounded(panelRaised, 17)
                setPadding(dp(9), dp(9), dp(9), dp(9))
            }
            iconFrame.addView(ImageView(this).apply {
                setImageDrawable(info.loadIcon(packageManager))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, FrameLayout.LayoutParams(-1, -1))
            item.addView(iconFrame, LinearLayout.LayoutParams(dp(56), dp(56)))
            item.addView(TextView(this).apply {
                text = info.loadLabel(packageManager)
                setTextColor(textColor)
                textSize = 10.5f
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(6), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
            grid.addView(item, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(100)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, -2))
    }

    private fun toggleAvatarPanel() {
        avatarPanel?.let {
            rootFrame.removeView(it)
            avatarPanel = null
            return
        }
        val overlay = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(16))
            background = rounded(Color.rgb(8, 18, 35), 24)
            elevation = dp(22).toFloat()
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(AvatarView(this), LinearLayout.LayoutParams(dp(52), dp(52)))
        val titleBlock = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        titleBlock.addView(label("MANA", 19, textColor, true))
        titleBlock.addView(label("Аватар • Genesis 4.0", 11, accentSoft, false))
        top.addView(titleBlock, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(action("×") {
            avatarPanel?.let(rootFrame::removeView)
            avatarPanel = null
        })
        overlay.addView(top)

        overlay.addView(label("Я здесь. Мой визуальный образ загружен из встроенного интерфейса.", 13, textColor, false).apply {
            setPadding(0, dp(14), 0, dp(8))
        })
        overlay.addView(label("Состояние ИИ: локальная модель ещё не подключена. Я не буду имитировать ответ движка.", 11, muted, false))
        val controls = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        controls.addView(action("Голос") { startVoiceInput() }, LinearLayout.LayoutParams(0, dp(42), 1f))
        controls.addView(action("Закрыть") {
            avatarPanel?.let(rootFrame::removeView)
            avatarPanel = null
        }, LinearLayout.LayoutParams(0, dp(42), 1f))
        overlay.addView(controls)
        val lp = FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            leftMargin = dp(14)
            rightMargin = dp(14)
            bottomMargin = dp(151)
        }
        rootFrame.addView(overlay, lp)
        avatarPanel = overlay
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите что-нибудь для MANA")
        }
        try {
            startActivityForResult(intent, voiceRequestCode)
        } catch (_: ActivityNotFoundException) {
            showMessage("На устройстве не найден сервис распознавания речи.")
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == voiceRequestCode && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) showMessage("Распознано: $spoken\nЛокальный ИИ пока не подключён, поэтому запрос ещё не обработан.")
        }
    }

    private fun showMessage(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private fun action(title: String, run: () -> Unit): TextView =
        TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentSoft)
            setOnClickListener { run() }
        }

    private fun label(value: String, size: Int, color: Int, bold: Boolean) =
        TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        setStroke(dp(1), Color.rgb(22, 57, 94))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class AvatarDragListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0f
        private var startY = 0f
        private var dragged = false

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            val lp = view.layoutParams as FrameLayout.LayoutParams
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startX = lp.rightMargin.toFloat()
                    startY = lp.bottomMargin.toFloat()
                    dragged = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (kotlin.math.abs(dx) + kotlin.math.abs(dy) > dp(7)) dragged = true
                    if (dragged) {
                        lp.gravity = Gravity.END or Gravity.BOTTOM
                        lp.rightMargin = max(0, (startX - dx).toInt())
                        lp.bottomMargin = max(dp(60), (startY - dy).toInt())
                        view.layoutParams = lp
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragged) toggleAvatarPanel()
                    view.performClick()
                    return true
                }
            }
            return true
        }
    }

    private inner class AvatarView(context: android.content.Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val bounds = RectF()
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val side = minOf(width, height).toFloat()
            val cx = width / 2f
            val cy = height / 2f
            val r = side * 0.47f
            paint.shader = android.graphics.RadialGradient(
                cx, cy, r,
                intArrayOf(Color.rgb(65, 179, 255), Color.rgb(15, 62, 117), Color.rgb(5, 13, 27)),
                floatArrayOf(0f, 0.72f, 1f),
                android.graphics.Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, paint)
            paint.shader = null
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = side * 0.035f
            paint.color = accentSoft
            canvas.drawCircle(cx, cy, r * 0.96f, paint)
            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(10, 24, 44)
            bounds.set(cx - r * .46f, cy - r * .48f, cx + r * .46f, cy + r * .53f)
            canvas.drawOval(bounds, paint)
            paint.color = Color.rgb(24, 48, 75)
            bounds.set(cx - r * .50f, cy - r * .50f, cx + r * .50f, cy + r * .20f)
            canvas.drawOval(bounds, paint)
            paint.color = Color.rgb(225, 238, 250)
            canvas.drawCircle(cx - r * .16f, cy - r * .04f, r * .035f, paint)
            canvas.drawCircle(cx + r * .16f, cy - r * .04f, r * .035f, paint)
            paint.color = accentSoft
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            paint.textSize = side * .17f
            canvas.drawText("M", cx, cy + r * .82f, paint)
        }
    }
}
