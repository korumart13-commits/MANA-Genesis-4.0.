package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaEngineShogunIntegrationTest {
    private class MemoryStore : ManaMemoryStore {
        override fun readAll() = emptyList<ManaMemoryItem>()
        override fun writeAll(items: List<ManaMemoryItem>) = Unit
    }

    private class WorldStore : MicroworldStore {
        private var value = MicroworldSnapshot()
        override fun load() = value
        override fun save(snapshot: MicroworldSnapshot) { value = snapshot }
    }

    @Test
    fun engineDelegatesOnlyThroughHydraAndRechecksPermissionOnEveryCall() {
        val security = ManaSecurityService()
        val capability = SecurityCapability("owner", "microworld", SecurityAction.WRITE)
        var executions = 0
        val hydra = ManaHydraRegistry(security)
        hydra.register(object : ManaHydraTool {
            override val id = "microworld.observe"
            override val capability = capability
            override fun execute(input: String): String {
                executions++
                return "recorded:$input"
            }
        })

        val system = ManaSystemCore(DeviceFacts("RMX5566RU", "RE645F", 36, "5.15.197", "arm64-v8a"))
        system.register(object : ManaSystemModule {
            override val id = "engine.foundation"
            override fun start(context: ManaSystemContext) { context.services["ready"] = true }
        })
        val memory = ManaMemoryService(MemoryStore())
        val world = ManaMicroworldService(WorldStore())
        val reasoning = ManaReasoningPort {
            ManaReasoningResult("local response", "local-test", local = true)
        }
        val engine = ManaEngineRuntime(
            system, reasoning, memory, world, ManaShogun(hydra)
        )

        assertEquals(ManaEngineState.READY, engine.start().state)

        security.grant(SecurityGrant(capability))
        val allowed = engine.executeTask(
            ShogunTask(
                "microworld-write",
                "store an owner-approved observation",
                listOf(ManaHydraCall("microworld.observe", "owner", "new evidence"))
            )
        )
        assertTrue(allowed.completed)
        assertEquals("recorded:new evidence", allowed.results.single().output)
        assertEquals(1, executions)

        security.revoke(capability)
        val denied = engine.executeTask(
            ShogunTask(
                "microworld-write-after-revoke",
                "attempt another observation after permission revocation",
                listOf(ManaHydraCall("microworld.observe", "owner", "must not run"))
            )
        )
        assertFalse(denied.completed)
        assertEquals(1, executions, "Hydra must not execute after the grant is revoked")
        assertEquals(SecurityEffect.DENY, security.auditSnapshot().last().effect)

        assertEquals(ManaEngineState.STOPPED, engine.stop().state)
    }
}
