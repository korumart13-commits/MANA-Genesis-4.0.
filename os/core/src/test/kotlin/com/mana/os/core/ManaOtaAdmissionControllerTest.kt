package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ManaOtaAdmissionControllerTest {
    private val digest = "a".repeat(64)
    private val manifest = OtaManifest(
        version = 5, minVersion = 3, product = "RMX5566RU", device = "RE645F",
        fingerprintPrefix = "realme/RMX5566RU/RE645F:16/",
        payloadSha256 = digest, payloadSizeBytes = 1024, signingKeyId = "mana-test-release"
    )
    private val device = OtaDeviceProfile(
        product = "RMX5566RU", device = "RE645F",
        fingerprint = "realme/RMX5566RU/RE645F:16/build",
        installedVersion = 4, batteryPercent = 85, charging = true, unmeteredNetwork = true
    )

    @Test fun admitsMatchingSignedPayloadAndRequiresExplicitStateProgression() {
        val controller = ManaOtaAdmissionController { _, _ -> true }
        assertTrue(controller.inspect(manifest, device, digest).accepted)
        assertEquals(OtaState.VERIFIED, controller.state)
        controller.markStaged()
        controller.markApplying()
        controller.markAppliedAfterSuccessfulBoot()
        assertEquals(OtaState.APPLIED, controller.state)
    }

    @Test fun rejectsWrongDeviceEvenWhenSignatureIsValid() {
        val controller = ManaOtaAdmissionController { _, _ -> true }
        val result = controller.inspect(manifest.copy(device = "WRONG"), device, digest)
        assertFalse(result.accepted)
        assertTrue(result.reasons.any { it.contains("Device mismatch") })
        assertEquals(OtaState.FAILED, controller.state)
    }

    @Test fun rejectsDigestMismatchAndBadSignature() {
        val controller = ManaOtaAdmissionController { _, _ -> false }
        val result = controller.inspect(manifest, device, "b".repeat(64))
        assertFalse(result.accepted)
        assertTrue(result.reasons.any { it.contains("SHA-256 mismatch") })
        assertTrue(result.reasons.any { it.contains("signature") })
    }

    @Test fun rejectsLowBatteryAndMeteredNetworkForUnattendedUpdate() {
        val controller = ManaOtaAdmissionController { _, _ -> true }
        val result = controller.inspect(
            manifest,
            device.copy(batteryPercent = 10, charging = false, unmeteredNetwork = false),
            digest
        )
        assertFalse(result.accepted)
        assertEquals(2, result.reasons.size)
    }

    @Test fun preventsApplyingBeforeVerificationAndStaging() {
        val controller = ManaOtaAdmissionController { _, _ -> true }
        assertFailsWith<IllegalStateException> { controller.markApplying() }
    }

    @Test fun requiresNewerVersionAndSuccessfulBootToFinish() {
        val controller = ManaOtaAdmissionController { _, _ -> true }
        assertFalse(controller.inspect(manifest.copy(version = 4), device, digest).accepted)
        assertTrue(controller.lastFailure!!.contains("not newer"))
    }
}
