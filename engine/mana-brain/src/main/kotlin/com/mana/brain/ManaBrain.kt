package com.mana.brain

import kotlinx.coroutines.withTimeoutOrNull

/**
 * MANA Brain routing contract.
 *
 * Local inference is always preferred. Cloud inference is an explicit,
 * per-request fallback and sensitive requests are never sent to a provider.
 */
data class BrainRequest(
    val prompt: String,
    val allowCloud: Boolean = false,
    val sensitive: Boolean = false,
    val preferredCloudProvider: String? = null
) {
    init {
        require(prompt.isNotBlank()) { "Prompt must not be blank" }
        require(prompt.length <= ManaBrain.MAX_PROMPT_CHARS) { "Prompt exceeds the maximum supported length" }
        require(preferredCloudProvider == null || preferredCloudProvider.isNotBlank())
    }
}

data class BrainResponse(
    val text: String,
    val provider: String,
    val local: Boolean
)

interface InferenceProvider {
    val id: String
    fun isAvailable(): Boolean
    suspend fun generate(prompt: String): String
}

class ManaBrain(
    private val localProvider: InferenceProvider,
    private val cloudProviders: List<InferenceProvider> = emptyList(),
    private val localTimeoutMs: Long = DEFAULT_LOCAL_TIMEOUT_MS,
    private val cloudTimeoutMs: Long = DEFAULT_CLOUD_TIMEOUT_MS
) {
    constructor(localProvider: InferenceProvider, cloudProvider: InferenceProvider?) :
        this(localProvider, listOfNotNull(cloudProvider))

    init {
        require(localTimeoutMs > 0) { "Local inference timeout must be positive" }
        require(cloudTimeoutMs > 0) { "Cloud inference timeout must be positive" }
        require(cloudProviders.map { canonicalProviderId(it.id) }.distinct().size == cloudProviders.size) {
            "Cloud provider identifiers must be unique after alias normalization"
        }
    }

    suspend fun respond(request: BrainRequest): BrainResponse {
        var localFailure: Exception? = null
        if (localProvider.isAvailable()) {
            try {
                val localText = withTimeoutOrNull(localTimeoutMs) {
                    localProvider.generate(request.prompt)
                } ?: throw InferenceTimeoutException("Local inference exceeded its time limit.")
                require(localText.isNotBlank()) { "Local inference returned an empty response." }
                return BrainResponse(
                    text = localText,
                    provider = localProvider.id,
                    local = true
                )
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                localFailure = failure
            }
        }

        if (!request.allowCloud || request.sensitive || cloudProviders.isEmpty()) {
            localFailure?.let { throw it }
            throw InferenceUnavailableException(
                "Local inference is unavailable and cloud processing is not permitted."
            )
        }

        val candidates = if (request.preferredCloudProvider == null) {
            cloudProviders
        } else {
            cloudProviders.filter {
                canonicalProviderId(it.id) == canonicalProviderId(request.preferredCloudProvider)
            }
        }
        val cloud = candidates.firstOrNull { it.isAvailable() }
            ?: throw InferenceUnavailableException(
                if (localFailure != null) {
                    "Local inference failed and no permitted cloud provider is available."
                } else {
                    request.preferredCloudProvider?.let { "Requested cloud provider '$it' is unavailable." }
                        ?: "No permitted cloud provider is available."
                }
            )

        val cloudText = withTimeoutOrNull(cloudTimeoutMs) {
            cloud.generate(request.prompt)
        } ?: throw InferenceTimeoutException("Cloud inference exceeded its time limit.")
        require(cloudText.isNotBlank()) { "Cloud inference returned an empty response." }
        return BrainResponse(
            text = cloudText,
            provider = canonicalProviderId(cloud.id),
            local = false
        )
    }

    private fun canonicalProviderId(value: String): String {
        val id = value.trim().lowercase(java.util.Locale.ROOT).removePrefix("cloud-")
        return when (id) {
            "gemini" -> "google"
            "grok" -> "xai"
            else -> id
        }
    }
    companion object {
        const val MAX_PROMPT_CHARS = 32_000
        const val DEFAULT_LOCAL_TIMEOUT_MS = 90_000L
        const val DEFAULT_CLOUD_TIMEOUT_MS = 60_000L
    }
}

class InferenceUnavailableException(message: String) : IllegalStateException(message)
class InferenceTimeoutException(message: String) : IllegalStateException(message)
