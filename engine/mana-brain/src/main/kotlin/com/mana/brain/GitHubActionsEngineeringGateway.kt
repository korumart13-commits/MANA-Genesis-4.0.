package com.mana.brain

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.zip.ZipInputStream

/** Supplies a narrowly-scoped GitHub token. Android hosts should back this with Android Keystore. */
fun interface GitHubTokenProvider {
    fun accessToken(): String
}

data class GitHubActionsConfig(
    val owner: String,
    val repository: String,
    val workflowFile: String,
    val branch: String,
    val apiBaseUrl: String = "https://api.github.com",
    val pollIntervalMillis: Long = 2_000,
    val maxWaitMillis: Long = 10 * 60 * 1_000,
    val maxLogChars: Int = 24_000
) {
    init {
        require(owner.matches(Regex("[A-Za-z0-9_.-]{1,100}")))
        require(repository.matches(Regex("[A-Za-z0-9_.-]{1,100}")))
        require(workflowFile.matches(Regex("[A-Za-z0-9_.-]{1,120}")))
        require(branch.isNotBlank() && branch.length <= 200 && !branch.contains(".."))
        require(apiBaseUrl.startsWith("https://") || apiBaseUrl.startsWith("http://127.0.0.1"))
        require(pollIntervalMillis in 100..60_000)
        require(maxWaitMillis in pollIntervalMillis..(30 * 60 * 1_000))
        require(maxLogChars in 1_000..100_000)
    }
}

data class EngineeringCiJob(
    val id: Long,
    val name: String,
    val status: String,
    val conclusion: String?,
    val logExcerpt: String = ""
)

data class EngineeringCiRunResult(
    val runId: Long,
    val runUrl: String,
    val status: String,
    val conclusion: String?,
    val headSha: String,
    val jobs: List<EngineeringCiJob>,
    val artifactIds: List<Long>,
    val diagnostics: String
) {
    val successful: Boolean get() = status == "completed" && conclusion == "success"
}

interface EngineeringCiGateway {
    suspend fun dispatchAndAwait(taskId: String, patch: String): EngineeringCiRunResult
}

/**
 * Real GitHub Actions integration. Requires a fine-grained token with Actions: read/write
 * on this repository. It dispatches the workflow, correlates the run by its unique run-name,
 * polls to completion, retrieves job summaries and downloads failed-job log ZIPs.
 */
