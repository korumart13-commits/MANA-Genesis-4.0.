package com.mana.launcher

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ManaAvatarStateTest {
    @Before fun reset() {
        ManaAvatarState.outfitIndex = 0
        ManaAvatarState.accessoryEnabled = true
        ManaAvatarState.expressionIndex = 0
        ManaAvatarState.isSpeaking = false
    }

    @After fun cleanup() {
        ManaAvatarState.outfitIndex = 0
        ManaAvatarState.accessoryEnabled = true
        ManaAvatarState.expressionIndex = 0
        ManaAvatarState.isSpeaking = false
    }

    @Test fun defaultAppearanceUsesBaseIdleAsset() {
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarState.assetName())
    }

    @Test fun wardrobeAndAccessorySelectPlayfulVariant() {
        ManaAvatarState.outfitIndex = 1
        ManaAvatarState.accessoryEnabled = false
        ManaAvatarState.expressionIndex = 1
        assertEquals("mana_avatar_genesis_casual_no_accessory_playful.gltf", ManaAvatarState.assetName())
    }

    @Test fun ttsUsesSpeakingVariantForEveryWardrobe() {
        ManaAvatarState.outfitIndex = 2
        ManaAvatarState.accessoryEnabled = false
        ManaAvatarState.isSpeaking = true
        assertEquals("mana_avatar_genesis_evening_no_accessory_speaking.gltf", ManaAvatarState.assetName())
    }

    @Test fun happyExpressionUsesBaseAssetAndHappyAnimationClip() {
        ManaAvatarState.expressionIndex = 2
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarState.assetName())
    }
}
