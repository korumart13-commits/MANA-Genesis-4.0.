package com.mana.brain

/**
 * Declarative boundary between MANA Brain (the decision-maker) and MANA OS
 * (the executor of device capabilities).
 *
 * This registry never performs Android actions and never grants Android
 * permissions. The platform adapter must authenticate the caller, check the
 * live Android permission/AppOps state, and execute only the registered action.
 */
enum class CapabilityRisk { LOW, ELEVATED, SENSITIVE }

data class ManaCapability(
    val id: String,
    val description: String,
    val requiredPermissions: Set<String> = emptySet(),
    val risk: CapabilityRisk = CapabilityRisk.LOW,
    val requiresOwnerConfirmation: Boolean = false,
    val enabledByDefault: Boolean = false
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_.-]{1,63}"))) { "Invalid capability id" }
        require(description.isNotBlank()) { "Capability description must not be blank" }
        require(requiredPermissions.none { it.isBlank() }) { "Permission names must not be blank" }
        require(risk != CapabilityRisk.SENSITIVE || requiresOwnerConfirmation) {
            "Sensitive capabilities must require owner confirmation"
        }
    }
}

data class CapabilityContext(
    val grantedPermissions: Set<String>,
    val ownerConfirmed: Boolean,
    val deviceSupportsCapability: Boolean,
    val enabled: Boolean
)

sealed class CapabilityDecision {
    data object Allowed : CapabilityDecision()
    data class Denied(val reason: DenialReason) : CapabilityDecision()
}

enum class DenialReason {
    NOT_REGISTERED,
    DISABLED,
    DEVICE_UNSUPPORTED,
    MISSING_PERMISSION,
    OWNER_CONFIRMATION_REQUIRED
}

class ManaCapabilityRegistry(capabilities: Collection<ManaCapability>) {
    private val byId: Map<String, ManaCapability>

    init {
        require(capabilities.map { it.id }.distinct().size == capabilities.size) {
            "Capability identifiers must be unique"
        }
        byId = capabilities.associateBy { it.id }
    }

    fun get(id: String): ManaCapability? = byId[id]

    fun all(): List<ManaCapability> = byId.values.sortedBy { it.id }

    fun evaluate(id: String, context: CapabilityContext): CapabilityDecision {
        val capability = byId[id]
            ?: return CapabilityDecision.Denied(DenialReason.NOT_REGISTERED)
        if (!context.enabled) return CapabilityDecision.Denied(DenialReason.DISABLED)
        if (!context.deviceSupportsCapability) {
            return CapabilityDecision.Denied(DenialReason.DEVICE_UNSUPPORTED)
        }
        if (!context.grantedPermissions.containsAll(capability.requiredPermissions)) {
            return CapabilityDecision.Denied(DenialReason.MISSING_PERMISSION)
        }
        if (capability.requiresOwnerConfirmation && !context.ownerConfirmed) {
            return CapabilityDecision.Denied(DenialReason.OWNER_CONFIRMATION_REQUIRED)
        }
        return CapabilityDecision.Allowed
    }

    companion object {
        /**
         * Conservative starter catalog. Platform adapters must map these IDs
         * to explicit Android APIs/intents; no shell or arbitrary code execution.
         */
        fun platformDefaults(): ManaCapabilityRegistry = ManaCapabilityRegistry(
            listOf(
                ManaCapability(
                    id = "apps.launch",
                    description = "Launch an installed application",
                    risk = CapabilityRisk.LOW,
                    enabledByDefault = true
                ),
                ManaCapability(
                    id = "device.settings.open",
                    description = "Open a supported Android settings screen",
                    risk = CapabilityRisk.LOW,
                    enabledByDefault = true
                ),
                ManaCapability(
                    id = "device.notifications.read",
                    description = "Read notifications through an owner-enabled listener",
                    requiredPermissions = setOf("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"),
                    risk = CapabilityRisk.ELEVATED
                ),
                ManaCapability(
                    id = "contacts.read",
                    description = "Read contacts stored on this device",
                    requiredPermissions = setOf("android.permission.READ_CONTACTS"),
                    risk = CapabilityRisk.SENSITIVE,
                    requiresOwnerConfirmation = true
                ),
                ManaCapability(
                    id = "calls.answer",
                    description = "Answer an incoming call through the approved dialer role",
                    requiredPermissions = setOf("android.permission.ANSWER_PHONE_CALLS"),
                    risk = CapabilityRisk.SENSITIVE,
                    requiresOwnerConfirmation = true
                ),
                ManaCapability(
                    id = "updates.install",
                    description = "Install a verified, signed MANA platform update",
                    risk = CapabilityRisk.SENSITIVE,
                    requiresOwnerConfirmation = true
                )
            )
        )
    }
}
