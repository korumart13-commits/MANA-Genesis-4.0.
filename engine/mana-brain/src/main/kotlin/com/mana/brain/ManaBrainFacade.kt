package com.mana.brain

/**
 * Transport-neutral application boundary used by Android Binder adapters.
 *
 * The AIDL service implementation must authenticate the Binder caller before invoking
 * this facade. This class deliberately contains no Android APIs and cannot grant caller
 * permissions by itself.
 */
data class BrainCall(
    val requestId: String,
    val prompt: String,
    val allowCloud: Boolean = false,
    val sensitive: Boolean = false,
    val preferredCloudProvider: String? = null
) {
    init {
        require(requestId.isNotBlank()) { "requestId must not be blank" }
        require(prompt.isNotBlank()) { "prompt must not be blank" }
        require(preferredCloudProvider == null || preferredCloudProvider.isNotBlank())
    }
}

data class BrainResult(
    val requestId: String,
    val text: String,
    val provider: String,
    val local: Boolean
)

class ManaBrainFacade(private val engine: ManaBrain) {
    suspend fun submit(call: BrainCall): BrainResult {
        val result = engine.respond(
            BrainRequest(
                prompt = call.prompt,
                allowCloud = call.allowCloud,
                sensitive = call.sensitive,
                preferredCloudProvider = call.preferredCloudProvider
            )
        )
        return BrainResult(
            requestId = call.requestId,
            text = result.text,
            provider = result.provider,
            local = result.local
        )
    }
}
