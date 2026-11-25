package com.mana.os.core

enum class SystemReadiness { READY, DEGRADED, BLOCKED }

data class SystemReadinessReport(
    val readiness: SystemReadiness,
    val requiredFailures: List<String>,
    val optionalFailures: List<String>,
    val missingHealthRecords: List<String>,
    val catalogErrors: List<String>
)

/**
 * Read-only readiness evaluator for the MANA userspace service graph.
 * It reports source/runtime health; it does not change boot state or Android policy.
 */
object ManaSystemReadinessEvaluator {
    fun evaluate(
        phase: BootPhase,
        catalog: List<SystemModuleDescriptor>,
        health: List<ModuleHealth>
    ): SystemReadinessReport {
        val errors = mutableListOf<String>()
        val ids = catalog.map { it.id }
        if (ids.size != ids.toSet().size) errors += "Duplicate module identifiers."
        val known = ids.toSet()
        catalog.forEach { descriptor ->
            (descriptor.dependencies - known).sorted().forEach {
                errors += "${descriptor.id} depends on missing module $it."
            }
            if (descriptor.id in descriptor.dependencies) {
                errors += "${descriptor.id} depends on itself."
            }
        }

        val healthIds = health.map { it.id }
        if (healthIds.size != healthIds.toSet().size) errors += "Duplicate module health records."
        val healthById = health.associateBy { it.id }
        val missing = catalog.map { it.id }.filter { it !in healthById }.sorted()
        val requiredFailures = mutableListOf<String>()
        val optionalFailures = mutableListOf<String>()
        catalog.forEach { descriptor ->
            val state = healthById[descriptor.id]?.state
            if (state != ModuleState.RUNNING) {
                val detail = healthById[descriptor.id]?.detail
                val reason = "${descriptor.id}: ${detail ?: state?.name ?: "no health record"}"
                if (descriptor.criticality == ModuleCriticality.REQUIRED) requiredFailures += reason
                else optionalFailures += reason
            }
        }

        val readiness = when {
            errors.isNotEmpty() || requiredFailures.isNotEmpty() || missing.isNotEmpty() ||
                phase !in setOf(BootPhase.READY, BootPhase.DEGRADED) -> SystemReadiness.BLOCKED
            phase == BootPhase.DEGRADED || optionalFailures.isNotEmpty() -> SystemReadiness.DEGRADED
            else -> SystemReadiness.READY
        }
        return SystemReadinessReport(
            readiness,
            requiredFailures.sorted(),
            optionalFailures.sorted(),
            missing,
            errors.sorted()
        )
    }
}
