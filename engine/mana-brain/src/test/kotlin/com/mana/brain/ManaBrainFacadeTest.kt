package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManaBrainFacadeTest {
    private class Provider(
        override val id: String,
        private val available: Boolean,
        private val answer: String
    ) : InferenceProvider {
        override fun isAvailable() = available
        override suspend fun generate(prompt: String) = answer
    }

    @Test
    fun facadePreservesRequestIdAndLocalProvenance() {
        val facade = ManaBrainFacade(
            ManaBrain(Provider("litert-local", true, "local result"))
        )
        val result = kotlinx.coroutines.runBlocking {
            facade.submit(BrainCall(requestId = "req-1", prompt = "hello"))
        }
        assertEquals("req-1", result.requestId)
        assertEquals("litert-local", result.provider)
        assertEquals(true, result.local)
        assertEquals("local result", result.text)
    }

    @Test
    fun facadeDoesNotEnableCloudByDefault() {
        val facade = ManaBrainFacade(
            ManaBrain(
                Provider("litert-local", false, ""),
                Provider("openai", true, "cloud result")
            )
        )
        assertFailsWith<InferenceUnavailableException> {
            kotlinx.coroutines.runBlocking {
                facade.submit(BrainCall(requestId = "req-2", prompt = "hello"))
            }
        }
    }

    @Test
    fun facadeNeverSendsSensitiveRequestToCloud() {
        val cloud = Provider("openai", true, "must not be used")
        val facade = ManaBrainFacade(
            ManaBrain(Provider("litert-local", false, ""), cloud)
        )
        assertFailsWith<InferenceUnavailableException> {
            kotlinx.coroutines.runBlocking {
                facade.submit(
                    BrainCall(
                        requestId = "req-3",
                        prompt = "private",
                        allowCloud = true,
                        sensitive = true
                    )
                )
            }
        }
    }

    @Test
    fun invalidRequestIdIsRejectedAtBoundary() {
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            BrainCall(requestId = " ", prompt = "hello")
        }
    }
}
