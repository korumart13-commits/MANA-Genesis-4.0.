package com.mana.brain

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
    private val cloudProviders: List<InferenceProvider> = emptyList()
) {
    constructor(localProvider: InferenceProvider, cloudProvider: InferenceProvider?) :
        this(localProvider, listOfNotNull(cloudProvider))

    init {
        require(cloudProviders.map { it.id }.distinct().size == cloudProviders.size) {
            "Cloud provider identifiers must be unique"
        }
    }

    suspend fun respond(request: BrainRequest): BrainResponse {
        if (localProvider.isAvailable()) {
            return BrainResponse(
                text = localProvider.generate(request.prompt),
                provider = localProvider.id,
                local = true
            )
        }

        if (!request.allowCloud || request.sensitive || cloudProviders.isEmpty()) {
            throw InferenceUnavailableException(
                "Local inference is unavailable and cloud processing is not permitted."
            )
        }

        val candidates = if (request.preferredCloudProvider == null) {
            cloudProviders
        } else {
            cloudProviders.filter { it.id == request.preferredCloudProvider }
        }
        val cloud = candidates.firstOrNull { it.isAvailable() }
            ?: throw InferenceUnavailableException(
                request.preferredCloudProvider?.let { "Requested cloud provider '$it' is unavailable." }
                    ?: "No permitted cloud provider is available."
            )

        return BrainResponse(
            text = cloud.generate(request.prompt),
            provider = cloud.id,
            local = false
        )
    }
}

class InferenceUnavailableException(message: String) : IllegalStateException(message)
