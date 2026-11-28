package com.mana.brain

import kotlinx.coroutines.withTimeoutOrNull

/**
 * Deterministic Shogun-to-Hydra execution boundary.
 *
 * A plan is data, not authority: every step must resolve to a registered
 * capability and pass the host application's authorization policy.
 */
enum class CapabilityRisk {
    SAFE,
    SENSITIVE,
    CONSEQUENTIAL
}

data class CapabilityManifest(
    val id: String,
    val requiredPermissions: Set<String> = emptySet(),
    val risk: CapabilityRisk = CapabilityRisk.SAFE,
    val maxInputChars: Int = DEFAULT_MAX_INPUT_CHARS,
    val maxOutputChars: Int = DEFAULT_MAX_OUTPUT_CHARS
) {
    init {
        require(id.isNotBlank()) { "Capability id must not be blank" }
        require(requiredPermissions.none { it.isBlank() }) {
            "Capability permissions must not contain blank values"
        }
        require(maxInputChars > 0) { "Maximum input length must be positive" }
        require(maxOutputChars > 0) { "Maximum output length must be positive" }
    }

    companion object {
        const val DEFAULT_MAX_INPUT_CHARS = 16_000
        const val DEFAULT_MAX_OUTPUT_CHARS = 32_000
    }
}

data class CapabilityStep(
    val id: String,
    val capabilityId: String,
    val input: String
) {
    init {
        require(id.isNotBlank()) { "Step id must not be blank" }
        require(capabilityId.isNotBlank()) { "Capability id must not be blank" }
    }
}

data class ShogunPlan(
    val id: String,
    val steps: List<CapabilityStep>
) {
    init {
        require(id.isNotBlank()) { "Plan id must not be blank" }
        require(steps.isNotEmpty()) { "Plan must contain at least one step" }
        require(steps.map { it.id }.distinct().size == steps.size) {
            "Plan step ids must be unique"
        }
    }
}

data class CapabilityResult(val output: String)

interface HydraCapability {
    val manifest: CapabilityManifest
    suspend fun execute(input: String): CapabilityResult
}

enum class CapabilityAuthorization {
    ALLOW,
    DENY,
    REQUIRE_USER_CONFIRMATION
}

/**
 * The Android host implements this policy using current permissions,
 * owner consent and capability-specific safeguards. Confirmation is never
 * inferred from a model-generated plan.
 */
fun interface CapabilityAuthorizer {
    suspend fun authorize(
        manifest: CapabilityManifest,
        step: CapabilityStep
    ): CapabilityAuthorization
}

class CapabilityRegistry(capabilities: List<HydraCapability>) {
    private val byId: Map<String, HydraCapability>

    init {
        require(capabilities.map { it.manifest.id }.distinct().size == capabilities.size) {
            "Capability ids must be unique"
        }
        byId = capabilities.associateBy { it.manifest.id }
    }

    fun find(id: String): HydraCapability? = byId[id]

    fun manifests(): List<CapabilityManifest> = byId.values.map { it.manifest }
}

enum class ShogunExecutionStatus {
    COMPLETED,
    DENIED,
    CONFIRMATION_REQUIRED,
    CAPABILITY_UNAVAILABLE,
    TIMED_OUT,
    FAILED
}

data class ShogunStepResult(
    val stepId: String,
    val capabilityId: String,
    val status: ShogunExecutionStatus,
    val output: String? = null,
    val detail: String? = null
)

data class ShogunExecutionResult(
    val planId: String,
    val steps: List<ShogunStepResult>,
    val status: ShogunExecutionStatus
)

/**
 * Executes a bounded plan sequentially. It deliberately has no shell,
 * reflection, arbitrary-intent or unrestricted filesystem capability.
 */
class ShogunMANA(
    private val registry: CapabilityRegistry,
    private val authorizer: CapabilityAuthorizer,
    private val maxPlanSteps: Int = DEFAULT_MAX_PLAN_STEPS,
    private val stepTimeoutMs: Long = DEFAULT_STEP_TIMEOUT_MS
) {
    init {
        require(maxPlanSteps > 0) { "Maximum plan steps must be positive" }
        require(stepTimeoutMs > 0) { "Step timeout must be positive" }
    }

    suspend fun execute(plan: ShogunPlan): ShogunExecutionResult {
        require(plan.steps.size <= maxPlanSteps) {
            "Plan exceeds the maximum number of allowed steps"
        }

        val results = mutableListOf<ShogunStepResult>()
        for (step in plan.steps) {
            val capability = registry.find(step.capabilityId)
            if (capability == null) {
                results += ShogunStepResult(
                    step.id, step.capabilityId,
                    ShogunExecutionStatus.CAPABILITY_UNAVAILABLE,
                    detail = "Capability is not registered."
                )
                return ShogunExecutionResult(plan.id, results, ShogunExecutionStatus.CAPABILITY_UNAVAILABLE)
            }

            val manifest = capability.manifest
            if (step.input.length > manifest.maxInputChars) {
                results += ShogunStepResult(
                    step.id, step.capabilityId,
                    ShogunExecutionStatus.FAILED,
                    detail = "Capability input exceeds its declared limit."
                )
                return ShogunExecutionResult(plan.id, results, ShogunExecutionStatus.FAILED)
            }

            val authorization = authorizer.authorize(manifest, step)
            if (authorization != CapabilityAuthorization.ALLOW) {
                val status = when (authorization) {
                    CapabilityAuthorization.DENY -> ShogunExecutionStatus.DENIED
                    CapabilityAuthorization.REQUIRE_USER_CONFIRMATION ->
                        ShogunExecutionStatus.CONFIRMATION_REQUIRED
                    CapabilityAuthorization.ALLOW -> error("Unreachable")
                }
                results += ShogunStepResult(
                    step.id, step.capabilityId, status,
                    detail = "Host authorization did not permit execution."
                )
                return ShogunExecutionResult(plan.id, results, status)
            }

            try {
                val capabilityResult = withTimeoutOrNull(stepTimeoutMs) {
                    capability.execute(step.input)
                }
                if (capabilityResult == null) {
                    results += ShogunStepResult(
                        step.id, step.capabilityId,
                        ShogunExecutionStatus.TIMED_OUT,
                        detail = "Capability exceeded its execution deadline."
                    )
                    return ShogunExecutionResult(plan.id, results, ShogunExecutionStatus.TIMED_OUT)
                }
                if (capabilityResult.output.length > manifest.maxOutputChars) {
                    results += ShogunStepResult(
                        step.id, step.capabilityId,
                        ShogunExecutionStatus.FAILED,
                        detail = "Capability output exceeds its declared limit."
                    )
                    return ShogunExecutionResult(plan.id, results, ShogunExecutionStatus.FAILED)
                }
                results += ShogunStepResult(
                    step.id, step.capabilityId,
                    ShogunExecutionStatus.COMPLETED,
                    output = capabilityResult.output
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                results += ShogunStepResult(
                    step.id, step.capabilityId,
                    ShogunExecutionStatus.FAILED,
                    detail = failure.message ?: failure::class.simpleName
                )
                return ShogunExecutionResult(plan.id, results, ShogunExecutionStatus.FAILED)
            }
        }

        return ShogunExecutionResult(plan.id, results, ShogunExecutionStatus.COMPLETED)
    }

    companion object {
        const val DEFAULT_MAX_PLAN_STEPS = 16
        const val DEFAULT_STEP_TIMEOUT_MS = 30_000L
    }
}
