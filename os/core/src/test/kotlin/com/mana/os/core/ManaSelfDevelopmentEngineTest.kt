package com.mana.os.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaSelfDevelopmentEngineTest {
    private class FakeHost(
        private val validation: SelfDevelopmentValidation = SelfDevelopmentValidation(true, true, true, true, "ok"),
        private val approved: Boolean = true,
        private val signature: Boolean = true,
        private val rollback: Boolean = true
    ) : ManaSelfDevelopmentHost {
        var installed = false
        var rebootScheduled = false
        override fun analyze(request: SelfDevelopmentRequest) = "missing module identified"
        override fun generatePatch(request: SelfDevelopmentRequest, analysis: String) =
            GeneratedChangeSet("patch-1", setOf("os/core/src/NewModule.kt"), "diff --git ...")
        override fun validateInIsolation(request: SelfDevelopmentRequest, changes: GeneratedChangeSet) = validation
        override fun stageSignedArtifact(request: SelfDevelopmentRequest, changes: GeneratedChangeSet) =
            StagedSelfDevelopmentArtifact("artifact-1", "4.0.1", "a".repeat(64), signature, rollback)
        override fun installStagedArtifact(artifact: StagedSelfDevelopmentArtifact): Boolean {
            installed = true
            return true
        }
        override fun requestOwnerApproval(request: SelfDevelopmentRequest, artifact: StagedSelfDevelopmentArtifact) = approved
        override fun scheduleRebootAfterApproval(): Boolean {
            rebootScheduled = true
            return true
        }
    }

    private fun request(scope: ChangeScope) = SelfDevelopmentRequest(
        "task-1", "Implement missing MANA feature", "RMX5566RU", "RMX5566_16.0.5.1310(EX01)", scope
    )

    @Test fun appModuleCanUpdateWithoutReboot() {
        val host = FakeHost()
        val result = ManaSelfDevelopmentEngine(host).develop(request(ChangeScope.APP_MODULE))
        assertEquals(SelfDevelopmentPhase.INSTALLED, result.phase)
        assertTrue(host.installed)
        assertFalse(host.rebootScheduled)
    }

    @Test fun protectedPlatformUpdateRequiresOwnerApprovalAndSchedulesReboot() {
        val host = FakeHost()
        val result = ManaSelfDevelopmentEngine(host).develop(request(ChangeScope.PROTECTED_PLATFORM))
        assertEquals(SelfDevelopmentPhase.REBOOT_PENDING, result.phase)
        assertTrue(host.installed)
        assertTrue(host.rebootScheduled)
    }

    @Test fun protectedPlatformUpdateWaitsWhenOwnerDoesNotApprove() {
        val host = FakeHost(approved = false)
        val result = ManaSelfDevelopmentEngine(host).develop(request(ChangeScope.PROTECTED_PLATFORM))
        assertEquals(SelfDevelopmentPhase.AWAITING_OWNER_APPROVAL, result.phase)
        assertFalse(host.installed)
        assertFalse(host.rebootScheduled)
    }

    @Test fun failedTestsRejectPatchBeforeStagingOrInstall() {
        val host = FakeHost(validation = SelfDevelopmentValidation(false, true, true, true, "unit tests failed"))
        val result = ManaSelfDevelopmentEngine(host).develop(request(ChangeScope.APP_MODULE))
        assertEquals(SelfDevelopmentPhase.REJECTED, result.phase)
        assertFalse(host.installed)
    }

    @Test fun unsignedOrNonRollbackableArtifactIsNeverInstalled() {
        val unsigned = FakeHost(signature = false)
        assertEquals(SelfDevelopmentPhase.FAILED,
            ManaSelfDevelopmentEngine(unsigned).develop(request(ChangeScope.APP_MODULE)).phase)
        assertFalse(unsigned.installed)

        val noRollback = FakeHost(rollback = false)
        assertEquals(SelfDevelopmentPhase.FAILED,
            ManaSelfDevelopmentEngine(noRollback).develop(request(ChangeScope.APP_MODULE)).phase)
        assertFalse(noRollback.installed)
    }

    @Test fun protectedPathTraversalIsRejectedBeforeValidation() {
        val host = object : FakeHost() {
            override fun generatePatch(request: SelfDevelopmentRequest, analysis: String) =
                GeneratedChangeSet("patch-evil", setOf("../../boot.img"), "bad diff")
        }
        val result = ManaSelfDevelopmentEngine(host).develop(request(ChangeScope.APP_MODULE))
        assertEquals(SelfDevelopmentPhase.FAILED, result.phase)
        assertFalse(host.installed)
    }
}
