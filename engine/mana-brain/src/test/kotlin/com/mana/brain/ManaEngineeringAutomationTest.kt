package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ManaEngineeringAutomationTest {
    private val sourcePath = "engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt"
    private val head = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"

    private class FakeProvider : InferenceProvider {
        override val id = "test-local"
        var calls = 0
        val prompts = mutableListOf<String>()
        override fun isAvailable() = true
        override suspend fun generate(prompt: String): String {
            calls++
            prompts += prompt
            return """
                {
                  "summary":"Add a bounded test change (proposal $calls)",
                  "unifiedDiff":"diff --git a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt\n--- a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt\n+++ b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt\n@@ -1 +1,2 @@\n class Fixture\n+// reviewed change",
                  "testPlan":["Run MANA Brain JVM tests"],
                  "riskNotes":["Review before merge"]
                }
            """.trimIndent()
        }
    }

    private class FakeGateway(private val failuresBeforeSuccess: Int) : GitHubActionsGateway {
        var dispatches = 0
        var logReads = 0
        val clientRunIds = mutableListOf<String>()
        override suspend fun currentHeadSha(branch: String): String = headSha()
        override suspend fun dispatchPatch(
            branch: String,
            patch: String,
            expectedHeadSha: String,
            clientRunId: String
        ): GitHubWorkflowRun {
            assertEquals("chatgpt/mana-asi-rmx5566-one-action-setup", branch)
            assertTrue(patch.contains("diff --git"))
            assertEquals(headSha(), expectedHeadSha)
            dispatches++
            clientRunIds += clientRunId
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
            return "Compilation failed in Fixture.kt: unresolved reference".take(maxChars)
        }

        private fun headSha() = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    }

    private fun task(approved: Boolean = true) = EngineeringTask(
        id = "autofix-test",
        objective = "Add a harmless test fixture change",
        targetDevice = "RMX5566RU",
        targetBuild = "Android 16",
        sourceFiles = listOf(EngineeringSourceFile(sourcePath, "class Fixture")),
        ownerApprovedContext = approved,
        allowCloud = true
    )

    @Test fun dispatchesReadsFailureAndRetriesUntilCiPasses() = runBlocking {
        val provider = FakeProvider()
        val gateway = FakeGateway(failuresBeforeSuccess = 1)
        val automation = ManaEngineeringAutomation(
            ManaEngineeringCopilot(provider), gateway,
            "chatgpt/mana-asi-rmx5566-one-action-setup", maxRepairAttempts = 2
        )

        val result = automation.run(task(), head)

        assertTrue(result.succeeded)
        assertEquals(2, result.attempts.size)
        assertEquals(2, gateway.dispatches)
        assertEquals(1, gateway.logReads)
        assertEquals(2, provider.calls)
        assertTrue(provider.prompts[1].contains("Compilation failed"))
        assertTrue(provider.prompts[1].contains("Previous candidate diff"))
        assertTrue(result.attempts.first().failureLogExcerpt!!.contains("Compilation failed"))
        assertTrue(result.finalRunUrl!!.endsWith("/2"))
        assertEquals(2, gateway.clientRunIds.distinct().size)
    }

    @Test fun neverExceedsConfiguredRetryBudget() = runBlocking {
        val provider = FakeProvider()
        val gateway = FakeGateway(failuresBeforeSuccess = 10)
        val automation = ManaEngineeringAutomation(
            ManaEngineeringCopilot(provider), gateway,
            "chatgpt/mana-asi-rmx5566-one-action-setup", maxRepairAttempts = 2
        )

        val result = automation.run(task(), head)

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
        assertFailsWith<IllegalStateException> {
            automation.run(task(approved = false), head)
        }
        assertEquals(0, gateway.dispatches)
    }

    @Test fun abortsRepairWhenBranchHeadChangesAfterFailedAttempt() = runBlocking {
        val provider = FakeProvider()
        var headReads = 0
        var dispatches = 0
        val gateway = object : GitHubActionsGateway {
            override suspend fun currentHeadSha(branch: String): String {
                headReads++
                return if (headReads <= 2) head else "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
            }
            override suspend fun dispatchPatch(branch: String, patch: String, expectedHeadSha: String, clientRunId: String): GitHubWorkflowRun {
                dispatches++
                return GitHubWorkflowRun(77, expectedHeadSha, "queued", null, "https://github.com/example/repo/actions/runs/77")
            }
            override suspend fun awaitCompletion(run: GitHubWorkflowRun, timeoutMs: Long) =
                run.copy(status = "completed", conclusion = "failure")
            override suspend fun failedJobLogs(runId: Long, maxChars: Int) =
                "Compilation failed".take(maxChars)
        }

        assertFailsWith<IllegalStateException> {
            ManaEngineeringAutomation(
                ManaEngineeringCopilot(provider), gateway,
                "chatgpt/mana-asi-rmx5566-one-action-setup", maxRepairAttempts = 2
            ).run(task(), head)
        }
        assertEquals(1, dispatches)
        assertEquals(1, provider.calls)
        assertEquals(3, headReads)
    }

    @Test fun rejectsStaleBranchBeforeDispatch() = runBlocking {
        val provider = FakeProvider()
        val gateway = object : GitHubActionsGateway {
            override suspend fun currentHeadSha(branch: String) = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
            override suspend fun dispatchPatch(branch: String, patch: String, expectedHeadSha: String, clientRunId: String) =
                error("Stale branch must not dispatch.")
            override suspend fun awaitCompletion(run: GitHubWorkflowRun, timeoutMs: Long) = run
            override suspend fun failedJobLogs(runId: Long, maxChars: Int) = ""
        }
        assertFailsWith<IllegalStateException> {
            ManaEngineeringAutomation(
                ManaEngineeringCopilot(provider), gateway,
                "chatgpt/mana-asi-rmx5566-one-action-setup"
            ).run(task(), head)
        }
        assertEquals(0, provider.calls)
    }
}
