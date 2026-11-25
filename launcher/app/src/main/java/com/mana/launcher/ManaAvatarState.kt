package com.mana.launcher

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Process-wide UI state shared by the assistant screen and the floating avatar. */
object ManaAvatarState {
    var isSpeaking by mutableStateOf(false)
    var expressionIndex by mutableIntStateOf(0)
}

object ManaAvatarAssetSelector {
    fun assetName(context: Context): String {
        val prefs = context.getSharedPreferences("mana.settings", Context.MODE_PRIVATE)
        val outfit = prefs.getInt("avatar_outfit_index", 0).coerceIn(0, 2)
        val accessory = prefs.getBoolean("avatar_accessory_enabled", true)
        val base = when (outfit) {
            1 -> "mana_avatar_genesis_casual.gltf"
            2 -> "mana_avatar_genesis_evening.gltf"
            else -> "mana_avatar_genesis.gltf"
        }
        return if (accessory) base else base.removeSuffix(".gltf") + "_no_accessory.gltf"
    }
}
