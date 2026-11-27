package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaOtaInstallGateTest {
    private fun safe() = ManaOtaPreflight(
        exactDeviceMatch = true,
        exactBuildFamilyMatch = true,
        trustedSignatureVerified = true,
        payloadHashVerified = true,
        rollbackIndexAccepted = true,
        supportedOtaBackendAvailable = true,
        recoveryPathVerified = true,
        powerConnected = true,
        batteryPercent = 65,
        temperatureCelsius = 31.0,
        freeBytes = 8_000_000_000,
        requiredFreeBytes = 4_000_000_000,
        storageHealthy = true,
        ownerAuthorizationGranted = true
    )

    @Test fun permitsOnlyFullyVerifiedAndAuthorizedPreflight() {
        val decision = ManaOtaInstallGate.evaluate(safe())
        assertTrue(decision.allowed)
        assertTrue(decision.blockers.isEmpty())
    }

    @Test fun blocksWrongDeviceAndUntrustedPayload() {
        val decision = ManaOtaInstallGate.evaluate(safe().copy(
            exactDeviceMatch = false,
            trustedSignatureVerified = false,
            payloadHashVerified = false
        ))
        assertFalse(decision.allowed)
        assertEquals(
            listOf("DEVICE_MISMATCH", "SIGNATURE_NOT_VERIFIED", "PAYLOAD_HASH_NOT_VERIFIED"),
            decision.blockers
        )
    }

    @Test fun blocksUnsafePowerTemperatureStorageAndMissingRecovery() {
        val decision = ManaOtaInstallGate.evaluate(safe().copy(
            powerConnected = false,
            batteryPercent = 25,
            temperatureCelsius = 45.0,
            freeBytes = 1,
            recoveryPathVerified = false
        ))
        assertFalse(decision.allowed)
        assertTrue("INSUFFICIENT_BATTERY" in decision.blockers)
        assertTrue("TEMPERATURE_UNSAFE" in decision.blockers)
        assertTrue("INSUFFICIENT_STORAGE" in decision.blockers)
        assertTrue("RECOVERY_PATH_NOT_VERIFIED" in decision.blockers)
    }

    @Test fun neverAllowsUnapprovedProtectedFirmwareUpdate() {
        val decision = ManaOtaInstallGate.evaluate(safe().copy(ownerAuthorizationGranted = false))
        assertFalse(decision.allowed)
        assertEquals(listOf("OWNER_AUTHORIZATION_REQUIRED"), decision.blockers)
    }

    @Test fun rejectsInvalidSensorValuesAndMissingBackend() {
        val decision = ManaOtaInstallGate.evaluate(safe().copy(
            batteryPercent = 101,
            temperatureCelsius = Double.NaN,
            supportedOtaBackendAvailable = false
        ))
        assertFalse(decision.allowed)
        assertTrue("INVALID_BATTERY_READING" in decision.blockers)
        assertTrue("TEMPERATURE_UNSAFE" in decision.blockers)
        assertTrue("SUPPORTED_OTA_BACKEND_UNAVAILABLE" in decision.blockers)
    }
}
