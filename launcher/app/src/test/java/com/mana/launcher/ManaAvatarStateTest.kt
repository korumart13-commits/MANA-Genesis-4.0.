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

    @Test fun defaultAppearanceUsesBaseNeutralAsset() {
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarState.assetName())
    }

    @Test fun wardrobeAndAccessorySelectMatchingPlayfulAsset() {
        ManaAvatarState.outfitIndex = 1
        ManaAvatarState.accessoryEnabled = false
        ManaAvatarState.expressionIndex = 1
        assertEquals("mana_avatar_genesis_casual_no_accessory_playful.gltf", ManaAvatarState.assetName())
    }

    @Test fun eveningWardrobeUsesSpeakingVariantWhenTtsStarts() {
        ManaAvatarState.outfitIndex = 2
        ManaAvatarState.isSpeaking = true
        ManaAvatarState.expressionIndex = 1
        assertEquals("mana_avatar_genesis_evening_speaking.gltf", ManaAvatarState.assetName())
    }

    @Test fun speakingOverridesPlayfulExpression() {
        ManaAvatarState.expressionIndex = 1
        ManaAvatarState.isSpeaking = true
        assertEquals("mana_avatar_genesis_speaking.gltf", ManaAvatarState.assetName())
    }

    @Test fun happyExpressionUsesHappyAnimationOnBaseGeometry() {
        ManaAvatarState.expressionIndex = 2
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarState.assetName())
    }

    @Test fun invalidOutfitIndexFallsBackToBase() {
        ManaAvatarState.outfitIndex = 99
        assertEquals("mana_avatar_genesis.gltf", ManaAvatarState.assetName())
    }
}
