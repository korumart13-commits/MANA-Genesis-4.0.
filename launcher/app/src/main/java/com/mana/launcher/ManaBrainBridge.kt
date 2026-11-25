package com.mana.launcher

/**
 * MANA Brain's Android bridge: routes avatar chat to the on-device LiteRT-LM
 * runtime. It intentionally has no cloud fallback, so local-first behavior
 * cannot silently send prompts off-device.
 */
class ManaBrainBridge(private val localRuntime: LocalModelRuntime) {
    fun respond(
        prompt: String,
        onChunk: (String) -> Unit,
        callback: (Result<Unit>) -> Unit
    ) {
        localRuntime.generate(prompt, onChunk, callback)
    }
}
