package com.mana.brain

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

/** Supplies a short-lived GitHub token from Android Keystore-backed storage.
 * Never persist a token in source, preferences in plaintext, logs or workflow inputs.
 */
fun interface GitHubTokenProvider {
    fun accessToken(): String?
}

data class GitHubWorkflowRun(
    val id: Long,
    val headSha: String,
    val status: String,
    val conclusion: String?,
    val htmlUrl: String
) {
    val completed: Boolean get() = status == "completed"
    val succeeded: Boolean get() = completed && conclusion == "success"
}

data class GitHubWorkflowJob(
    val id: Long,
    val name: String,
    val conclusion: String?
)

interface GitHubActionsGateway {
    suspend fun dispatchPatch(branch: String, patch: String, expectedHeadSha: String): GitHubWorkflowRun
    suspend fun awaitCompletion(run: GitHubWorkflowRun, timeoutMs: Long = 240_000): GitHubWorkflowRun
    suspend fun failedJobLogs(runId: Long, maxChars: Int = 40_000): String
}

/** GitHub REST implementation for workflow_dispatch, run polling and job logs. */
class GitHubActionsRestClient(
    private val repository: String,
    private val workflowFile: String,
    private val tokenProvider: GitHubTokenProvider,
    private val pollIntervalMs: Long = 3_000,
    private val dispatchTimeoutMs: Long = 120_000
) : GitHubActionsGateway {
    init {
        require(repository.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")))
        require(workflowFile.matches(Regex("[A-Za-z0-9_.-]+")))
        require(pollIntervalMs in 250..30_000)
        require(dispatchTimeoutMs in 5_000..600_000)
    }

    override suspend fun dispatchPatch(
        branch: String,
        patch: String,
        expectedHeadSha: String
    ): GitHubWorkflowRun = withContext(Dispatchers.IO) {
        require(branch.matches(Regex("[A-Za-z0-9._/-]{1,200}")) && !branch.contains(".."))
        require(expectedHeadSha.matches(Regex("[a-fA-F0-9]{40}")))
        require(patch.isNotBlank() && patch.length <= MAX_WORKFLOW_PATCH_CHARS)
        EngineeringPatchPolicy.validate(patch)

        val dispatchedAt = Instant.now()
        val payload = JsonObject().apply {
            addProperty("ref", branch)
            add("inputs", JsonObject().apply { addProperty("patch", patch) })
        }
        request("POST", "actions/workflows/$workflowFile/dispatches", payload.toString(), expected = setOf(204))
        val deadline = System.currentTimeMillis() + dispatchTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            val root = JsonParser.parseString(
                request("GET", "actions/workflows/$workflowFile/runs?branch=$branch&event=workflow_dispatch&per_page=20")
            ).asJsonObject
            val runs = root.getAsJsonArray("workflow_runs")
            val match = runs.firstOrNull { element ->
                val item = element.asJsonObject
                item.get("head_sha")?.asString == expectedHeadSha &&
                    item.get("event")?.asString == "workflow_dispatch" &&
                    runCreatedAt(item) >= dispatchedAt.minusSeconds(5)
            }?.asJsonObject
            if (match != null) return@withContext match.toRun()
            delay(pollIntervalMs)
        }
        throw IllegalStateException("GitHub accepted workflow_dispatch but no matching run appeared before timeout.")
    }

    override suspend fun awaitCompletion(run: GitHubWorkflowRun, timeoutMs: Long): GitHubWorkflowRun =
        withContext(Dispatchers.IO) {
            require(timeoutMs in 5_000..900_000)
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                val item = JsonParser.parseString(request("GET", "actions/runs/${run.id}")).asJsonObject
                val current = item.toRun()
                if (current.completed) return@withContext current
                delay(pollIntervalMs)
            }
            throw IllegalStateException("GitHub Actions run ${run.id} did not complete before timeout.")
        }
    override suspend fun failedJobLogs(runId: Long, maxChars: Int): String = withContext(Dispatchers.IO) {
        require(maxChars in 1_000..100_000)
        val root = JsonParser.parseString(request("GET", "actions/runs/$runId/jobs?per_page=50")).asJsonObject
        val jobs = root.getAsJsonArray("jobs")
        val output = StringBuilder()
        jobs.filter { element ->
            val item = element.asJsonObject
            item.get("conclusion")?.asString == "failure"
        }.forEach { element ->
            val job = element.asJsonObject
            val id = job.get("id").asLong
            val name = job.get("name").asString
            output.appendLine("## Failed job: $name (id=$id)")
            val logs = request("GET", "actions/jobs/$id/logs", followRedirect = true)
            output.append(logs.take((maxChars - output.length).coerceAtLeast(0)))
            output.appendLine()
            if (output.length >= maxChars) return@forEach
        }
        output.toString().take(maxChars)
    }

    private fun request(
        method: String,
        pathAndQuery: String,
        body: String? = null,
        expected: Set<Int> = setOf(200)
    ): String {
        val token = tokenProvider.accessToken()?.trim().orEmpty()
        require(token.isNotBlank()) { "GitHub Actions is not connected: no access token is available." }
        var url = URL("$API_BASE/$pathAndQuery")
        var authenticated = true
        repeat(3) {
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 15_000
                readTimeout = 90_000
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", API_VERSION)
                if (authenticated) setRequestProperty("Authorization", "Bearer $token")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }
            try {
                if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                if (status in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IllegalStateException("GitHub returned a redirect without a location.")
                    url = URL(url, location)
                    authenticated = false // Never forward the GitHub token to a signed artifact-log URL.
                    return@repeat
                }
                if (status !in expected) {
                    throw IllegalStateException("GitHub API request failed with HTTP " + status + " for " + pathAndQuery + ".")
                }
                return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException("GitHub API redirect limit exceeded for " + pathAndQuery + ".")
    }
    private fun runCreatedAt(item: com.google.gson.JsonObject): Instant =
        Instant.parse(item.get("created_at").asString)

    private fun com.google.gson.JsonObject.toRun() = GitHubWorkflowRun(
        id = get("id").asLong,
        headSha = get("head_sha").asString,
        status = get("status").asString,
        conclusion = get("conclusion")?.takeUnless { it.isJsonNull }?.asString,
        htmlUrl = get("html_url")?.asString.orEmpty()
    )

    companion object {
        const val MAX_WORKFLOW_PATCH_CHARS = 60_000
        private const val API_BASE = "https://api.github.com/repos"
        private const val API_VERSION = "2022-11-28"
    }
}

