package com.mana.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ManaVoiceProfileTest {
    @Test fun prefersRussianFemaleNamedVoiceAndOfflineAvailability() {
        val selected = ManaVoiceProfile.choose(listOf(
            ManaVoiceProfile.Candidate("ru-standard", "ru-RU", 500, false),
            ManaVoiceProfile.Candidate("ru-female-local", "ru-RU", 300, false),
            ManaVoiceProfile.Candidate("en-female", "en-US", 500, false)
        ))
        assertEquals("ru-female-local", selected?.name)
    }

    @Test fun prefersOfflineWhenFemaleMarkerIsUnavailable() {
        val selected = ManaVoiceProfile.choose(listOf(
            ManaVoiceProfile.Candidate("ru-online", "ru-RU", 500, true),
            ManaVoiceProfile.Candidate("ru-local", "ru-RU", 300, false)
        ))
        assertEquals("ru-local", selected?.name)
    }

    @Test fun returnsNullWhenNoRussianVoiceExists() {
        assertNull(ManaVoiceProfile.choose(listOf(
            ManaVoiceProfile.Candidate("en-female", "en-US", 500, false)
        )))
    }
}
