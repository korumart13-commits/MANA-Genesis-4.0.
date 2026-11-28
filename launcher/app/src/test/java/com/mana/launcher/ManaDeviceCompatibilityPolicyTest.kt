package com.mana.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManaDeviceCompatibilityPolicyTest {
    private fun identity(
        manufacturer: String = "Samsung",
        model: String = "Galaxy A55",
        device: String = "a55x",
        product: String = "a55x",
        fingerprint: String = "samsung/a55x/a55x:14/UP1A.231005.007/release",
        apiLevel: Int = 29
    ) = ManaDeviceIdentity(manufacturer, model, device, product, fingerprint, apiLevel)

    @Test
    fun nonTargetAndroid29DeviceStillRunsUniversalSetup() {
        val result = ManaDeviceCompatibilityPolicy.evaluate(identity())

        assertTrue(result.canRunUniversalSetup)
        assertFalse(result.matchesRmx5566TestProfile)
        assertTrue(result.profileMismatchReasons.isNotEmpty())
    }

    @Test
    fun exactRmx5566Api36ProfileIsRecognized() {
        val result = ManaDeviceCompatibilityPolicy.evaluate(
            identity(
                manufacturer = "realme",
                model = "RMX5566",
                device = "RE645F",
                product = "RE645F",
                fingerprint = "realme/RMX5566RU/RE645F:16/RMX5566_16.0.5.1310/EX01",
                apiLevel = 36
            )
        )

        assertTrue(result.canRunUniversalSetup)
        assertTrue(result.matchesRmx5566TestProfile)
        assertEquals(emptyList<String>(), result.profileMismatchReasons)
    }

    @Test
    fun profileMismatchDoesNotBlockUniversalSetup() {
        val result = ManaDeviceCompatibilityPolicy.evaluate(
            identity(
                manufacturer = "realme",
                model = "RMX5566",
                device = "RE645F",
                product = "RE645F",
                fingerprint = "realme/RMX5566RU/RE645F:16/custom-build",
                apiLevel = 36
            )
        )

        assertTrue(result.canRunUniversalSetup)
        assertFalse(result.matchesRmx5566TestProfile)
        assertEquals(listOf("fingerprint"), result.profileMismatchReasons)
    }

    @Test
    fun androidBelowMinimumApiIsNotEligibleForUniversalSetup() {
        val result = ManaDeviceCompatibilityPolicy.evaluate(identity(apiLevel = 28))

        assertFalse(result.canRunUniversalSetup)
    }
}
