package com.mana.launcher

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant

/**
 * Searches public GitHub repository metadata for possible RMX5566/RE645F/SM6225 sources.
 *
 * Search results are untrusted leads only. This worker never clones repositories, executes
 * code, downloads firmware, changes partitions, or marks a source as compatible.
 */
class ManaGenesisSourceDiscoveryWorker(
    context: Context,
    parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result = try {
        withContext(Dispatchers.IO) {
            val report = searchPublicRepositoryMetadata()
            persistReport(report)
        }
        Result.success()
    } catch (error: Exception) {
        android.util.Log.w(TAG, "Public source discovery did not complete", error)
        if (runAttemptCount < 4) Result.retry() else Result.failure()
    }

    private fun searchPublicRepositoryMetadata(): JSONObject {
        val query = "RMX5566 OR RE645F OR SM6225 android device tree"
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
        val connection = (URL(
            "https://api.github.com/search/repositories?q=$encoded&per_page=25&sort=updated"
        ).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "MANA-Genesis-SourceDiscovery/1.0")
            instanceFollowRedirects = false
        }

        try {
            val status = connection.responseCode
            if (status !in 200..299) {
                throw java.io.IOException("GitHub repository search returned HTTP $status")
            }
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_RESPONSE_BYTES) {
                        throw java.io.IOException("Repository search response exceeded size limit")
                    }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            val root = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val sourceItems = root.optJSONArray("items") ?: JSONArray()
            val candidates = JSONArray()
            for (index in 0 until minOf(sourceItems.length(), MAX_CANDIDATES)) {
                val item = sourceItems.optJSONObject(index) ?: continue
                val fullName = item.optString("full_name")
                val htmlUrl = item.optString("html_url")
                if (!fullName.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")) ||
                    !htmlUrl.startsWith("https://github.com/")) continue

                candidates.put(JSONObject()
                    .put("repository", fullName)
                    .put("url", htmlUrl)
                    .put("description", item.optString("description").take(500))
                    .put("defaultBranch", item.optString("default_branch").take(100))
                    .put("stars", item.optInt("stargazers_count", 0))
                    .put("updatedAt", item.optString("updated_at").take(40))
                    .put("verification", "UNVERIFIED_PUBLIC_METADATA_ONLY"))
            }
            return JSONObject()
                .put("schema", 1)
                .put("query", query)
                .put("searchedAt", Instant.now().toString())
                .put("source", "GitHub public repository search API")
                .put("candidateCount", candidates.length())
                .put("candidates", candidates)
                .put("limitations", JSONArray(listOf(
                    "Repository metadata is not proof of device compatibility.",
                    "No source archives or binaries were downloaded.",
                    "Kernel, device tree, vendor, VINTF and restore provenance require separate verification."
                )))
        } finally {
            connection.disconnect()
        }
    }

    private fun persistReport(report: JSONObject) {
        val directory = File(applicationContext.filesDir, "mana/genesis/research").apply {
            check(mkdirs() || isDirectory) { "Cannot create MANA research directory" }
        }
        val destination = File(directory, "public-device-source-candidates.json")
        val temporary = File(directory, "public-device-source-candidates.json.tmp")
        FileOutputStream(temporary).use { stream ->
            stream.write(report.toString(2).toByteArray(StandardCharsets.UTF_8))
            stream.fd.sync()
        }
        try {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
            Files.move(
                temporary.toPath(),
                destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    companion object {
        private const val TAG = "MANA-SourceDiscovery"
        private const val MAX_RESPONSE_BYTES = 512 * 1024
        private const val MAX_CANDIDATES = 25
    }
}
