package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class ManaEngineeringCopilotTest {
    private class FakeProvider(
        private val response: String,
        private val available: Boolean = true
    ) : InferenceProvider {
        override val id = "openai"
        var prompt: String? = null
        override fun isAvailable() = available
        override suspend fun generate(prompt: String): String {
            this.prompt = prompt
            return response
        }
    }

    private fun task(
        ownerApproved: Boolean = true,
        allowCloud: Boolean = true,
        path: String = "os/core/src/main/kotlin/com/mana/os/core/Example.kt",
        content: String = "class Example"
    ) = EngineeringTask(
        "task-1", "Add an example capability", "RMX5566RU", "Android 16",
        listOf(EngineeringSourceFile(path, content)), ownerApproved, allowCloud
    )

    private val validResponse = """{
        "summary":"Add a small example change",
        "unifiedDiff":"diff --git a/os/core/src/main/kotlin/com/mana/os/core/Example.kt b/os/core/src/main/kotlin/com/mana/os/core/Example.kt\n--- a/os/core/src/main/kotlin/com/mana/os/core/Example.kt\n+++ b/os/core/src/main/kotlin/com/mana/os/core/Example.kt\n@@ -1 +1,2 @@\n class Example\n+// reviewed proposal",
        "testPlan":["Run JVM unit tests"],
        "riskNotes":["Review API compatibility"]
    }"""

    @Test fun returnsProposalForReviewAndNeverClaimsItWasApplied() = runBlocking {
        val provider = FakeProvider(validResponse)
        val proposal = ManaEngineeringCopilot(provider).propose(task())
        assertEquals(EngineeringProposalStatus.REVIEW_REQUIRED, proposal.status)
        assertEquals(setOf("os/core/src/main/kotlin/com/mana/os/core/Example.kt"), proposal.changedPaths)
        assertTrue(proposal.testPlan.isNotEmpty())
    }

    @Test fun refusesCloudWithoutOwnerConsent() = runBlocking {
        val provider = FakeProvider(validResponse)
        assertFailsWith<IllegalStateException> {
            ManaEngineeringCopilot(provider).propose(task(ownerApproved = false))
        }
        assertEquals(null, provider.prompt)
    }

    @Test fun refusesCloudWhenTaskDoesNotAllowIt() = runBlocking {
        val provider = FakeProvider(validResponse)
        assertFailsWith<IllegalStateException> {
            ManaEngineeringCopilot(provider).propose(task(allowCloud = false))
        }
        assertEquals(null, provider.prompt)
    }

    @Test fun redactsSecretsBeforeSendingSourceContext() = runBlocking {
        val provider = FakeProvider(validResponse)
        ManaEngineeringCopilot(provider).propose(task(content = "api_key = \"sk-abcdefghijklmnopqrstuvwxyz123456\""))
        assertTrue(provider.prompt!!.contains("[REDACTED]"))
        assertTrue(!provider.prompt!!.contains("sk-abcdefghijklmnopqrstuvwxyz123456"))
    }

    @Test fun rejectsPathsOutsideApprovedWorkspace() = runBlocking {
        val provider = FakeProvider(validResponse)
        assertFailsWith<IllegalArgumentException> {
            ManaEngineeringCopilot(provider).propose(task(path = "kernel/evil.c"))
        }
        assertEquals(null, provider.prompt)
    }

    @Test fun rejectsPathTraversalInGeneratedDiff() = runBlocking {
        val response = validResponse.replace(
            "os/core/src/main/kotlin/com/mana/os/core/Example.kt",
            "../../etc/passwd"
        )
        assertFailsWith<IllegalArgumentException> {
            ManaEngineeringCopilot(FakeProvider(response)).propose(task())
        }
    }

    @Test fun rejectsProposalWithoutTestPlan() = runBlocking {
        val response = validResponse.replace("\"testPlan\":[\"Run JVM unit tests\"]", "\"testPlan\":[]")
        assertFailsWith<IllegalArgumentException> {
            ManaEngineeringCopilot(FakeProvider(response)).propose(task())
        }
    }

    @Test fun rejectsChangesToFilesNotExplicitlySelectedForTheTask() = runBlocking {
        val response = validResponse.replace(
            "os/core/src/main/kotlin/com/mana/os/core/Example.kt",
            "docs/unselected-file.md"
        )
        assertFailsWith<IllegalArgumentException> {
            ManaEngineeringCopilot(FakeProvider(response)).propose(task())
        }
    }

    @Test fun refusesUnavailableProvider() = runBlocking {
        assertFailsWith<IllegalStateException> {
            ManaEngineeringCopilot(FakeProvider(validResponse, available = false)).propose(task())
        }
    }

    @Test fun permitsReviewOnlyPlatformServiceProposals() = runBlocking {
        val response = validResponse.replace(
            "os/core/src/main/kotlin/com/mana/os/core/Example.kt",
            "system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java"
        )
        val proposal = ManaEngineeringCopilot(FakeProvider(response)).propose(task())
        assertTrue(proposal.changedPaths.all { it.startsWith("system/mana/") })
        assertEquals(EngineeringProposalStatus.REVIEW_REQUIRED, proposal.status)
    }

    @Test fun rejectsDeviceAndKernelSourceTargets() = runBlocking {
        for (blocked in listOf("device/realme/RMX5566/device.mk", "kernel/arch/arm64/configs/defconfig")) {
            assertFailsWith<IllegalArgumentException> {
                ManaEngineeringCopilot(FakeProvider(validResponse)).propose(task(path = blocked))
            }
        }
    }
}
