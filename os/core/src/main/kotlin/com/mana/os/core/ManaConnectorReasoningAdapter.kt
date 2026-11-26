package com.mana.os.core

import kotlinx.coroutines.CancellationException

/**
 * Supplies only connectors that are currently reachable and have an owner-granted
 * capability. Implementations should probe health without sending user prompt data.
 */
fun interface ManaProviderCatalog {
    fun candidates(capability: String): List<ManaProviderCandidate>
}

/**
 * Bridges the central MANA Brain to the one connector registry.
 *
 * Sensitive requests are local-only. For ordinary requests, cloud use is possible only
 * when the request permits it and the engine's caller has already granted the connector
 * capability. A failed cloud call may fall back to local inference.
 */
class ManaConnectorReasoningAdapter(
    private val registry: ManaConnectorRegistry,
    private val catalog: ManaProviderCatalog,
    private val router: ManaProviderRouter = ManaProviderRouter(),
    private val chatCapability: String = "chat.respond",
    private val localConnectorId: String = "local.llama_cpp"
) : ManaReasoningPort {

    init {
        require(chatCapability.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
        require(localConnectorId.matches(Regex("[a-z][a-z0-9_.-]{1,63}")))
    }

    override suspend fun respond(request: ManaReasoningRequest): ManaReasoningResult {
        require(request.prompt.isNotBlank() && request.prompt.length <= 32_000)

        val registered = registry.catalog()
            .filter { it.state == ManaConnectorState.ENABLED }
            .associateBy { it.id }

        val eligible = catalog.candidates(chatCapability).filter { candidate ->
            val connector = registered[candidate.connectorId]
            connector != null &&
                candidate.capability == chatCapability &&
                chatCapability in connector.capabilities &&
                (!request.sensitive || (
                    candidate.connectorId == localConnectorId &&
                        connector.supportsSensitiveData &&
                        candidate.supportsSensitiveData
                ))
        }

        val selected = router.choose(
            candidates = eligible,
            sensitive = request.sensitive,
            cloudAllowed = request.allowCloud && !request.sensitive
        ) ?: error("No authorized MANA reasoning provider is available")

        return try {
            invoke(selected, request)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cloudFailure: Exception) {
            if (selected.connectorId == localConnectorId) throw cloudFailure
            val local = eligible.firstOrNull { it.connectorId == localConnectorId }
                ?.takeIf { !request.sensitive || it.supportsSensitiveData }
                ?: throw cloudFailure
            invoke(local, request)
        }
    }

    private suspend fun invoke(
        candidate: ManaProviderCandidate,
        request: ManaReasoningRequest
    ): ManaReasoningResult {
        // Length-prefixing keeps prompt text unambiguous without adding a JSON dependency.
        val payload = buildString {
            append("MANA-REASONING/1\n")
            append("sensitive=").append(request.sensitive).append('\n')
            append("prompt-length=").append(request.prompt.length).append('\n')
            append(request.prompt)
        }
        val response = registry.invoke(
            id = candidate.connectorId,
            capability = chatCapability,
            request = payload,
            sensitive = request.sensitive
        )
        require(response.isNotBlank()) { "Reasoning provider returned an empty response" }
        return ManaReasoningResult(
            text = response,
            provider = candidate.connectorId,
            local = candidate.connectorId == localConnectorId
        )
    }
}
