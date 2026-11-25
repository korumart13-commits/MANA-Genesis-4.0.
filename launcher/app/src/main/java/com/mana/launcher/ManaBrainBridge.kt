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
 * Routes requests through MANA Brain: local LiteRT-LM always wins.
 * Cloud is passed as an optional provider and requires per-request consent.
 */
class ManaBrainBridge(
    private val localRuntime: LocalModelRuntime,
    private val cloudProviders: List<InferenceProvider> = emptyList()
) {
    constructor(localRuntime: LocalModelRuntime, cloudProvider: InferenceProvider?) :
        this(localRuntime, listOfNotNull(cloudProvider))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun respond(
        prompt: String,
        allowCloud: Boolean = false,
        sensitive: Boolean = false,
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
                val response = ManaBrain(localProvider, cloudProviders).respond(
                    BrainRequest(
                        prompt = prompt,
                        allowCloud = allowCloud,
                        sensitive = sensitive
                    )
                )
                // Cloud provider currently returns a complete response, not a token stream.
                if (!response.local) onChunk(response.text)
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
