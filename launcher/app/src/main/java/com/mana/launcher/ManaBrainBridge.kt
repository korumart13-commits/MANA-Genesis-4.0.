package com.mana.launcher

import com.mana.brain.BrainRequest
import com.mana.brain.InferenceProvider
import com.mana.brain.ManaBrain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Connects the shared MANA Brain router to the on-device LiteRT-LM runtime.
 * This Android build deliberately has no cloud provider, so prompts remain local.
 */
class ManaBrainBridge(private val localRuntime: LocalModelRuntime) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun respond(
        prompt: String,
        onChunk: (String) -> Unit,
        callback: (Result<Unit>) -> Unit
    ) {
        scope.launch {
            try {
                val localProvider = object : InferenceProvider {
                    override val id: String = "litertlm-local"
                    override fun isAvailable(): Boolean = localRuntime.isReady()

                    override suspend fun generate(prompt: String): String =
                        suspendCancellableCoroutine { continuation ->
                            val output = StringBuilder()
                            localRuntime.generate(
                                prompt,
                                onChunk = { chunk ->
                                    output.append(chunk)
                                    onChunk(chunk)
                                },
                                callback = { result ->
                                    result.fold(
                                        onSuccess = {
                                            if (continuation.isActive) continuation.resume(output.toString())
                                        },
                                        onFailure = { error ->
                                            if (continuation.isActive) continuation.resumeWithException(error)
                                        }
                                    )
                                }
                            )
                        }
                }
                val brain = ManaBrain(localProvider)
                brain.respond(BrainRequest(prompt = prompt, allowCloud = false, sensitive = false))
                callback(Result.success(Unit))
            } catch (error: Exception) {
                callback(Result.failure(error))
            }
        }
    }

    fun shutdown() {
        scope.cancel()
    }
}
