package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManaSystemCoreTest {
    private val device = DeviceFacts(
        product = "RMX5566RU",
        device = "RE645F",
        androidApi = 36,
        kernelRelease = "5.15.197-android13-8-o",
        abi = "arm64-v8a",
        activeSlot = "_a"
    )

    @Test fun startsModulesInDependencyOrderAndStopsInReverse() {
        val events = mutableListOf<String>()
        val core = ManaSystemCore(device)
        core.register(object : ManaSystemModule {
            override val id = "hardware"
            override fun start(context: ManaSystemContext) { events += "start-hardware" }
            override fun stop(context: ManaSystemContext) { events += "stop-hardware" }
        })
        core.register(object : ManaSystemModule {
            override val id = "mana.brain"
            override val dependencies = setOf("hardware")
            override fun start(context: ManaSystemContext) { events += "start-brain" }
            override fun stop(context: ManaSystemContext) { events += "stop-brain" }
        })
        core.boot()
        assertEquals(BootPhase.READY, core.phase)
        core.stop()
        assertEquals(listOf("start-hardware", "start-brain", "stop-brain", "stop-hardware"), events)
    }

    @Test fun rejectsMissingDependenciesBeforeStarting() {
        val core = ManaSystemCore(device)
        core.register(object : ManaSystemModule {
            override val id = "mana.avatar"
            override val dependencies = setOf("mana.brain")
            override fun start(context: ManaSystemContext) = Unit
        })
        assertFailsWith<IllegalArgumentException> { core.boot() }
    }

    @Test fun failsSafelyWhenModuleCannotStart() {
        val core = ManaSystemCore(device)
        core.register(object : ManaSystemModule {
            override val id = "security"
            override fun start(context: ManaSystemContext) { error("policy init failed") }
        })
        val status = core.boot()
        assertEquals(BootPhase.DEGRADED, core.phase)
        assertEquals(ModuleState.FAILED, status.single().state)
    }

    @Test fun rollsBackStartedModulesWhenLaterModuleFails() {
        val events = mutableListOf<String>()
        val core = ManaSystemCore(device)
        core.register(object : ManaSystemModule {
            override val id = "base"
            override fun start(context: ManaSystemContext) { events += "start-base" }
            override fun stop(context: ManaSystemContext) { events += "stop-base" }
        })
        core.register(object : ManaSystemModule {
            override val id = "mana.brain"
            override val dependencies = setOf("base")
            override fun start(context: ManaSystemContext) { error("brain failed") }
        })
        val result = core.boot()
        assertEquals(BootPhase.DEGRADED, core.phase)
        assertEquals(listOf("start-base", "stop-base"), events)
        assertEquals(ModuleState.STOPPED, result.first { it.id == "base" }.state)
        assertEquals(ModuleState.FAILED, result.first { it.id == "mana.brain" }.state)
    }

    @Test fun rejectsDependencyCyclesBeforeStartingModules() {
        val events = mutableListOf<String>()
        val core = ManaSystemCore(device)
        core.register(object : ManaSystemModule {
            override val id = "alpha"
            override val dependencies = setOf("beta")
            override fun start(context: ManaSystemContext) { events += "alpha" }
        })
        core.register(object : ManaSystemModule {
            override val id = "beta"
            override val dependencies = setOf("alpha")
            override fun start(context: ManaSystemContext) { events += "beta" }
        })
        assertEquals(BootPhase.DEGRADED, core.boot().let { core.phase })
        assertEquals(emptyList(), events)
    }

}