class GitHubActionsEngineeringGateway(
    private val config: GitHubActionsConfig,
    private val tokenProvider: GitHubTokenProvider,
    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
) : EngineeringCiGateway {
    private val repositoryPath = "/repos/${config.owner}/${config.repository}"

    override suspend fun dispatchAndAwait(taskId: String, patch: String): EngineeringCiRunResult {
        require(taskId.matches(Regex("[A-Za-z0-9._-]{1,70}"))) { "Invalid CI task identifier." }
        val validation = EngineeringPatchPolicy.validate(patch)
        require(validation.changedPaths.size <= EngineeringPatchPolicy.MAX_FILES)
        require(patch.length <= 60_000) { "Patch exceeds GitHub workflow input limit." }

        val startedAt = Instant.now()
        val payload = JsonObject().apply {
            addProperty("ref", config.branch)
            add("inputs", JsonObject().apply {
                addProperty("task_id", taskId)
                addProperty("patch", patch)
            })
        }
        val dispatchPath = "$repositoryPath/actions/workflows/${config.workflowFile}/dispatches"
        val dispatchResponse = request("POST", dispatchPath, payload.toString())
        check(dispatchResponse.statusCode() == 204) {
            "GitHub workflow dispatch failed with HTTP ${dispatchResponse.statusCode()}."
        }

        val run = awaitMatchingRun(taskId, startedAt)
        val jobs = fetchJobs(run)
        val enrichedJobs = jobs.map { job ->
            if (job.conclusion == "failure" || job.conclusion == "cancelled") {
                job.copy(logExcerpt = fetchJobLogExcerpt(job.id))
            } else job
        }
        val artifacts = fetchArtifactIds(run.get("id").asLong)
        val diagnostics = buildDiagnostics(enrichedJobs)
        return EngineeringCiRunResult(
            run.get("id").asLong,
            run.get("html_url")?.asString.orEmpty(),
            run.get("status")?.asString.orEmpty(),
            run.get("conclusion")?.takeUnless { it.isJsonNull }?.asString,
            run.get("head_sha")?.asString.orEmpty(),
            enrichedJobs,
            artifacts,
            diagnostics
        )
    }

    private suspend fun awaitMatchingRun(taskId: String, startedAt: Instant): JsonObject {
        val deadline = System.nanoTime() + config.maxWaitMillis * 1_000_000
        var found: JsonObject? = null
        while (System.nanoTime() < deadline) {
            val query = "$repositoryPath/actions/workflows/${config.workflowFile}/runs?event=workflow_dispatch&branch=${encode(config.branch)}&per_page=30"
            val root = getJson(query)
            val runs = root.getAsJsonArray("workflow_runs")
            found = runs.firstOrNull { element ->
                val candidate = element.asJsonObject
                val created = runCatching { Instant.parse(candidate.get("created_at").asString) }.getOrNull()
                val title = candidate.get("display_title")?.asString.orEmpty()
                created != null && !created.isBefore(startedAt.minusSeconds(30)) &&
                    title.contains(taskId)
            }?.asJsonObject
            if (found != null) {
                val status = found.get("status")?.asString
                if (status == "completed") return found
            }
            delay(config.pollIntervalMillis)
        }
        throw IllegalStateException(
            if (found == null) "Timed out waiting for the dispatched MANA workflow run."
            else "Timed out waiting for MANA workflow run ${found.get("id").asLong} to complete."
        )
    }

    private suspend fun fetchJobs(run: JsonObject): List<EngineeringCiJob> {
        val jobsUrl = run.get("jobs_url")?.asString
            ?: "$repositoryPath/actions/runs/${run.get("id").asLong}/jobs"
        val path = jobsUrl.removePrefix(config.apiBaseUrl)
        val root = getJson(path)
        return root.getAsJsonArray("jobs").map { element ->
            val item = element.asJsonObject
            EngineeringCiJob(
                item.get("id").asLong,
                item.get("name")?.asString.orEmpty(),
                item.get("status")?.asString.orEmpty(),
                item.get("conclusion")?.takeUnless { it.isJsonNull }?.asString
            )
        }
    }

    private suspend fun fetchArtifactIds(runId: Long): List<Long> {
        val root = getJson("$repositoryPath/actions/runs/$runId/artifacts")
        return root.getAsJsonArray("artifacts")?.map { it.asJsonObject.get("id").asLong } ?: emptyList()
    }

    private suspend fun fetchJobLogExcerpt(jobId: Long): String = withContext(Dispatchers.IO) {
        val response = requestBytes("GET", "$repositoryPath/actions/jobs/$jobId/logs")
        if (response.statusCode() !in 200..299) return@withContext "Unable to retrieve job logs (HTTP ${response.statusCode()})."
        val output = StringBuilder()
        ZipInputStream(ByteArrayInputStream(response.body())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && isTextLog(entry.name) && output.length < config.maxLogChars) {
                    val remaining = config.maxLogChars - output.length
                    val bytes = zip.readNBytes(remaining.coerceAtMost(256_000))
                    output.append(String(bytes, Charsets.UTF_8)).append('\n')
                }
                zip.closeEntry()
            }
        }
        redact(output.toString()).take(config.maxLogChars)
    }

    private fun buildDiagnostics(jobs: List<EngineeringCiJob>): String {
        val failed = jobs.filter { it.conclusion == "failure" || it.conclusion == "cancelled" }
        if (failed.isEmpty()) return ""
        val out = StringBuilder()
        failed.forEach { job ->
            out.appendLine("JOB: ${job.name} [${job.conclusion}]")
            val useful = job.logExcerpt.lineSequence().filter { line ->
                val lower = line.lowercase()
                listOf("error:", "e: ", "failure:", "what went wrong", "execution failed",
                    "assertion", "exception", "failed", "could not", "cannot find symbol")
                    .any(lower::contains)
            }.takeLast(80).toList()
            if (useful.isEmpty()) out.appendLine(job.logExcerpt.takeLast(4_000))
            else useful.forEach(out::appendLine)
        }
        return out.toString().take(config.maxLogChars)
    }

    private suspend fun getJson(path: String): JsonObject {
        val response = request("GET", path)
        check(response.statusCode() in 200..299) {
            "GitHub API request failed with HTTP ${response.statusCode()}."
        }
        return JsonParser.parseString(response.body()).asJsonObject
    }

    private suspend fun request(method: String, path: String, body: String? = null): HttpResponse<String> =
        withContext(Dispatchers.IO) {
            val builder = HttpRequest.newBuilder(resolve(path))
                .timeout(Duration.ofSeconds(45))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer ${validatedToken()}")
                .header("X-GitHub-Api-Version", "2022-11-28")
            if (body != null) builder.header("Content-Type", "application/json")
            val request = when (method) {
                "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(body.orEmpty())).build()
                "GET" -> builder.GET().build()
                else -> error("Unsupported GitHub HTTP method.")
            }
            http.send(request, HttpResponse.BodyHandlers.ofString())
        }

    private suspend fun requestBytes(method: String, path: String): HttpResponse<ByteArray> =
        withContext(Dispatchers.IO) {
            val request = HttpRequest.newBuilder(resolve(path))
                .timeout(Duration.ofSeconds(60))
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer ${validatedToken()}")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .GET().build()
            http.send(request, HttpResponse.BodyHandlers.ofByteArray())
        }

    private fun validatedToken(): String = tokenProvider.accessToken().also {
        require(it.length in 20..500 && it.none(Char::isWhitespace)) {
            "A valid GitHub access token is required; tokens are never embedded in MANA source."
        }
    }

    private fun resolve(path: String): URI {
        val full = if (path.startsWith("https://") || path.startsWith("http://")) path
            else config.apiBaseUrl.trimEnd('/') + "/" + path.trimStart('/')
        val uri = URI.create(full)
        require(uri.host == URI.create(config.apiBaseUrl).host) { "GitHub API response attempted to change hosts." }
        return uri
    }

    private fun encode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8).replace("+", "%20")

    private fun isTextLog(name: String): Boolean =
        name.endsWith(".txt", true) || name.endsWith(".log", true)

    private fun redact(value: String): String {
        var safe = value
        listOf(
            Regex("(?i)(authorization: bearer\\s+)[^\\s]+"),
            Regex("(?i)(token|password|api[_-]?key)(\\s*[:=]\\s*)[^\\s,]+"),
            Regex("gh[pousr]_[A-Za-z0-9_]{20,}")
        ).forEach { safe = it.replace(safe, "$1[REDACTED]") }
        return safe
    }
}

