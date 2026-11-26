package com.mana.launcher

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Full-screen MANA avatar preview backed by the native OpenGL ES renderer.
 * It does not depend on glTF animation names or asynchronous model loading.
 */
class ManaAvatarPreviewActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(4, 7, 14)
        window.navigationBarColor = Color.rgb(4, 7, 14)
        window.decorView.setBackgroundColor(Color.rgb(7, 20, 38))

        val avatar = ManaAvatar3DView(this, onDrag = { _: Float, _: Float -> Unit }, onTap = { cycleExpression() }, onDragEnd = { Unit })
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(7, 20, 38))
        }
        root.addView(TextView(this).apply {
            text = "MANA • GENESIS 4.0"
            textSize = 18f
            setTextColor(Color.rgb(84, 200, 245))
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }, LinearLayout.LayoutParams(-1, dp(44)))
        root.addView(avatar, LinearLayout.LayoutParams(-1, 0, 1f))
        status = TextView(this).apply {
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(145, 169, 197))
            setPadding(0, dp(6), 0, dp(8))
        }
        root.addView(status, LinearLayout.LayoutParams(-1, dp(34)))

        val outfits = LinearLayout(this).apply { gravity = Gravity.CENTER }
        outfits.addView(control("Одежда") {
            ManaAvatar3DView.outfitIndex = (ManaAvatar3DView.outfitIndex + 1) % 3
            persistWardrobe()
            updateStatus()
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        outfits.addView(control("Аксессуар") {
            ManaAvatar3DView.accessoryEnabled = !ManaAvatar3DView.accessoryEnabled
            persistWardrobe()
            updateStatus()
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(outfits, LinearLayout.LayoutParams(-1, dp(52)))

        val expressions = LinearLayout(this).apply { gravity = Gravity.CENTER }
        expressions.addView(control("Выражение") { cycleExpression() },
            LinearLayout.LayoutParams(0, dp(48), 1f))
        expressions.addView(control("Говорит") {
            ManaAvatar3DView.isSpeaking = !ManaAvatar3DView.isSpeaking
            ManaAvatarState.isSpeaking = ManaAvatar3DView.isSpeaking
            updateStatus()
        }, LinearLayout.LayoutParams(0, dp(48), 1f))
        root.addView(expressions, LinearLayout.LayoutParams(-1, dp(52)))
        root.addView(control("Закрыть") { finish() }, LinearLayout.LayoutParams(-1, dp(48)))
        setContentView(root)
        updateStatus()
    }

    private fun control(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setTextColor(Color.rgb(234, 244, 255))
        setBackgroundColor(Color.rgb(23, 65, 105))
        setOnClickListener { action() }
    }

    private fun persistWardrobe() {
        getSharedPreferences("mana.settings", MODE_PRIVATE).edit()
            .putInt("avatar_outfit_index", ManaAvatar3DView.outfitIndex)
            .putBoolean("avatar_accessory_enabled", ManaAvatar3DView.accessoryEnabled)
            .apply()
    }

    private fun cycleExpression() {
        ManaAvatar3DView.expressionIndex = (ManaAvatar3DView.expressionIndex + 1) % 3
        updateStatus()
    }

    private fun updateStatus() {
        val outfit = when (ManaAvatar3DView.outfitIndex % 3) {
            1 -> "Повседневная"
            2 -> "Вечерняя"
            else -> "Основная"
        }
        val expression = when (ManaAvatar3DView.expressionIndex % 3) {
            1 -> "Радостное"
            2 -> "Игривое"
            else -> "Спокойное"
        }
        val accessory = if (ManaAvatar3DView.accessoryEnabled) "аксессуар включён" else "без аксессуара"
        status.text = "$outfit • $expression • $accessory"
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
