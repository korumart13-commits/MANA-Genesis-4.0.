package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import java.time.Instant

class ManaShogunHydraTest {
    @Test fun registeredToolIsNotCallableWithoutExplicitGrant() {
        val security = ManaSecurityService()
        var executions = 0
        val tool = object : ManaHydraTool {
            override val id = "microworld.observe"
            override val capability = SecurityCapability("placeholder", "microworld", SecurityAction.WRITE)
            override fun execute(input: String): String { executions++; return "stored:$input" }
        }
        val hydra = ManaHydraRegistry(security)
        hydra.register(tool)
        val report = ManaShogun(hydra).execute(
            ShogunTask("task-1", "record observation", listOf(ManaHydraCall(tool.id, "owner", "evidence")))
        )
        assertFalse(report.completed)
        assertEquals(0, executions)
        assertEquals(SecurityEffect.DENY, security.auditSnapshot().single().effect)
    }

    @Test fun grantedCapabilityExecutesAndIsAudited() {
        val security = ManaSecurityService()
        val grantedCapability = SecurityCapability("owner", "microworld", SecurityAction.WRITE)
        security.grant(SecurityGrant(grantedCapability))
        val hydra = ManaHydraRegistry(security)
        hydra.register(object : ManaHydraTool {
            override val id = "microworld.observe"
            override val capability = grantedCapability
            override fun execute(input: String) = "saved:$input"
        })
        val report = ManaShogun(hydra).execute(
            ShogunTask("task-2", "record observation", listOf(ManaHydraCall("microworld.observe", "owner", "signal")))
        )
        assertTrue(report.completed)
        assertEquals("saved:signal", report.results.single().output)
        assertEquals(SecurityEffect.ALLOW, security.auditSnapshot().single().effect)
    }

    @Test fun sensitiveNetworkActionRemainsDeniedEvenWithGrant() {
        val security = ManaSecurityService()
        val capability = SecurityCapability("owner", "cloud", SecurityAction.NETWORK)
        security.grant(SecurityGrant(capability))
        val hydra = ManaHydraRegistry(security)
        hydra.register(object : ManaHydraTool {
            override val id = "cloud.send"
            override val capability = capability
            override fun execute(input: String) = "should-not-run"
        })
        val report = ManaShogun(hydra).execute(
            ShogunTask("task-3", "send secret", listOf(ManaHydraCall("cloud.send", "owner", "secret", DataSensitivity.SECRET)))
        )
        assertFalse(report.completed)
        assertTrue(report.results.isEmpty())
    }

    @Test fun taskStopsAfterFirstDeniedStep() {
        val security = ManaSecurityService()
        val allowed = SecurityCapability("owner", "local", SecurityAction.READ)
        security.grant(SecurityGrant(allowed))
        val hydra = ManaHydraRegistry(security)
        var calls = 0
        hydra.register(object : ManaHydraTool {
            override val id = "local.read"
            override val capability = allowed
            override fun execute(input: String): String { calls++; return "ok" }
        })
        hydra.register(object : ManaHydraTool {
            override val id = "system.delete"
            override val capability = SecurityCapability("owner", "system", SecurityAction.DELETE)
            override fun execute(input: String): String { calls++; return "deleted" }
        })
        val report = ManaShogun(hydra).execute(
            ShogunTask("task-4", "read then delete", listOf(
                ManaHydraCall("local.read", "owner", "x"),
                ManaHydraCall("system.delete", "owner", "y")
            ))
        )
        assertFalse(report.completed)
        assertEquals(1, calls)
        assertEquals(1, report.results.size)
    }
}
