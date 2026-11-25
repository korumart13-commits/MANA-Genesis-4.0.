package com.mana.launcher

import org.junit.Assert.assertTrue
import org.junit.Test

class ManaPersonaTest {
    @Test fun sharedPersonaDefinesIdentityLanguageToneAndActionTruthfulness() {
        val prompt = ManaPersona.SYSTEM_PROMPT
        assertTrue(prompt.contains("MANA"))
        assertTrue(prompt.contains("Отвечай на русском языке"))
        assertTrue(prompt.contains("игривые"))
        assertTrue(prompt.contains("Никогда не утверждай"))
        assertTrue(prompt.contains("Защищай секреты"))
    }
}
