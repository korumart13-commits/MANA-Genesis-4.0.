package com.mana.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Observable appearance shared by the assistant, full-screen preview and floating 3D avatar. */
object ManaAvatarState {
    var outfitIndex by mutableIntStateOf(0)
    var accessoryEnabled by mutableStateOf(true)
    var isSpeaking by mutableStateOf(false)
    var expressionIndex by mutableIntStateOf(0)

    fun loadFromPreferences(context: Context) {
        val prefs = context.getSharedPreferences("mana.settings", Context.MODE_PRIVATE)
        outfitIndex = prefs.getInt("avatar_outfit_index", 0).takeIf { it in 0..2 } ?: 0
        accessoryEnabled = prefs.getBoolean("avatar_accessory_enabled", true)
    }

    fun assetName(): String {
        val base = ManaAvatarAssetSelector.assetName(outfitIndex, accessoryEnabled).removeSuffix(".gltf")
        val expression = when {
            isSpeaking -> "speaking"
            expressionIndex == 1 -> "playful"
            else -> null
        }
        return if (expression == null) "$base.gltf" else "${base}_$expression.gltf"
    }
}

