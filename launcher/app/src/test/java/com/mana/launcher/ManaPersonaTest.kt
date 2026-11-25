package com.mana.launcher

import org.junit.Assert.assertTrue
import org.junit.Test

class ManaPersonaTest {
    @Test fun sharedPersonaDefinesIdentityLanguageToneAndActionTruthfulness() {
        val prompt = ManaPersona.SYSTEM_PROMPT
        assertTrue(prompt.contains("MANA"))
        assertTrue(prompt.contains("Reply in Russian"))
        assertTrue(prompt.contains("playful"))
        assertTrue(prompt.contains("Never claim"))
        assertTrue(prompt.contains("Protect secrets"))
    }
}
