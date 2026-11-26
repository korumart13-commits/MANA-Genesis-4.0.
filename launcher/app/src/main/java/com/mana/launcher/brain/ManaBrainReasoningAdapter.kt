package com.mana.launcher.brain

import com.mana.ai.BrainRequest
import com.mana.ai.ManaBrain
import com.mana.os.core.ManaReasoningPort
import com.mana.os.core.ManaReasoningRequest
import com.mana.os.core.ManaReasoningResult

/**
 * Bridges the canonical MANA Brain provider router into the OS-level reasoning port.
 *
 * The Brain owns provider selection (local-first, explicit cloud opt-in, sensitive
 * request blocking); the OS core owns memory policy and Shogun/Hydra task authorization.
 */
class ManaBrainReasoningAdapter(
    private val brain: ManaBrain
) : ManaReasoningPort {
    override suspend fun respond(request: ManaReasoningRequest): ManaReasoningResult {
        val response = brain.respond(
            BrainRequest(
                prompt = request.prompt,
                allowCloud = request.allowCloud,
                sensitive = request.sensitive,
                preferredCloudProvider = request.preferredCloudProvider
            )
        )
        return ManaReasoningResult(
            text = response.text,
            provider = response.provider,
            local = response.local
        )
    }
}