data class EngineeringRepairContext(
    val taskId: String,
    val attempt: Int,
    val previousPatch: String,
    val diagnostics: String,
    val approvedPaths: Set<String>
)

/** Must return a complete replacement diff from the original base commit, not a delta atop a prior attempt. */
fun interface EngineeringPatchRepairer {
    suspend fun generateReplacement(context: EngineeringRepairContext): String
}

data class EngineeringRepairLoopResult(
    val runs: List<EngineeringCiRunResult>,
    val finalPatch: String,
    val repairAttempts: Int,
    val successful: Boolean
)

/**
 * Runs at most maxRepairAttempts + 1 CI attempts. It never merges or releases an artifact;
 * it only returns a verified candidate for a separate, owner-approved release step.
 */
class EngineeringCiRepairLoop(
    private val gateway: EngineeringCiGateway,
    private val repairer: EngineeringPatchRepairer,
    private val maxRepairAttempts: Int = 2
) {
    init { require(maxRepairAttempts in 0..3) }

    suspend fun run(
        taskId: String,
        initialPatch: String,
        approvedPaths: Set<String>,
        ownerApprovedCi: Boolean
    ): EngineeringRepairLoopResult {
        check(ownerApprovedCi) { "Owner approval is required before dispatching repository CI." }
        require(taskId.matches(Regex("[A-Za-z0-9._-]{1,60}")))
        require(approvedPaths.isNotEmpty())
        var patch = initialPatch
        val runs = mutableListOf<EngineeringCiRunResult>()
        for (attempt in 0..maxRepairAttempts) {
            val validation = EngineeringPatchPolicy.validate(patch)
            require(validation.changedPaths.all { it in approvedPaths }) {
                "CI patch modifies a file outside the owner-approved source set."
            }
            val runId = "$taskId-attempt-${attempt + 1}"
            val result = gateway.dispatchAndAwait(runId, patch)
            runs += result
            if (result.successful) {
                return EngineeringRepairLoopResult(runs, patch, attempt, true)
            }
            if (attempt == maxRepairAttempts) break
            require(result.diagnostics.isNotBlank()) {
                "CI failed without actionable diagnostics; automatic repair stopped safely."
            }
            patch = repairer.generateReplacement(
                EngineeringRepairContext(taskId, attempt + 1, patch, result.diagnostics, approvedPaths)
            )
        }
        return EngineeringRepairLoopResult(runs, patch, maxRepairAttempts, false)
    }
}
