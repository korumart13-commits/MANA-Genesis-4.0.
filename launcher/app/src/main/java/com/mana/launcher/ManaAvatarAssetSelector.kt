package com.mana.launcher

/** Stable selector for the same MANA character identity across wardrobe variants. */
object ManaAvatarAssetSelector {
    fun assetName(outfitIndex: Int, accessoryEnabled: Boolean): String {
        val safeOutfit = if (outfitIndex in 0..2) outfitIndex else 0
        return when (safeOutfit) {
            1 -> if (accessoryEnabled) "mana_avatar_genesis_casual.gltf" else "mana_avatar_genesis_casual_no_accessory.gltf"
            2 -> if (accessoryEnabled) "mana_avatar_genesis_evening.gltf" else "mana_avatar_genesis_evening_no_accessory.gltf"
            else -> if (accessoryEnabled) "mana_avatar_genesis.gltf" else "mana_avatar_genesis_no_accessory.gltf"
        }
    }
}
