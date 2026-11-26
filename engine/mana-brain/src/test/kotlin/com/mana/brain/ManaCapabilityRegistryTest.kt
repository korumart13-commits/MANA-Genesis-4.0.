package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ManaCapabilityRegistryTest {
    private val registry = ManaCapabilityRegistry.platformDefaults()

    @Test
    fun unknownCapabilityIsDenied() {
        val result = registry.evaluate(
            "device.root_shell",
            CapabilityContext(emptySet(), ownerConfirmed = true, deviceSupportsCapability = true, enabled = true)
        )
        assertEquals(
            CapabilityDecision.Denied(DenialReason.NOT_REGISTERED),
            result
        )
    }

    @Test
    fun missingAndroidPermissionIsDenied() {
        val result = registry.evaluate(
            "contacts.read",
            CapabilityContext(emptySet(), ownerConfirmed = true, deviceSupportsCapability = true, enabled = true)
        )
        assertEquals(CapabilityDecision.Denied(DenialReason.MISSING_PERMISSION), result)
    }

    @Test
    fun sensitiveActionRequiresOwnerConfirmationEvenWhenPermissionExists() {
        val result = registry.evaluate(
            "contacts.read",
            CapabilityContext(
                setOf("android.permission.READ_CONTACTS"),
                ownerConfirmed = false,
                deviceSupportsCapability = true,
                enabled = true
            )
        )
        assertEquals(
            CapabilityDecision.Denied(DenialReason.OWNER_CONFIRMATION_REQUIRED),
            result
        )
    }

    @Test
    fun supportedLowRiskActionCanBeAllowed() {
        val result = registry.evaluate(
            "apps.launch",
            CapabilityContext(emptySet(), ownerConfirmed = false, deviceSupportsCapability = true, enabled = true)
        )
        assertIs<CapabilityDecision.Allowed>(result)
    }

    @Test
    fun disabledOrUnsupportedCapabilityIsDenied() {
        val disabled = registry.evaluate(
            "apps.launch",
            CapabilityContext(emptySet(), ownerConfirmed = false, deviceSupportsCapability = true, enabled = false)
        )
        val unsupported = registry.evaluate(
            "apps.launch",
            CapabilityContext(emptySet(), ownerConfirmed = false, deviceSupportsCapability = false, enabled = true)
        )
        assertEquals(CapabilityDecision.Denied(DenialReason.DISABLED), disabled)
        assertEquals(CapabilityDecision.Denied(DenialReason.DEVICE_UNSUPPORTED), unsupported)
    }

    @Test
    fun duplicateCapabilityIdsAreRejected() {
        val item = ManaCapability("apps.launch", "Launch an app")
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            ManaCapabilityRegistry(listOf(item, item))
        }
    }
}