data class EngineeringAttempt(
    val attempt: Int,
    val run: GitHubWorkflowRun,
    val patchSummary: String,
    val failureLogExcerpt: String? = null
)

data class EngineeringAutomationResult(
    val succeeded: Boolean,
    val attempts: List<EngineeringAttempt>,
    val finalRunUrl: String?
)

/**
 * Bounded repair loop. Every candidate remains a review-only proposal; CI applies it
 * only in its disposable checkout. Nothing is committed, merged, installed or flashed.
 */
class ManaEngineeringAutomation(
    private val copilot: ManaEngineeringCopilot,
    private val github: GitHubActionsGateway,
    private val branch: String,
    private val maxRepairAttempts: Int = 2,
    private val failureContextChars: Int = 12_000
) {
    init {
        require(maxRepairAttempts in 0..2) { "Autonomous repair is deliberately capped at two retries." }
        require(failureContextChars in 1_000..20_000)
    }

    suspend fun run(
        task: EngineeringTask,
        expectedHeadSha: String
    ): EngineeringAutomationResult {
        check(task.ownerApprovedContext) { "Owner approval is required for an engineering task." }
        require(expectedHeadSha.matches(Regex("[a-fA-F0-9]{40}")))
        val attempts = mutableListOf<EngineeringAttempt>()
        var objective = task.objective
        var latestRun: GitHubWorkflowRun? = null

        for (attempt in 0..maxRepairAttempts) {
            val currentTask = task.copy(
                id = "${task.id}-ci-${attempt + 1}",
                objective = objective.take(6_000)
            )
            val proposal = copilot.propose(currentTask)
            check(proposal.status == EngineeringProposalStatus.REVIEW_REQUIRED)
            val dispatched = github.dispatchPatch(branch, proposal.unifiedDiff, expectedHeadSha)
            val completed = github.awaitCompletion(dispatched)
            latestRun = completed
            if (completed.succeeded) {
                attempts += EngineeringAttempt(attempt + 1, completed, proposal.summary)
                return EngineeringAutomationResult(true, attempts, completed.htmlUrl)
            }
            val logs = github.failedJobLogs(completed.id, failureContextChars)
            attempts += EngineeringAttempt(
                attempt + 1, completed, proposal.summary,
                logs.take(failureContextChars)
            )
            if (attempt == maxRepairAttempts || logs.isBlank()) break
            objective = buildString {
                appendLine(task.objective)
                appendLine()
                appendLine("The previous candidate failed in GitHub Actions. Diagnose the failure from the CI evidence below.")
                appendLine("Generate a corrected complete unified diff against the original selected source files.")
                appendLine("Do not claim the failure is fixed until a subsequent CI run passes.")
                appendLine("Previous CI failure evidence:")
                append(logs.take(failureContextChars))
            }.take(6_000)
        }
        return EngineeringAutomationResult(false, attempts, latestRun?.htmlUrl)
    }
}
