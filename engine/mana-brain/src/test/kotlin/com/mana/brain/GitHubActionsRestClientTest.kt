package com.mana.brain

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubActionsRestClientTest {
    private val head = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    private val patch = """diff --git a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt
--- a/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt
+++ b/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt
@@ -1 +1,2 @@
 class Fixture
+// test change
"""

    @Test fun dispatchesAndPollsTheCorrelatedWorkflowRun() = runBlocking {
        var receivedRunId = ""
        var receivedPatch = ""
        val server = startServer { path, exchange ->
            when {
                path.endsWith("/branches/test-branch") -> respond(exchange, 200, """{"commit":{"sha":"$head"}}""")
                path.contains("/contents/engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt") -> {
                    val encoded = java.util.Base64.getEncoder().encodeToString("class Fixture".toByteArray(StandardCharsets.UTF_8))
                    respond(exchange, 200, """{"encoding":"base64","content":"$encoded"}""")
                }
                path.endsWith("/dispatches") -> {
                    val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                    receivedRunId = Regex("\"client_run_id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
                    receivedPatch = Regex("\"patch\":\"([^\"]+)\"").find(body)!!.groupValues[1]
                    respond(exchange, 204, "")
                }
                path.endsWith("/runs") -> respond(exchange, 200, runList(receivedRunId, "queued", null))
                path.endsWith("/actions/runs/42") -> respond(exchange, 200, runObject("completed", "success"))
                else -> respond(exchange, 404, "{}")
            }
        }
        try {
            val client = client(server)
            assertEquals(head, client.currentHeadSha("test-branch"))
            val source = client.fetchRepositoryFile("test-branch", "engine/mana-brain/src/main/kotlin/com/mana/brain/Fixture.kt")
            assertEquals("class Fixture", source.content)
            val dispatched = client.dispatchPatch("test-branch", patch, head, "run-test-1")
            assertEquals(42L, dispatched.id)
            assertEquals("run-test-1", receivedRunId)
            assertTrue(receivedPatch.contains("test change"))
            val completed = client.awaitCompletion(dispatched, timeoutMs = 5_000)
            assertTrue(completed.succeeded)
            assertEquals(head, completed.headSha)
        } finally { server.stop(0) }
    }

    @Test fun downloadsZipLogsAndReturnsBoundedFailureEvidence() = runBlocking {
        val zipBytes = logZip("e: unresolved reference: missingSymbol\nFAILURE: Build failed with an exception.")
        var receivedRunId = ""
        val server = startServer { path, exchange ->
            when {
                path.endsWith("/dispatches") -> {
                    val body = exchange.requestBody.readBytes().toString(StandardCharsets.UTF_8)
                    receivedRunId = Regex("\"client_run_id\":\"([^\"]+)\"").find(body)!!.groupValues[1]
                    respond(exchange, 204, "")
                }
                path.endsWith("/runs") -> respond(exchange, 200, runList(receivedRunId, "completed", "failure"))
                path.endsWith("/actions/runs/42") -> respond(exchange, 200, runObject("completed", "failure"))
                path.endsWith("/actions/runs/42/jobs") -> respond(exchange, 200, """{"jobs":[{"id":7,"name":"gradle-test","conclusion":"failure"}]}""")
                path.endsWith("/actions/jobs/7/logs") -> {
                    exchange.responseHeaders.add("Location", "http://127.0.0.1:${exchange.localAddress.port}/blob/log.zip")
                    exchange.sendResponseHeaders(302, -1)
                    exchange.close()
                }
                path == "/blob/log.zip" -> {
                    exchange.responseHeaders.add("Content-Type", "application/zip")
                    exchange.sendResponseHeaders(200, zipBytes.size.toLong())
                    exchange.responseBody.use { it.write(zipBytes) }
                }
                else -> respond(exchange, 404, "{}")
            }
        }
        try {
            val client = client(server)
            val run = client.dispatchPatch("test-branch", patch, head, "run-log-test")
            val completed = client.awaitCompletion(run, timeoutMs = 5_000)
            assertEquals("failure", completed.conclusion)
            val logs = client.failedJobLogs(completed.id, maxChars = 4_000)
            assertTrue(logs.contains("unresolved reference"))
            assertTrue(logs.contains("Build failed"))
            assertTrue(logs.length <= 4_000)
        } finally { server.stop(0) }
    }

    private fun client(server: HttpServer) = GitHubActionsRestClient(
        repository = "test-owner/test-repo",
        workflowFile = "mana-engineering-pipeline.yml",
        tokenProvider = GitHubTokenProvider { "github_pat_abcdefghijklmnopqrstuvwxyz123456" },
        pollIntervalMs = 250,
        dispatchTimeoutMs = 5_000,
        apiBaseUrl = "http://127.0.0.1:${server.address.port}/repos"
    )

    private fun startServer(handler: (String, HttpExchange) -> Unit): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handler(exchange.requestURI.path, exchange) }
        server.start()
        return server
    }

    private fun respond(exchange: HttpExchange, status: Int, body: String) {
        if (status == 204) {
            exchange.sendResponseHeaders(204, -1)
            exchange.close()
            return
        }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun runList(runId: String, status: String, conclusion: String?) = """
        {"workflow_runs":[{
          "id":42,"display_title":"MANA Engineering CI $runId",
          "event":"workflow_dispatch","head_sha":"$head",
          "created_at":"${Instant.now()}","status":"$status",
          "conclusion":${conclusion?.let { "\"$it\"" } ?: "null"},
          "html_url":"https://github.com/test-owner/test-repo/actions/runs/42"
        }]}
    """.trimIndent()

    private fun runObject(status: String, conclusion: String) = """
        {"id":42,"head_sha":"$head","status":"$status","conclusion":"$conclusion",
         "html_url":"https://github.com/test-owner/test-repo/actions/runs/42"}
    """.trimIndent()

    private fun logZip(log: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry("0_gradle-test.txt"))
            zip.write(log.toByteArray(StandardCharsets.UTF_8))
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }
}
