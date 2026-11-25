package com.mana.brain

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class TestCapability(
    override val descriptor: CapabilityDescriptor,
    private val outcome: CapabilityOutcome = CapabilityOutcome(true, true, "verified")
) : ManaCapability {
    var calls = 0
    override fun isAvailable() = true
    override suspend fun execute(input: Map<String, String>): CapabilityOutcome {
        calls++
        return outcome
    }
}

private class TestAudit : ActionAudit {
    val events = mutableListOf<ActionAuditEvent>()
    override fun record(event: ActionAuditEvent) { events += event }
}

class ManaActionExecutorTest {
    @Test
    fun highRiskActionRequiresOwnerApproval() = runBlocking {
        val capability = TestCapability(
            CapabilityDescriptor("send-message", "Send a message", risk = CapabilityRisk.HIGH)
        )
        val audit = TestAudit()
        val executor = ManaActionExecutor(
            CapabilityRegistry(listOf(capability)),
            PermissionChecker { true },
            OwnerApproval { _, _, _ -> false },
            audit
        )

        val report = executor.execute(ActionPlan("r1", listOf(ActionStep("s1", "send-message"))))

        assertEquals(ActionStatus.REJECTED_CONFIRMATION, report.results.single().status)
        assertEquals(0, capability.calls)
        assertFalse(report.successful)
        assertEquals(1, audit.events.size)
    }

    @Test
    fun missingAndroidPermissionBlocksCapability() = runBlocking {
        val capability = TestCapability(
            CapabilityDescriptor("contacts-read", "Read contacts", setOf("android.permission.READ_CONTACTS"))
        )
        val executor = ManaActionExecutor(
            CapabilityRegistry(listOf(capability)),
            PermissionChecker { false },
            OwnerApproval { _, _, _ -> true },
            TestAudit()
        )

        val report = executor.execute(ActionPlan("r2", listOf(ActionStep("s1", "contacts-read"))))

        assertEquals(ActionStatus.REJECTED_PERMISSION, report.results.single().status)
        assertEquals(0, capability.calls)
    }

    @Test
    fun unverifiedActionIsNeverReportedAsSuccess() = runBlocking {
        val capability = TestCapability(
            CapabilityDescriptor("torch", "Toggle torch"),
            CapabilityOutcome(true, false, "torch state unknown")
        )
        val executor = ManaActionExecutor(
            CapabilityRegistry(listOf(capability)),
            PermissionChecker { true },
            OwnerApproval { _, _, _ -> true },
            TestAudit()
        )

        val report = executor.execute(ActionPlan("r3", listOf(ActionStep("s1", "torch"))))

        assertEquals(ActionStatus.FAILED_VERIFICATION, report.results.single().status)
        assertFalse(report.successful)
    }

    @Test
    fun verifiedLowRiskActionSucceedsAndIsAudited() = runBlocking {
        val capability = TestCapability(CapabilityDescriptor("brightness", "Set brightness"))
        val audit = TestAudit()
        val executor = ManaActionExecutor(
            CapabilityRegistry(listOf(capability)),
            PermissionChecker { true },
            OwnerApproval { _, _, _ -> true },
            audit
        )

        val report = executor.execute(ActionPlan("r4", listOf(ActionStep("s1", "brightness"))))

        assertTrue(report.successful)
        assertEquals(ActionStatus.SUCCEEDED, report.results.single().status)
        assertEquals(ActionStatus.SUCCEEDED, audit.events.single().status)
    }

    @Test
    fun executionStopsAfterFirstFailure() = runBlocking {
        val first = TestCapability(
            CapabilityDescriptor("first", "First action"),
            CapabilityOutcome(false, false, "failed")
        )
        val second = TestCapability(CapabilityDescriptor("second", "Second action"))
        val executor = ManaActionExecutor(
            CapabilityRegistry(listOf(first, second)),
            PermissionChecker { true },
            OwnerApproval { _, _, _ -> true },
            TestAudit()
        )

        val report = executor.execute(
            ActionPlan("r5", listOf(ActionStep("s1", "first"), ActionStep("s2", "second")))
        )

        assertEquals(1, report.results.size)
        assertEquals(0, second.calls)
    }
}
