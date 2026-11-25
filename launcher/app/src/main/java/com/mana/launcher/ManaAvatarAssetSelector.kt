package com.mana.launcher

import android.content.Context

/** Selects visual wardrobe variants while preserving the same MANA character identity. */
object ManaAvatarAssetSelector {
    fun assetName(context: Context): String {
        val prefs = context.getSharedPreferences("mana.settings", Context.MODE_PRIVATE)
        return assetName(
            prefs.getInt("avatar_outfit_index", 0),
            prefs.getBoolean("avatar_accessory_enabled", true)
        )
    }

    fun assetName(outfitIndex: Int, accessoryEnabled: Boolean): String {
        val base = when (if (outfitIndex in 0..2) outfitIndex else 0) {
            1 -> "mana_avatar_genesis_casual.gltf"
            2 -> "mana_avatar_genesis_evening.gltf"
            else -> "mana_avatar_genesis.gltf"
        }
        return if (accessoryEnabled) base else base.removeSuffix(".gltf") + "_no_accessory.gltf"
    }
}
