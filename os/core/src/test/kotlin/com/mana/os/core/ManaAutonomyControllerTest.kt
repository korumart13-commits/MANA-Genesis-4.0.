package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManaAutonomyControllerTest {
    private val capability = SecurityCapability("mana.brain", "device.torch", SecurityAction.EXECUTE)

    private fun controller(executed: MutableList<ManaActionRequest>): ManaAutonomyController {
        val security = ManaSecurityService().apply { grant(SecurityGrant(capability)) }
        return ManaAutonomyController(security, ManaDeviceActionExecutor {
            executed += it
            "ok"
        }).apply { register(ManaDefaultActions.specs.first { it.id == "set_torch" }) }
    }

    @Test fun `allowlisted action executes only with explicit capability`() {
        val calls = mutableListOf<ManaActionRequest>()
        val result = controller(calls).request(ManaActionRequest("1", action = "set_torch", parameters = mapOf("enabled" to "true")))
        assertEquals(ManaActionOutcome.EXECUTED, result.outcome)
        assertEquals(1, calls.size)
    }

    @Test fun `unknown action is denied and never reaches adapter`() {
        val calls = mutableListOf<ManaActionRequest>()
        val result = controller(calls).request(ManaActionRequest("2", action = "run_shell", parameters = mapOf("command" to "id")))
        assertEquals(ManaActionOutcome.DENIED, result.outcome)
        assertTrue(calls.isEmpty())
    }

    @Test fun `unconfirmed disruptive action waits for owner`() {
        val calls = mutableListOf<ManaActionRequest>()
        val c = controller(calls)
        c.register(ManaActionSpec("dnd", "device.notifications.dnd", ManaActionRisk.DISRUPTIVE,
            requiredParameters = setOf("enabled")))
        val result = c.request(ManaActionRequest("3", action = "dnd", parameters = mapOf("enabled" to "true")))
        assertEquals(ManaActionOutcome.NEEDS_OWNER_CONFIRMATION, result.outcome)
        assertTrue(calls.isEmpty())
    }

    @Test fun `firmware action cannot bypass signed OTA pipeline`() {
        val calls = mutableListOf<ManaActionRequest>()
        val c = controller(calls)
        c.register(ManaDefaultActions.specs.first { it.id == "install_verified_update" })
        val result = c.request(ManaActionRequest("4", action = "install_verified_update",
            parameters = mapOf("package_id" to "candidate", "sha256" to "00".repeat(32)), ownerConfirmed = true))
        assertEquals(ManaActionOutcome.DENIED, result.outcome)
        assertTrue(calls.isEmpty())
    }

    @Test fun `unexpected model parameters are denied`() {
        val calls = mutableListOf<ManaActionRequest>()
        val result = controller(calls).request(ManaActionRequest("5", action = "set_torch",
            parameters = mapOf("enabled" to "true", "shell" to "rm -rf /")))
        assertEquals(ManaActionOutcome.DENIED, result.outcome)
        assertTrue(calls.isEmpty())
    }
}
