package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class ManaBootEngineTest {
    private val identity = ManaRuntimeIdentity(
        product = "RMX5566RU",
        device = "RE645F",
        androidApi = 36,
        abi = "arm64-v8a",
        kernelRelease = "5.15.197-android13-8-o",
        verifiedBootState = "green"
    )

    private fun core() = ManaSystemCore(
        DeviceFacts("RMX5566RU", "RE645F", 36, "5.15.197-android13-8-o", "arm64-v8a")
    ).also { system ->
        system.register(object : ManaSystemModule {
            override val id = "mana.brain"
            override fun start(context: ManaSystemContext) = Unit
        })
    }

    @Test fun startsUserspaceOnlyAfterExactRuntimeIdentityMatches() {
        val engine = ManaBootEngine(core())
        val report = engine.start(identity)
        assertEquals(ManaBootStage.READY, report.stage)
        assertEquals(FirmwareInterfaceStatus.NOT_VERIFIED, report.firmwareInterface)
        assertTrue(report.blockers.isEmpty())
        assertEquals("RUNNING", report.moduleHealth.single().state.name)
    }

    @Test fun blocksOnWrongDeviceWithoutStartingModules() {
        val engine = ManaBootEngine(core())
        val report = engine.start(identity.copy(device = "RMX5253"))
        assertEquals(ManaBootStage.BLOCKED, report.stage)
        assertTrue(report.blockers.any { it.contains("Device mismatch") })
        assertTrue(report.moduleHealth.isEmpty())
    }

    @Test fun blocksWhenVerifiedBootIsNotGreen() {
        val engine = ManaBootEngine(core())
        val report = engine.start(identity.copy(verifiedBootState = "orange"))
        assertEquals(ManaBootStage.BLOCKED, report.stage)
        assertTrue(report.blockers.any { it.contains("Verified Boot") })
    }

    @Test fun refusesSecondStart() {
        val engine = ManaBootEngine(core())
        engine.start(identity)
        assertFailsWith<IllegalStateException> { engine.start(identity) }
    }

    @Test fun stopTransitionsToStopped() {
        val engine = ManaBootEngine(core())
        engine.start(identity)
        engine.stop()
        assertEquals(ManaBootStage.STOPPED, engine.stage)
    }
}
