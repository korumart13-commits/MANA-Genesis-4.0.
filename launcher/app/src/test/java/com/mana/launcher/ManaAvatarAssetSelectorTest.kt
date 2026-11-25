package com.mana.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class ManaAvatarAssetSelectorTest {
    @Test fun outfitSelectionPreservesOneCharacterIdentity() {
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarAssetSelector.assetName(0, true))
        assertEquals("mana_avatar_genesis_casual.gltf", ManaAvatarAssetSelector.assetName(1, true))
        assertEquals("mana_avatar_genesis_evening.gltf", ManaAvatarAssetSelector.assetName(2, true))
    }

    @Test fun accessoryCanBeHiddenForEveryOutfit() {
        assertEquals("mana_avatar_genesis_no_accessory.gltf", ManaAvatarAssetSelector.assetName(0, false))
        assertEquals("mana_avatar_genesis_casual_no_accessory.gltf", ManaAvatarAssetSelector.assetName(1, false))
        assertEquals("mana_avatar_genesis_evening_no_accessory.gltf", ManaAvatarAssetSelector.assetName(2, false))
    }

    @Test fun outOfRangeOutfitFallsBackToBaseIdentity() {
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarAssetSelector.assetName(99, true))
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarAssetSelector.assetName(-1, true))
    }
}
