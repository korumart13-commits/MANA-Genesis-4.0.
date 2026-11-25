package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManaSystemReadinessEvaluatorTest {
    private val required = SystemModuleDescriptor(
        "platform.security", "com.mana.platform.security", emptySet(),
        ModuleCriticality.REQUIRED, ModuleFailurePolicy.ABORT_BOOT, "system security"
    )
    private val optional = SystemModuleDescriptor(
        "mana.avatar", "com.mana.avatar", setOf("platform.security"),
        ModuleCriticality.OPTIONAL, ModuleFailurePolicy.DISABLE_MODULE, "explicit media permission"
    )

    @Test fun reportsReadyWhenAllModulesAreRunning() {
        val report = ManaSystemReadinessEvaluator.evaluate(
            BootPhase.READY,
            listOf(required, optional),
            listOf(
                ModuleHealth(required.id, ModuleState.RUNNING),
                ModuleHealth(optional.id, ModuleState.RUNNING)
            )
        )
        assertEquals(SystemReadiness.READY, report.readiness)
    }

    @Test fun optionalFailureDegradesButDoesNotBlockSystem() {
        val report = ManaSystemReadinessEvaluator.evaluate(
            BootPhase.READY,
            listOf(required, optional),
            listOf(
                ModuleHealth(required.id, ModuleState.RUNNING),
                ModuleHealth(optional.id, ModuleState.FAILED, "camera permission absent")
            )
        )
        assertEquals(SystemReadiness.DEGRADED, report.readiness)
        assertTrue(report.requiredFailures.isEmpty())
        assertEquals(1, report.optionalFailures.size)
    }

    @Test fun requiredFailureBlocksReadiness() {
        val report = ManaSystemReadinessEvaluator.evaluate(
            BootPhase.DEGRADED,
            listOf(required, optional),
            listOf(
                ModuleHealth(required.id, ModuleState.FAILED, "policy service failed"),
                ModuleHealth(optional.id, ModuleState.STOPPED)
            )
        )
        assertEquals(SystemReadiness.BLOCKED, report.readiness)
        assertEquals(1, report.requiredFailures.size)
    }

    @Test fun missingHealthAndCatalogDependencyAreBlocked() {
        val broken = optional.copy(dependencies = setOf("missing.module"))
        val report = ManaSystemReadinessEvaluator.evaluate(BootPhase.READY, listOf(required, broken),
            listOf(ModuleHealth(required.id, ModuleState.RUNNING)))
        assertEquals(SystemReadiness.BLOCKED, report.readiness)
        assertTrue(report.missingHealthRecords.contains(optional.id))
        assertTrue(report.catalogErrors.isNotEmpty())
    }

    @Test fun registeredRequiredModuleDoesNotCountAsReady() {
        val report = ManaSystemReadinessEvaluator.evaluate(
            BootPhase.READY, listOf(required),
            listOf(ModuleHealth(required.id, ModuleState.REGISTERED))
        )
        assertEquals(SystemReadiness.BLOCKED, report.readiness)
        assertEquals(1, report.requiredFailures.size)
    }

    @Test fun duplicateHealthRecordsAreCatalogErrorsAndBlockReadiness() {
        val report = ManaSystemReadinessEvaluator.evaluate(
            BootPhase.READY, listOf(required),
            listOf(
                ModuleHealth(required.id, ModuleState.RUNNING),
                ModuleHealth(required.id, ModuleState.RUNNING)
            )
        )
        assertEquals(SystemReadiness.BLOCKED, report.readiness)
        assertTrue(report.catalogErrors.contains("Duplicate module health records."))
    }

    @Test fun coldSystemIsNotReady() {
        val report = ManaSystemReadinessEvaluator.evaluate(BootPhase.COLD, listOf(required),
            listOf(ModuleHealth(required.id, ModuleState.REGISTERED)))
        assertEquals(SystemReadiness.BLOCKED, report.readiness)
    }
}
