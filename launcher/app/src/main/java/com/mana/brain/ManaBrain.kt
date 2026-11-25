package com.mana.brain

/**
 * MANA Brain routing contract.
 *
 * The engine prefers a local inference provider. Cloud providers are only
 * considered when the owner enables network use and local inference is
 * unavailable or the request explicitly permits cloud processing.
 *
 * This module deliberately does not claim to contain model weights or an
 * inference runtime. A LiteRT-LM adapter is supplied in a later integration
 * step after the exact supported Android artifact/API is pinned.
 */
data class BrainRequest(
    val prompt: String,
    val allowCloud: Boolean = false,
    val sensitive: Boolean = false
) {
    init {
        require(prompt.isNotBlank()) { "Prompt must not be blank" }
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
    private val cloudProvider: InferenceProvider? = null
) {
    suspend fun respond(request: BrainRequest): BrainResponse {
        if (localProvider.isAvailable()) {
            return BrainResponse(
                text = localProvider.generate(request.prompt),
                provider = localProvider.id,
                local = true
            )
        }

        val cloud = cloudProvider
        if (!request.allowCloud || request.sensitive || cloud == null || !cloud.isAvailable()) {
            throw InferenceUnavailableException(
                "Local inference is unavailable and cloud processing is not permitted."
            )
        }

        return BrainResponse(
            text = cloud.generate(request.prompt),
            provider = cloud.id,
            local = false
        )
    }
}

class InferenceUnavailableException(message: String) : IllegalStateException(message)
