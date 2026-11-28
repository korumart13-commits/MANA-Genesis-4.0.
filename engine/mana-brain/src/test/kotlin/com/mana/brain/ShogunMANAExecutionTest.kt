package com.mana.brain

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ShogunMANAExecutionTest {
    private class FakeCapability(
        override val manifest: CapabilityManifest,
        private val response: String = "ok",
        private val delayMs: Long = 0
    ) : HydraCapability {
        var calls = 0
            private set

        override suspend fun execute(input: String): CapabilityResult {
            calls++
            if (delayMs > 0) delay(delayMs)
            return CapabilityResult("$response:$input")
        }
    }

    private fun step(capabilityId: String = "device.status") =
        CapabilityStep("step-1", capabilityId, "battery")

    @Test
    fun executesOnlyRegisteredAndAuthorizedCapability() = runBlocking {
        val capability = FakeCapability(CapabilityManifest("device.status"))
        val shogun = ShogunMANA(
            CapabilityRegistry(listOf(capability)),
            CapabilityAuthorizer { _, _ -> CapabilityAuthorization.ALLOW }
        )

        val result = shogun.execute(ShogunPlan("plan-1", listOf(step())))

        assertEquals(ShogunExecutionStatus.COMPLETED, result.status)
        assertEquals("ok:battery", result.steps.single().output)
        assertEquals(1, capability.calls)
    }

    @Test
    fun unknownCapabilityFailsClosed() = runBlocking {
        val capability = FakeCapability(CapabilityManifest("device.status"))
        val shogun = ShogunMANA(
            CapabilityRegistry(listOf(capability)),
            CapabilityAuthorizer { _, _ -> CapabilityAuthorization.ALLOW }
        )

        val result = shogun.execute(ShogunPlan("plan-2", listOf(step("system.unlock"))))

        assertEquals(ShogunExecutionStatus.CAPABILITY_UNAVAILABLE, result.status)
        assertEquals(0, capability.calls)
    }

    @Test
    fun deniedCapabilityIsNeverInvoked() = runBlocking {
        val capability = FakeCapability(CapabilityManifest("communications.send"))
        val shogun = ShogunMANA(
            CapabilityRegistry(listOf(capability)),
            CapabilityAuthorizer { _, _ -> CapabilityAuthorization.DENY }
        )

        val result = shogun.execute(ShogunPlan("plan-3", listOf(step("communications.send"))))

        assertEquals(ShogunExecutionStatus.DENIED, result.status)
        assertEquals(0, capability.calls)
    }

    @Test
    fun consequentialActionCanRequireOwnerConfirmation() = runBlocking {
        val capability = FakeCapability(
            CapabilityManifest("payments.transfer", risk = CapabilityRisk.CONSEQUENTIAL)
        )
        val shogun = ShogunMANA(
            CapabilityRegistry(listOf(capability)),
            CapabilityAuthorizer { manifest, _ ->
                if (manifest.risk == CapabilityRisk.CONSEQUENTIAL) {
                    CapabilityAuthorization.REQUIRE_USER_CONFIRMATION
                } else CapabilityAuthorization.ALLOW
            }
        )

        val result = shogun.execute(ShogunPlan("plan-4", listOf(step("payments.transfer"))))

        assertEquals(ShogunExecutionStatus.CONFIRMATION_REQUIRED, result.status)
        assertEquals(0, capability.calls)
    }

    @Test
    fun inputAndOutputLimitsAreEnforced() = runBlocking {
        val tooLargeInput = FakeCapability(
            CapabilityManifest("device.status", maxInputChars = 3)
        )
        val inputGuard = ShogunMANA(
            CapabilityRegistry(listOf(tooLargeInput)),
            CapabilityAuthorizer { _, _ -> CapabilityAuthorization.ALLOW }
        )
        val inputResult = inputGuard.execute(ShogunPlan("plan-5", listOf(step())))
        assertEquals(ShogunExecutionStatus.FAILED, inputResult.status)
        assertEquals(0, tooLargeInput.calls)

        val tooLargeOutput = FakeCapability(
            CapabilityManifest("device.status", maxOutputChars = 2),
            response = "long"
        )
        val outputGuard = ShogunMANA(
            CapabilityRegistry(listOf(tooLargeOutput)),
            CapabilityAuthorizer { _, _ -> CapabilityAuthorization.ALLOW }
        )
        val outputResult = outputGuard.execute(ShogunPlan("plan-6", listOf(step())))
        assertEquals(ShogunExecutionStatus.FAILED, outputResult.status)
    }

    @Test
    fun stepTimeoutStopsThePlan() = runBlocking {
        val slow = FakeCapability(CapabilityManifest("device.status"), delayMs = 100)
        val shogun = ShogunMANA(
            CapabilityRegistry(listOf(slow)),
            CapabilityAuthorizer { _, _ -> CapabilityAuthorization.ALLOW },
            stepTimeoutMs = 5
        )

        val result = shogun.execute(ShogunPlan("plan-7", listOf(step())))

        assertEquals(ShogunExecutionStatus.TIMED_OUT, result.status)
    }

    @Test
    fun duplicateCapabilityIdsAreRejected() {
        val first = FakeCapability(CapabilityManifest("device.status"))
        val second = FakeCapability(CapabilityManifest("device.status"))
        assertFailsWith<IllegalArgumentException> {
            CapabilityRegistry(listOf(first, second))
        }
    }

    @Test
    fun duplicateStepIdsAreRejected() {
        val steps = listOf(step(), CapabilityStep("step-1", "device.status", "network"))
        assertFailsWith<IllegalArgumentException> { ShogunPlan("plan-8", steps) }
    }
}
