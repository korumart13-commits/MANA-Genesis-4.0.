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
        return assetName(
            prefs.getInt("avatar_outfit_index", 0),
            prefs.getBoolean("avatar_accessory_enabled", true)
        )
    }

    fun assetName(outfitIndex: Int, accessoryEnabled: Boolean): String {
        val base = when (outfitIndex) {
            1 -> "mana_avatar_genesis_casual.gltf"
            2 -> "mana_avatar_genesis_evening.gltf"
            else -> "mana_avatar_genesis.gltf"
        }
        return if (accessoryEnabled) base else base.removeSuffix(".gltf") + "_no_accessory.gltf"
    }
}
