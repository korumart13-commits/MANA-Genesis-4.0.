package com.mana.brain

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubActionsEngineeringGatewayTest {
    private val patch = """diff --git a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt
--- a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt
+++ b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt
@@ -1 +1,2 @@
 class Fixture
+// approved change
"""

    @Test fun dispatchesPollsAndCollectsSuccessfulRunAndArtifacts() = runBlocking {
        var receivedTaskId = ""
        var receivedPatch = ""
        val server = server { path, exchange ->
            when {
                path.endsWith("/dispatches") -> {
                    val payload = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                    receivedTaskId = Regex("\"task_id\":\"([^\"]+)\"").find(payload)!!.groupValues[1]
                    receivedPatch = Regex("\"patch\":\"([^\"]+)\"").find(payload)!!.groupValues[1]
                    respond(exchange, 204, "")
                }
                path.endsWith("/runs") -> respond(exchange, 200, runJson(receivedTaskId, "completed", "success"))
                path.endsWith("/jobs") -> respond(exchange, 200, """{"jobs":[{"id":7,"name":"validate-build-test","status":"completed","conclusion":"success"}]}""")
                path.endsWith("/artifacts") -> respond(exchange, 200, """{"artifacts":[{"id":91},{"id":92}]}""")
                else -> respond(exchange, 404, "{}")
            }
        }
        try {
            val result = gateway(server).dispatchAndAwait("test-task-1", patch)
            assertEquals("test-task-1", receivedTaskId)
            assertTrue(receivedPatch.contains("approved change"))
            assertTrue(result.successful)
            assertEquals(42L, result.runId)
            assertEquals(listOf(91L, 92L), result.artifactIds)
            assertEquals("abc123", result.headSha)
        } finally { server.stop(0) }
    }

    @Test fun downloadsFailedJobZipAndExtractsActionableDiagnostics() = runBlocking {
        val zip = logZip("e: unresolved reference: missingSymbol\nFAILURE: Build failed with an exception.")
        var taskId = ""
        val server = server { path, exchange ->
            when {
                path.endsWith("/dispatches") -> {
                    val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                    taskId = Regex("\"task_id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
                    respond(exchange, 204, "")
                }
                path.endsWith("/runs") -> respond(exchange, 200, runJson(taskId, "completed", "failure"))
                path.endsWith("/jobs") -> respond(exchange, 200, """{"jobs":[{"id":8,"name":"test","status":"completed","conclusion":"failure"}]}""")
                path.endsWith("/logs") -> {
                    exchange.responseHeaders.add("Content-Type", "application/zip")
                    exchange.sendResponseHeaders(200, zip.size.toLong())
                    exchange.responseBody.use { it.write(zip) }
                }
                path.endsWith("/artifacts") -> respond(exchange, 200, """{"artifacts":[]}""")
                else -> respond(exchange, 404, "{}")
            }
        }
        try {
            val result = gateway(server).dispatchAndAwait("failing-task", patch)
            assertFalse(result.successful)
            assertTrue(result.diagnostics.contains("unresolved reference"))
            assertTrue(result.jobs.single().logExcerpt.contains("Build failed"))
        } finally { server.stop(0) }
    }

    @Test fun repairLoopRetriesAtMostConfiguredCountAndStopsOnSuccess() = runBlocking {
        val taskIds = mutableListOf<String>()
        val fakeGateway = object : EngineeringCiGateway {
            override suspend fun dispatchAndAwait(taskId: String, patch: String): EngineeringCiRunResult {
                taskIds += taskId
                val success = taskIds.size == 2
                return EngineeringCiRunResult(
                    taskIds.size.toLong(), "https://example.invalid/run", "completed",
                    if (success) "success" else "failure", "sha",
                    emptyList(), emptyList(), if (success) "" else "Unresolved reference in Fixture.kt"
                )
            }
        }
        var repairs = 0
        val loop = EngineeringCiRepairLoop(fakeGateway, EngineeringPatchRepairer {
            repairs++
            patch
        }, maxRepairAttempts = 2)
        val result = loop.run(
            "repair-task", patch,
            setOf("engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt"),
            ownerApprovedCi = true
        )
        assertTrue(result.successful)
        assertEquals(1, repairs)
        assertEquals(2, result.runs.size)
        assertEquals(listOf("repair-task-attempt-1", "repair-task-attempt-2"), taskIds)
    }

    @Test fun repairLoopRequiresOwnerApprovalAndNeverExceedsRetryLimit() = runBlocking {
        var calls = 0
        val fakeGateway = object : EngineeringCiGateway {
            override suspend fun dispatchAndAwait(taskId: String, patch: String): EngineeringCiRunResult {
                calls++
                return EngineeringCiRunResult(calls.toLong(), "", "completed", "failure", "", emptyList(), emptyList(), "compiler error")
            }
        }
        val loop = EngineeringCiRepairLoop(fakeGateway, EngineeringPatchRepairer { patch }, maxRepairAttempts = 1)
        assertFailsWith<IllegalStateException> {
            loop.run("no-consent", patch, setOf("engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt"), false)
        }
        assertEquals(0, calls)
        val result = loop.run(
            "bounded", patch,
            setOf("engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt"),
            true
        )
        assertFalse(result.successful)
        assertEquals(2, calls)
        assertEquals(1, result.repairAttempts)
    }

    private fun gateway(server: HttpServer) = GitHubActionsEngineeringGateway(
        GitHubActionsConfig(
            "test-owner", "test-repo", "mana-engineering-pipeline.yml", "test-branch",
            apiBaseUrl = "http://127.0.0.1:${server.address.port}",
            pollIntervalMillis = 100,
            maxWaitMillis = 2_000
        ),
        GitHubTokenProvider { "ghp_abcdefghijklmnopqrstuvwxyz123456" }
    )

    private fun server(handler: (String, com.sun.net.httpserver.HttpExchange) -> Unit): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handler(exchange.requestURI.path + if (exchange.requestURI.rawQuery == null) "" else "?"+exchange.requestURI.rawQuery, exchange) }
        server.start()
        return server
    }

    private fun respond(exchange: com.sun.net.httpserver.HttpExchange, status: Int, body: String) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        if (status == 204) {
            exchange.sendResponseHeaders(204, -1)
        } else {
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

    private fun runJson(taskId: String, status: String, conclusion: String) = """
        {"workflow_runs":[{
          "id":42,"display_title":"MANA Engineering CI $taskId",
          "created_at":"${Instant.now()}","status":"$status","conclusion":"$conclusion",
          "html_url":"https://github.com/test-owner/test-repo/actions/runs/42",
          "head_sha":"abc123","jobs_url":"http://127.0.0.1/${"repos/test-owner/test-repo/actions/runs/42/jobs"}
        }]}
    """.trimIndent()

    private fun logZip(log: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("0_test.txt"))
            zip.write(log.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }
}
