package com.mana.brain

/**
 * Owner-authorized action orchestration for MANA Brain.
 *
 * This layer never invokes shell commands or bypasses Android permissions. Concrete
 * capabilities must call documented Android APIs or authenticated system services.
 */
enum class CapabilityRisk { LOW, MEDIUM, HIGH, CRITICAL }

data class CapabilityDescriptor(
    val id: String,
    val description: String,
    val requiredPermissions: Set<String> = emptySet(),
    val risk: CapabilityRisk = CapabilityRisk.LOW,
    val ownerConfirmationRequired: Boolean = false,
    val enabled: Boolean = true
) {
    init {
        require(id.isNotBlank()) { "Capability id must not be blank" }
        require(description.isNotBlank()) { "Capability description must not be blank" }
        require(requiredPermissions.none { it.isBlank() }) { "Permission names must not be blank" }
    }

    val requiresConfirmation: Boolean
        get() = ownerConfirmationRequired ||
            risk == CapabilityRisk.HIGH || risk == CapabilityRisk.CRITICAL
}

data class ActionStep(
    val id: String,
    val capabilityId: String,
    val input: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "Step id must not be blank" }
        require(capabilityId.isNotBlank()) { "Capability id must not be blank" }
    }
}

data class ActionPlan(
    val requestId: String,
    val steps: List<ActionStep>
) {
    init {
        require(requestId.isNotBlank()) { "Request id must not be blank" }
        require(steps.isNotEmpty()) { "Action plan must contain at least one step" }
        require(steps.map { it.id }.distinct().size == steps.size) { "Step ids must be unique" }
    }
}

data class CapabilityOutcome(
    val success: Boolean,
    val verified: Boolean,
    val message: String
)

interface ManaCapability {
    val descriptor: CapabilityDescriptor
    fun isAvailable(): Boolean
    suspend fun execute(input: Map<String, String>): CapabilityOutcome
}

class CapabilityRegistry(capabilities: List<ManaCapability> = emptyList()) {
    private val byId: Map<String, ManaCapability>

    init {
        require(capabilities.map { it.descriptor.id }.distinct().size == capabilities.size) {
            "Capability identifiers must be unique"
        }
        byId = capabilities.associateBy { it.descriptor.id }
    }

    fun find(id: String): ManaCapability? = byId[id]

    fun available(): List<CapabilityDescriptor> =
        byId.values.filter { it.descriptor.enabled && it.isAvailable() }
            .map { it.descriptor }
            .sortedBy { it.id }
}

fun interface PermissionChecker {
    fun hasAll(permissions: Set<String>): Boolean
}

fun interface OwnerApproval {
    suspend fun approve(requestId: String, step: ActionStep, descriptor: CapabilityDescriptor): Boolean
}

interface ActionAudit {
    fun record(event: ActionAuditEvent)
}

data class ActionAuditEvent(
    val requestId: String,
    val stepId: String,
    val capabilityId: String,
    val status: ActionStatus,
    val message: String
)

enum class ActionStatus {
    SUCCEEDED,
    REJECTED_DISABLED,
    REJECTED_UNAVAILABLE,
    REJECTED_PERMISSION,
    REJECTED_CONFIRMATION,
    FAILED,
    FAILED_VERIFICATION
}

data class ActionStepResult(
    val stepId: String,
    val capabilityId: String,
    val status: ActionStatus,
    val message: String
)

data class ActionExecutionReport(
    val requestId: String,
    val results: List<ActionStepResult>
) {
    val successful: Boolean
        get() = results.isNotEmpty() && results.all { it.status == ActionStatus.SUCCEEDED }
}

/**
 * Executes a plan sequentially and stops at the first failed precondition or action.
 * A capability is successful only when it explicitly verifies its result.
 */
class ManaActionExecutor(
    private val registry: CapabilityRegistry,
    private val permissions: PermissionChecker,
    private val approval: OwnerApproval,
    private val audit: ActionAudit
) {
    suspend fun execute(plan: ActionPlan): ActionExecutionReport {
        val results = mutableListOf<ActionStepResult>()

        for (step in plan.steps) {
            val capability = registry.find(step.capabilityId)
            val result = when {
                capability == null ->
                    rejected(step, ActionStatus.REJECTED_UNAVAILABLE, "Capability is not registered")
                !capability.descriptor.enabled ->
                    rejected(step, ActionStatus.REJECTED_DISABLED, "Capability is disabled")
                !capability.isAvailable() ->
                    rejected(step, ActionStatus.REJECTED_UNAVAILABLE, "Capability is unavailable")
                !permissions.hasAll(capability.descriptor.requiredPermissions) ->
                    rejected(step, ActionStatus.REJECTED_PERMISSION, "Required Android permissions are missing")
                capability.descriptor.requiresConfirmation &&
                    !approval.approve(plan.requestId, step, capability.descriptor) ->
                    rejected(step, ActionStatus.REJECTED_CONFIRMATION, "Owner confirmation was not granted")
                else -> {
                    try {
                        val outcome = capability.execute(step.input)
                        when {
                            !outcome.success -> rejected(step, ActionStatus.FAILED, outcome.message)
                            !outcome.verified -> rejected(
                                step,
                                ActionStatus.FAILED_VERIFICATION,
                                "Action ran but its result was not verified: ${outcome.message}"
                            )
                            else -> rejected(step, ActionStatus.SUCCEEDED, outcome.message)
                        }
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        rejected(
                            step,
                            ActionStatus.FAILED,
                            error.message?.take(240) ?: "Capability execution failed"
                        )
                    }
                }
            }

            results += result
            audit.record(
                ActionAuditEvent(
                    requestId = plan.requestId,
                    stepId = step.id,
                    capabilityId = step.capabilityId,
                    status = result.status,
                    message = result.message
                )
            )
            if (result.status != ActionStatus.SUCCEEDED) break
        }

        return ActionExecutionReport(plan.requestId, results)
    }

    private fun rejected(
        step: ActionStep,
        status: ActionStatus,
        message: String
    ) = ActionStepResult(step.id, step.capabilityId, status, message)
}
