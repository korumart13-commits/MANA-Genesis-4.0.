package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ManaEngineeringAutomationTest {
    private class FakeProvider : InferenceProvider {
        override val id = "test-local"
        var calls = 0
        override fun isAvailable() = true
        override suspend fun generate(prompt: String): String {
            calls++
            return """
                {
                  "summary":"Add a bounded test change (proposal $calls)",
                  "unifiedDiff":"diff --git a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt\\n--- a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt\\n+++ b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt\\n@@ -1 +1,2 @@\\n class Fixture\\n+// reviewed change",
                  "testPlan":["Run MANA Brain JVM tests"],
                  "riskNotes":["Review before merge"]
                }
            """.trimIndent()
        }
    }

    private class FakeGateway(private val failuresBeforeSuccess: Int) : GitHubActionsGateway {
        var dispatches = 0
        var logReads = 0
        override suspend fun dispatchPatch(
            branch: String,
            patch: String,
            expectedHeadSha: String
        ): GitHubWorkflowRun {
            assertEquals("chatgpt/mana-asi-rmx5566-one-action-setup", branch)
            assertTrue(patch.contains("diff --git"))
            assertEquals("a".repeat(40), expectedHeadSha)
            dispatches++
            return GitHubWorkflowRun(
                dispatches.toLong(), expectedHeadSha, "queued", null,
                "https://github.com/example/repo/actions/runs/$dispatches"
            )
        }

        override suspend fun awaitCompletion(run: GitHubWorkflowRun, timeoutMs: Long): GitHubWorkflowRun =
            run.copy(status = "completed", conclusion =
                if (run.id <= failuresBeforeSuccess) "failure" else "success"
            )

        override suspend fun failedJobLogs(runId: Long, maxChars: Int): String {
            logReads++
            return "Compilation failed in Fixture.kt: unresolved reference"
        }
    }

    private fun task() = EngineeringTask(
        id = "autofix-test",
        objective = "Add a harmless test fixture change",
        targetDevice = "RMX5566RU",
        targetBuild = "Android 16",
        sourceFiles = listOf(
            EngineeringSourceFile(
                "engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt",
                "class Fixture"
            )
        ),
        ownerApprovedContext = true,
        allowCloud = true
    )

    @Test fun dispatchesReadsFailureAndRetriesUntilCiPasses() = runBlocking {
        val provider = FakeProvider()
        val gateway = FakeGateway(failuresBeforeSuccess = 1)
        val automation = ManaEngineeringAutomation(
            ManaEngineeringCopilot(provider), gateway,
            "chatgpt/mana-asi-rmx5566-one-action-setup", maxRepairAttempts = 2
        )

        val result = automation.run(task(), "a".repeat(40))

        assertTrue(result.succeeded)
        assertEquals(2, result.attempts.size)
        assertEquals(2, gateway.dispatches)
        assertEquals(1, gateway.logReads)
        assertEquals(2, provider.calls)
        assertTrue(result.attempts.first().failureLogExcerpt!!.contains("Compilation failed"))
        assertTrue(result.finalRunUrl!!.endsWith("/2"))
    }

    @Test fun neverExceedsConfiguredRetryBudget() = runBlocking {
        val provider = FakeProvider()
        val gateway = FakeGateway(failuresBeforeSuccess = 10)
        val automation = ManaEngineeringAutomation(
            ManaEngineeringCopilot(provider), gateway,
            "chatgpt/mana-asi-rmx5566-one-action-setup", maxRepairAttempts = 2
        )

        val result = automation.run(task(), "a".repeat(40))

        assertFalse(result.succeeded)
        assertEquals(3, result.attempts.size)
        assertEquals(3, gateway.dispatches)
        assertEquals(3, gateway.logReads)
        assertEquals(3, provider.calls)
    }

    @Test fun requiresOwnerApprovalBeforeDispatch() = runBlocking {
        val provider = FakeProvider()
        val gateway = FakeGateway(failuresBeforeSuccess = 0)
        val automation = ManaEngineeringAutomation(
            ManaEngineeringCopilot(provider), gateway,
            "chatgpt/mana-asi-rmx5566-one-action-setup"
        )
        val unapproved = task().copy(ownerApprovedContext = false)

        kotlin.test.assertFailsWith<IllegalStateException> {
            automation.run(unapproved, "a".repeat(40))
        }
        assertEquals(0, gateway.dispatches)
    }
}
