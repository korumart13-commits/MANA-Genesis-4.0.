package com.mana.launcher

import java.io.File

/**
 * Backend-neutral contract for local model execution.
 * Implementations must serialize access to native/runtime state and never block the UI thread.
 */
interface LocalInferenceRuntime {
    fun isReady(): Boolean
    fun modelName(): String?
    fun load(modelFile: File, callback: (Result<String>) -> Unit)
    fun generate(prompt: String, onChunk: (String) -> Unit, callback: (Result<Unit>) -> Unit)
    /** Cancels the active generation when the caller's deadline or lifecycle ends. */
    fun cancelGeneration() {}
    fun shutdown()
}
