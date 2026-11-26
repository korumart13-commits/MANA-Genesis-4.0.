package com.mana.brain

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManaSelfDevelopmentTest {
    private val engineDiff = """
        diff --git a/engine/mana-brain/src/main/kotlin/com/mana/brain/NewSkill.kt b/engine/mana-brain/src/main/kotlin/com/mana/brain/NewSkill.kt
        new file mode 100644
        --- /dev/null
        +++ b/engine/mana-brain/src/main/kotlin/com/mana/brain/NewSkill.kt
        @@ -0,0 +1,1 @@
        +package com.mana.brain
    """.trimIndent()

    @Test fun engineChangesEnterIsolatedTesting() {
        val result = ManaSelfDevelopmentPolicy().evaluate(DevelopmentTrack.ENGINE,
            ProposedCodePatch(engineDiff, listOf("engine/mana-brain/src/main/kotlin/com/mana/brain/NewSkill.kt")))
        assertEquals(DevelopmentDisposition.ACCEPT_FOR_TESTING, result.disposition)
    }

    @Test fun buildScriptsAndWorkflowsAreRejected() {
        val policy = ManaSelfDevelopmentPolicy()
        for (path in listOf("engine/mana-brain/build.gradle.kts", ".github/workflows/release.yml")) {
            assertEquals(DevelopmentDisposition.REJECT,
                policy.evaluate(DevelopmentTrack.ENGINE, ProposedCodePatch(engineDiff, listOf(path))).disposition)
        }
    }

    @Test fun kernelTrackIsReviewOnlyWithoutExactOemSource() {
        val result = ManaSelfDevelopmentPolicy(false).evaluate(DevelopmentTrack.KERNEL_PATCHSET,
            ProposedCodePatch("diff", listOf("kernel/mana-patches/0001-mana.patch")))
        assertEquals(DevelopmentDisposition.ACCEPT_FOR_REVIEW_ONLY, result.disposition)
    }

    @Test fun unsafePathsAndBinaryChangesAreRejected() {
        val policy = ManaSelfDevelopmentPolicy()
        assertEquals(DevelopmentDisposition.REJECT,
            policy.evaluate(DevelopmentTrack.ENGINE, ProposedCodePatch("diff", listOf("../secret.kt"))).disposition)
        assertEquals(DevelopmentDisposition.REJECT,
            policy.evaluate(DevelopmentTrack.ENGINE, ProposedCodePatch("diff",
                listOf("engine/mana-brain/src/main/kotlin/A.kt"), containsBinaryChanges = true)).disposition)
    }

    @Test fun brainGeneratedPatchPassesPolicyBeforeWorkspace() = runBlocking {
        val local = object : InferenceProvider {
            override val id = "local-test"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String) =
                "Change prepared.\n" + 96.toChar().toString().repeat(3) + "diff\n" +
                    engineDiff + "\n" + 96.toChar().toString().repeat(3)
        }
        var called = false
        val workspace = ManaCandidateWorkspace { track, patch ->
            called = true
            assertEquals(DevelopmentTrack.ENGINE, track)
            assertTrue(patch.changedPaths.single().endsWith("NewSkill.kt"))
            CandidateRunResult(true, "mana-candidate-engine-test", "abc123", "All tests passed")
        }
        val coordinator = ManaSelfDevelopmentCoordinator(ManaBrain(local), workspace)
        val (candidate, result) = coordinator.develop("Add a safe engine skill",
            DevelopmentTrack.ENGINE, "Existing engine API excerpt")
        assertNotNull(candidate)
        assertNotNull(result)
        assertTrue(called)
        assertTrue(result.accepted)
    }

    @Test fun invalidGeneratedPathNeverReachesWorkspace() = runBlocking {
        val local = object : InferenceProvider {
            override val id = "local-test"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String) =
                96.toChar().toString().repeat(3) + "diff\ndiff --git a/.github/workflows/x.yml b/.github/workflows/x.yml\n" +
                    "--- a/x\n+++ b/x\n" + 96.toChar().toString().repeat(3)
        }
        var called = false
        val coordinator = ManaSelfDevelopmentCoordinator(ManaBrain(local),
            ManaCandidateWorkspace { _, _ ->
                called = true
                CandidateRunResult(true, "bad", "bad", "should not run")
            })
        val (candidate, result) = coordinator.develop("Modify workflow",
            DevelopmentTrack.ENGINE, "context")
        assertNotNull(candidate)
        assertEquals(DevelopmentDisposition.REJECT, candidate.admission.disposition)
        assertFalse(called)
        assertNotNull(result)
        assertFalse(result.accepted)
    }
}
