package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ManaGenesisBacklogTest {
    @Test fun initialBacklogKeepsHardwareAndRomWorkBlockedUntilEvidenceExists() {
        val backlog = ManaGenesisBacklog.initial()
        val plan = backlog.plan(GenesisCapabilitySnapshot(
            deviceFingerprint = "RMX5566RU/RE645F/Android16",
            androidApi = 36,
            installedModules = setOf("mana.brain", "mana.launcher"),
            availableCapabilities = emptySet()
        ))
        assertTrue(plan.blockedWork.any { it.id == "device.rmx5566.sources" })
        assertTrue(plan.orderedWork.any { it.id == "device.public-source-discovery" })
        assertTrue(plan.orderedWork.any { it.id == "android.emulator-validation" })
        assertTrue(plan.orderedWork.any { it.id == "brain.gpt-link" })
        assertTrue(plan.orderedWork.any { it.id == "forge.patch-pipeline" })
        assertTrue(plan.orderedWork.none { it.id == "device.rom-build" })
    }

    @Test fun discoveryAndEmulatorWorkCanProceedBeforeDeviceSourcesAreAvailable() {
        val plan = ManaGenesisBacklog.initial().plan(GenesisCapabilitySnapshot(
            "RMX5566RU", 36, emptySet(), emptySet()
        ))
        val ids = plan.orderedWork.map { it.id }
        assertTrue("device.public-source-discovery" in ids)
        assertTrue("android.emulator-validation" in ids)
        assertTrue("device.rmx5566.sources" in plan.blockedWork.map { it.id })
        assertTrue("device.rom-build" !in ids)
    }

    @Test fun dependenciesAreOrderedBeforeDependentWork() {
        val plan = ManaGenesisBacklog.initial().plan(GenesisCapabilitySnapshot(
            "RMX5566RU", 36, emptySet(), emptySet()
        ))
        val ids = plan.orderedWork.map { it.id }
        assertTrue(ids.indexOf("brain.gpt-link") < ids.indexOf("forge.patch-pipeline"))
        assertTrue(ids.indexOf("forge.patch-pipeline") < ids.indexOf("security.lab"))
        assertTrue(ids.indexOf("android.platform-integration") < ids.indexOf("android.emulator-validation"))
        assertTrue(ids.indexOf("device.public-source-discovery") < ids.indexOf("device.first-boot-self-completion"))
    }

    @Test fun unknownDependenciesAreRejected() {
        assertFailsWith<IllegalArgumentException> {
            ManaGenesisBacklog(listOf(GenesisWorkItem(
                "test.item", "Test", "Test item", GenesisWorkPriority.NORMAL,
                GenesisWorkStatus.INTEGRATION_PENDING, dependencies = setOf("missing.item")
            )))
        }
    }

    @Test fun dependencyCyclesAreRejected() {
        assertFailsWith<IllegalStateException> {
            ManaGenesisBacklog(listOf(
                GenesisWorkItem("test.one", "One", "One", GenesisWorkPriority.NORMAL,
                    GenesisWorkStatus.INTEGRATION_PENDING, dependencies = setOf("test.two")),
                GenesisWorkItem("test.two", "Two", "Two", GenesisWorkPriority.NORMAL,
                    GenesisWorkStatus.INTEGRATION_PENDING, dependencies = setOf("test.one"))
            ))
        }
    }

    @Test fun workItemsRemainDeterministicallyOrdered() {
        val snapshot = GenesisCapabilitySnapshot("RMX5566RU", 36, emptySet(), emptySet())
        val backlog = ManaGenesisBacklog.initial()
        assertEquals(backlog.plan(snapshot).orderedWork.map { it.id },
            backlog.plan(snapshot).orderedWork.map { it.id })
    }
}
