package com.mana.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ManaResourceGovernorTest {
    @Test
    fun gemmaE2BUsesMappedWeightMemoryBudgetForEightGbPhones() {
        val gemmaBytes = 2_588_147_712L
        assertEquals(3_548L, ManaResourceGovernor.minimumAvailableRamMbForModel(gemmaBytes))
    }

    @Test
    fun smallModelsKeepAtLeastOneGbRuntimeHeadroom() {
        val oneGbModel = 1_073_741_824L
        assertEquals(2_048L, ManaResourceGovernor.minimumAvailableRamMbForModel(oneGbModel))
    }

    @Test
    fun emptyModelIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ManaResourceGovernor.minimumAvailableRamMbForModel(0L)
        }
    }
}
