package com.mana.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val bg = Color.rgb(7, 20, 38)
    private val panel = Color.rgb(16, 43, 80)
    private val accent = Color.rgb(84, 200, 245)
    private val textColor = Color.rgb(234, 244, 255)
    private val muted = Color.rgb(145, 169, 197)
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var search: EditText
    private var showingDrawer = false
    private var apps: List<android.content.pm.ResolveInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        loadApps()
        buildShell()
        showHome()
    }

    private fun loadApps() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(14))
            setBackgroundColor(bg)
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(8), 0, dp(18))
        }
        header.addView(label("MANA", 34, textColor, true))
        header.addView(label("ТВОЁ ПРОСТРАНСТВО", 11, accent, true))
        root.addView(header)

        search = EditText(this).apply {
            hint = "Поиск приложений"
            setHintTextColor(muted)
            setTextColor(textColor)
            textSize = 15f
            singleLine = true
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(panel, 24)
            addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (showingDrawer) renderApps(s?.toString().orEmpty())
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        }
        root.addView(search, LinearLayout.LayoutParams(-1, dp(52)))
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(16), 0, dp(12))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val nav = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, 0)
        }
        nav.addView(action("Главная") { showHome() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        nav.addView(action("Все приложения") { showDrawer() }, LinearLayout.LayoutParams(0, dp(46), 1f))
        root.addView(nav)
        setContentView(root)
    }

    private fun showHome() {
        showingDrawer = false
        search.setText("")
        content.removeAllViews()
        content.addView(label("Добро пожаловать", 23, textColor, true))
        content.addView(label("MANA всегда рядом.", 14, muted, false))
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(panel, 22)
        }
        card.addView(label("MANA ASSISTANT", 12, accent, true))
        card.addView(label("Твой телефон. Твои правила.", 18, textColor, true))
        card.addView(label("Локальный интерфейс MANA • Android 16", 12, muted, false))
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(20)
        content.addView(card, params)
        content.addView(label("Избранное", 18, textColor, true).apply {
            setPadding(0, dp(24), 0, dp(10))
        })
        val favorites = apps.filter {
            val n = it.loadLabel(packageManager).toString().lowercase()
            n.contains("phone") || n.contains("camera") || n.contains("settings") ||
                n.contains("chrome") || n.contains("messages") || n.contains("телефон") ||
                n.contains("камера") || n.contains("настройки") || n.contains("сообщ")
        }.take(8)
        renderGrid(favorites)
        content.addView(label("Проведи вверх через «Все приложения», чтобы открыть полный список.", 12, muted, false).apply {
            setPadding(0, dp(20), 0, 0)
        })
    }

    private fun showDrawer() {
        showingDrawer = true
        search.setText("")
        content.removeAllViews()
        content.addView(label("Приложения", 23, textColor, true))
        content.addView(label("${apps.size} приложений на устройстве", 12, muted, false))
        renderApps("")
    }

    private fun renderApps(filter: String) {
        content.removeAllViews()
        content.addView(label("Приложения", 23, textColor, true))
        val filtered = apps.filter { it.loadLabel(packageManager).toString().contains(filter, true) }
        renderGrid(filtered)
        if (filtered.isEmpty()) content.addView(label("Ничего не найдено", 14, muted, false))
    }

    private fun renderGrid(items: List<android.content.pm.ResolveInfo>) {
        val grid = GridLayout(this).apply {
            columnCount = 4
            useDefaultMargins = false
        }
        items.forEach { info ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(3), dp(10), dp(3), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    val launch = packageManager.getLaunchIntentForPackage(info.activityInfo.packageName)
                    if (launch != null) startActivity(launch)
                }
            }
            val icon = TextView(this).apply {
                gravity = Gravity.CENTER
                text = ""
                background = rounded(Color.rgb(24, 58, 98), 18)
                val drawable = info.loadIcon(packageManager)
                setCompoundDrawablesWithIntrinsicBounds(null, drawable, null, null)
                compoundDrawablePadding = dp(2)
            }
            item.addView(icon, LinearLayout.LayoutParams(dp(54), dp(54)))
            item.addView(TextView(this).apply {
                text = info.loadLabel(packageManager)
                setTextColor(textColor)
                textSize = 10f
                gravity = Gravity.CENTER
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding(0, dp(5), 0, 0)
            }, LinearLayout.LayoutParams(-1, -2))
            grid.addView(item, GridLayout.LayoutParams().apply {
                width = 0
                height = dp(96)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
        }
        content.addView(grid, LinearLayout.LayoutParams(-1, -2))
    }

    private fun action(title: String, run: () -> Unit): TextView =
        TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(accent)
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
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (showingDrawer) showHome() else super.onBackPressed()
    }
}
