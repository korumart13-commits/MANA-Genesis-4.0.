package com.mana.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class ManaRealmeGameSpaceBridgeTest {
    @Test fun recognizesKnownRealmeAndOplusGameHubPackageIds() {
        assertEquals(
            listOf("com.coloros.gamespaceui", "com.oplus.games"),
            ManaRealmeGameSpaceBridge.SUPPORTED_PACKAGES
        )
    }
}
