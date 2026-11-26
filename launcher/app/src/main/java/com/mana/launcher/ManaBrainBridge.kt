package com.mana.launcher

import com.mana.brain.BrainRequest
import com.mana.brain.BrainResponse
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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Routes requests through MANA Brain: the configured local runtime is always attempted first.
 * Cloud providers are optional and require per-request consent.
 */
class ManaBrainBridge(
    private val localRuntime: LocalInferenceRuntime,
    private val cloudProviders: List<InferenceProvider> = emptyList()
) {
    constructor(localRuntime: LocalInferenceRuntime, cloudProvider: InferenceProvider?) :
        this(localRuntime, listOfNotNull(cloudProvider))
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val closed = AtomicBoolean(false)

    suspend fun respondDetailed(
        prompt: String,
        allowCloud: Boolean = false,
        sensitive: Boolean = false,
        preferredCloudProvider: String? = null,
        onChunk: (String) -> Unit = {}
    ): BrainResponse {
        check(!closed.get()) { "MANA Brain bridge is shut down" }
        val localProvider = object : InferenceProvider {
            override val id: String = "local-runtime"
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
        val personaCloudProviders = cloudProviders.map { provider ->
            object : InferenceProvider {
                override val id: String get() = provider.id
                override fun isAvailable(): Boolean = provider.isAvailable()
                override suspend fun generate(prompt: String): String = provider.generate(
                    ManaPersona.SYSTEM_PROMPT + "\n\nЗапрос владельца:\n" + prompt
                )
            }
        }
        val response = ManaBrain(localProvider, personaCloudProviders).respond(
            BrainRequest(
                prompt = prompt,
                allowCloud = allowCloud,
                sensitive = sensitive,
                preferredCloudProvider = preferredCloudProvider
            )
        )
        if (!response.local) onChunk(response.text)
        return response
    }

    fun respond(
        prompt: String,
        allowCloud: Boolean = false,
        sensitive: Boolean = false,
        preferredCloudProvider: String? = null,
        onChunk: (String) -> Unit,
        callback: (Result<Unit>) -> Unit
    ) {
        val completed = AtomicBoolean(false)
        fun complete(result: Result<Unit>) {
            if (completed.compareAndSet(false, true)) {
                runCatching { callback(result) }
            }
        }

        if (closed.get()) {
            complete(Result.failure(IllegalStateException("MANA Brain bridge is shut down")))
            return
        }

        val job = scope.launch {
            try {
                if (closed.get()) {
                    complete(Result.failure(IllegalStateException("MANA Brain bridge is shut down")))
                    return@launch
                }
                respondDetailed(prompt, allowCloud, sensitive, preferredCloudProvider, onChunk)
                complete(Result.success(Unit))
            } catch (error: Exception) {
                complete(Result.failure(error))
            }
        }
        // A scope cancelled between the closed check and launch must still complete the caller.
        job.invokeOnCompletion { cause ->
            if (cause != null) complete(Result.failure(cause))
        }
    }

    fun shutdown() {
        if (closed.compareAndSet(false, true)) scope.cancel()
    }
}
