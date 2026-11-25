package com.mana.brain

import java.util.ServiceLoader
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

/** Callback deliberately uses a Java-friendly contract for the Android Binder adapter. */
interface BrainCompletion {
    fun onComplete(result: BrainResult?, error: Throwable?)
}

/**
 * Android host for the canonical MANA Brain facade.
 *
 * Provider implementations are discovered through ServiceLoader. The core remains usable
 * without an installed model, but it never reports readiness or invents output in that case.
 * A LiteRT-LM provider can be added as a separate Soong library and registered through
 * META-INF/services/com.mana.brain.InferenceProvider.
 */
class ManaBrainAndroidRuntime private constructor() {
    private val providers: List<InferenceProvider> =
        ServiceLoader.load(InferenceProvider::class.java).toList()

    private val localProvider: InferenceProvider =
        providers.firstOrNull { it.id == "litert-lm" || it.id.startsWith("local-") }
            ?: UnavailableLocalProvider

    private val cloudProviders: List<InferenceProvider> =
        providers.filterNot { it === localProvider }

    private val facade = ManaBrainFacade(ManaBrain(localProvider, cloudProviders))

    fun isReady(): Boolean = localProvider.isAvailable()

    fun engineVersion(): String = ENGINE_VERSION

    fun submit(call: BrainCall, completion: BrainCompletion) {
        suspend { facade.submit(call) }.startCoroutine(
            object : Continuation<BrainResult> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<BrainResult>) {
                    result.fold(
                        onSuccess = { completion.onComplete(it, null) },
                        onFailure = { completion.onComplete(null, it) }
                    )
                }
            }
        )
    }

    private object UnavailableLocalProvider : InferenceProvider {
        override val id: String = "local-unavailable"
        override fun isAvailable(): Boolean = false
        override suspend fun generate(prompt: String): String {
            throw InferenceUnavailableException("No local MANA model provider is installed.")
        }
    }

    companion object {
        const val ENGINE_VERSION = "4.0.0-android-core"

        @Volatile private var instance: ManaBrainAndroidRuntime? = null

        @JvmStatic
        fun get(): ManaBrainAndroidRuntime =
            instance ?: synchronized(this) {
                instance ?: ManaBrainAndroidRuntime().also { instance = it }
            }
    }
}
