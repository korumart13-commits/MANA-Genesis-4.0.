package com.mana.brain

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FakeProvider(
    override val id: String,
    private var available: Boolean,
    private val answer: String
) : InferenceProvider {
    var calls: Int = 0
        private set

    override fun isAvailable() = available
    override suspend fun generate(prompt: String): String { calls++; return answer }
    fun setAvailable(value: Boolean) { available = value }
}

class ManaBrainTest {
    @Test fun localProviderHasPriorityEvenWhenCloudIsAllowed() {
        val local = FakeProvider("litert-local", true, "local answer")
        val cloud = FakeProvider("cloud", true, "cloud answer")
        val result = runBlocking { ManaBrain(local, cloud).respond(BrainRequest("hello", allowCloud = true)) }
        assertEquals("local answer", result.text)
        assertEquals(true, result.local)
        assertEquals(1, local.calls)
        assertEquals(0, cloud.calls)
    }

    @Test fun cloudIsNotUsedWithoutExplicitPermission() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = FakeProvider("cloud", true, "cloud answer")
        assertFailsWith<InferenceUnavailableException> { runBlocking { ManaBrain(local, cloud).respond(BrainRequest("hello")) } }
        assertEquals(0, cloud.calls)
    }

    @Test fun sensitiveRequestsNeverFallBackToCloud() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = FakeProvider("cloud", true, "cloud answer")
        assertFailsWith<InferenceUnavailableException> {
            runBlocking { ManaBrain(local, cloud).respond(BrainRequest("private note", allowCloud = true, sensitive = true)) }
        }
        assertEquals(0, cloud.calls)
    }

    @Test fun cloudFallbackRequiresOwnerPermissionAndLocalUnavailable() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = FakeProvider("cloud", true, "cloud answer")
        val result = runBlocking { ManaBrain(local, cloud).respond(BrainRequest("hello", allowCloud = true)) }
        assertEquals("cloud answer", result.text)
        assertEquals(false, result.local)
        assertEquals(1, cloud.calls)
    }

    @Test fun preferredCloudProviderIsSelectedFromRegistry() {
        val local = FakeProvider("litert-local", false, "")
        val openAi = FakeProvider("openai", true, "openai answer")
        val gemini = FakeProvider("google", true, "gemini answer")
        val result = runBlocking {
            ManaBrain(local, listOf(openAi, gemini)).respond(
                BrainRequest("hello", allowCloud = true, preferredCloudProvider = "google")
            )
        }
        assertEquals("google", result.provider)
        assertEquals("gemini answer", result.text)
        assertEquals(0, openAi.calls)
        assertEquals(1, gemini.calls)
    }

    @Test fun unknownPreferredProviderFailsClosedInsteadOfSilentlySwitching() {
        val local = FakeProvider("litert-local", false, "")
        val openAi = FakeProvider("openai", true, "openai answer")
        assertFailsWith<InferenceUnavailableException> {
            runBlocking {
                ManaBrain(local, listOf(openAi)).respond(
                    BrainRequest("hello", allowCloud = true, preferredCloudProvider = "unknown")
                )
            }
        }
        assertEquals(0, openAi.calls)
    }

    @Test fun duplicateCloudProviderIdsAreRejected() {
        val local = FakeProvider("litert-local", false, "")
        val first = FakeProvider("openai", true, "one")
        val second = FakeProvider("openai", true, "two")
        assertFailsWith<IllegalArgumentException> { ManaBrain(local, listOf(first, second)) }
    }
}
