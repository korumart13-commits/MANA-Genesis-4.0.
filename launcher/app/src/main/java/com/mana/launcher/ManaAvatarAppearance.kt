package com.mana.launcher

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared observable avatar state used by the launcher, preview and floating SceneView. */
object ManaAvatarAppearance {
    var outfitIndex by mutableIntStateOf(0)
    var accessoryEnabled by mutableStateOf(true)
    var expressionIndex by mutableIntStateOf(0)
    var isSpeaking by mutableStateOf(false)

    fun assetName(): String {
        val base = when (outfitIndex.mod(3)) {
            1 -> "mana_avatar_genesis_casual"
            2 -> "mana_avatar_genesis_evening"
            else -> "mana_avatar_genesis"
        } + if (accessoryEnabled) "" else "_no_accessory"
        val expression = when {
            isSpeaking -> "speaking"
            expressionIndex.mod(3) == 1 -> "playful"
            expressionIndex.mod(3) == 2 -> "speaking"
            else -> null
        }
        return if (expression == null) "$base.gltf" else "${base}_$expression.gltf"
    }
}
