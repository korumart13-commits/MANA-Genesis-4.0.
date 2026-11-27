package com.mana.brain

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

    override suspend fun generate(prompt: String): String {
        calls++
        return answer
    }

    fun setAvailable(value: Boolean) {
        available = value
    }
}

class ManaBrainCloudSelectionTest {
    @Test
    fun preferredCloudProviderIsSelectedFromRegistry() {
        val local = FakeProvider("litert-local", false, "")
        val openAi = FakeProvider("openai", true, "openai answer")
        val gemini = FakeProvider("google", true, "gemini answer")

        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, listOf(openAi, gemini)).respond(
                BrainRequest("hello", allowCloud = true, preferredCloudProvider = "google")
            )
        }

        assertEquals("google", result.provider)
        assertEquals("gemini answer", result.text)
        assertEquals(0, openAi.calls)
        assertEquals(1, gemini.calls)
    }

    @Test
    fun cloudGatewayIdsAndProviderAliasesResolveToTheSameConnector() {
        val local = FakeProvider("litert-local", false, "")
        val gateway = FakeProvider("cloud-gemini", true, "gemini answer")
        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, listOf(gateway)).respond(
                BrainRequest("hello", allowCloud = true, preferredCloudProvider = "google")
            )
        }
        assertEquals("google", result.provider)
        assertEquals("gemini answer", result.text)
        assertEquals(1, gateway.calls)
    }

    @Test
    fun geminiAliasSelectsGoogleConnector() {
        val local = FakeProvider("litert-local", false, "")
        val gateway = FakeProvider("cloud-gemini", true, "gemini answer")
        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, listOf(gateway)).respond(
                BrainRequest("hello", allowCloud = true, preferredCloudProvider = "gemini")
            )
        }
        assertEquals("google", result.provider)
        assertEquals(1, gateway.calls)
    }

    @Test
    fun cloudProviderAliasesCannotRegisterAsDuplicateConnectors() {
        val local = FakeProvider("litert-local", false, "")
        val google = FakeProvider("google", true, "one")
        val gemini = FakeProvider("cloud-gemini", true, "two")
        assertFailsWith<IllegalArgumentException> { ManaBrain(local, listOf(google, gemini)) }
    }

    @Test
    fun unknownPreferredProviderFailsClosedInsteadOfSilentlySwitching() {
        val local = FakeProvider("litert-local", false, "")
        val openAi = FakeProvider("openai", true, "openai answer")

        assertFailsWith<InferenceUnavailableException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, listOf(openAi)).respond(
                    BrainRequest("hello", allowCloud = true, preferredCloudProvider = "unknown")
                )
            }
        }
        assertEquals(0, openAi.calls)
    }

    @Test
    fun duplicateCloudProviderIdsAreRejected() {
        val local = FakeProvider("litert-local", false, "")
        val first = FakeProvider("openai", true, "one")
        val second = FakeProvider("openai", true, "two")
        assertFailsWith<IllegalArgumentException> { ManaBrain(local, listOf(first, second)) }
    }

}

class ManaBrainTest {
    @Test
    fun localProviderHasPriorityEvenWhenCloudIsAllowed() {
        val local = FakeProvider("litert-local", true, "local answer")
        val cloud = FakeProvider("cloud", true, "cloud answer")

        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, cloud).respond(BrainRequest("hello", allowCloud = true))
        }

        assertEquals("local answer", result.text)
        assertEquals(true, result.local)
        assertEquals(1, local.calls)
        assertEquals(0, cloud.calls)
    }

    @Test
    fun cloudIsNotUsedWithoutExplicitPermission() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = FakeProvider("cloud", true, "cloud answer")

        assertFailsWith<InferenceUnavailableException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, cloud).respond(BrainRequest("hello"))
            }
        }
        assertEquals(0, cloud.calls)
    }

    @Test
    fun sensitiveRequestsNeverFallBackToCloud() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = FakeProvider("cloud", true, "cloud answer")

        assertFailsWith<InferenceUnavailableException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, cloud).respond(
                    BrainRequest("private note", allowCloud = true, sensitive = true)
                )
            }
        }
        assertEquals(0, cloud.calls)
    }

    @Test
    fun localInferenceFailureFallsBackOnlyWithExplicitCloudConsent() {
        val local = object : InferenceProvider {
            override val id = "litert-local"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String): String = error("local runtime failure")
        }
        val cloud = FakeProvider("openai", true, "cloud answer")
        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, cloud).respond(BrainRequest("hello", allowCloud = true))
        }
        assertEquals("cloud answer", result.text)
        assertEquals("openai", result.provider)
        assertEquals(false, result.local)
        assertEquals(1, cloud.calls)
    }

    @Test
    fun localInferenceFailureDoesNotFallBackWithoutConsent() {
        val local = object : InferenceProvider {
            override val id = "litert-local"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String): String = error("local runtime failure")
        }
        val cloud = FakeProvider("openai", true, "cloud answer")
        assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, cloud).respond(BrainRequest("hello", allowCloud = false))
            }
        }
        assertEquals(0, cloud.calls)
    }

    @Test
    fun localInferenceFailureNeverFallsBackForSensitiveRequest() {
        val local = object : InferenceProvider {
            override val id = "litert-local"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String): String = error("local runtime failure")
        }
        val cloud = FakeProvider("openai", true, "cloud answer")
        assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, cloud).respond(
                    BrainRequest("private", allowCloud = true, sensitive = true)
                )
            }
        }
        assertEquals(0, cloud.calls)
    }

    @Test
    fun blankLocalResponseCanUseCloudOnlyWithExplicitConsent() {
        val local = object : InferenceProvider {
            override val id = "litert-local"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String): String = "  "
        }
        val cloud = FakeProvider("openai", true, "cloud answer")
        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, cloud).respond(BrainRequest("hello", allowCloud = true))
        }
        assertEquals("cloud answer", result.text)
        assertEquals(1, cloud.calls)
    }

    @Test
    fun timedOutLocalInferenceFallsBackOnlyWithExplicitConsent() {
        val local = object : InferenceProvider {
            override val id = "litert-local"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String): String {
                kotlinx.coroutines.delay(200)
                return "late local answer"
            }
        }
        val cloud = FakeProvider("openai", true, "timely cloud answer")
        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, listOf(cloud), localTimeoutMs = 20, cloudTimeoutMs = 200)
                .respond(BrainRequest("hello", allowCloud = true))
        }
        assertEquals("timely cloud answer", result.text)
        assertEquals(false, result.local)
        assertEquals(1, cloud.calls)
    }

    @Test
    fun timedOutLocalInferenceDoesNotUseCloudWithoutConsent() {
        val local = object : InferenceProvider {
            override val id = "litert-local"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String): String {
                kotlinx.coroutines.delay(200)
                return "late local answer"
            }
        }
        val cloud = FakeProvider("openai", true, "must not be used")
        assertFailsWith<IllegalStateException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, listOf(cloud), localTimeoutMs = 20)
                    .respond(BrainRequest("hello", allowCloud = false))
            }
        }
        assertEquals(0, cloud.calls)
    }

    @Test
    fun cloudInferenceHasItsOwnDeadline() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = object : InferenceProvider {
            override val id = "openai"
            override fun isAvailable() = true
            override suspend fun generate(prompt: String): String {
                kotlinx.coroutines.delay(200)
                return "late cloud answer"
            }
        }
        assertFailsWith<InferenceTimeoutException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, listOf(cloud), localTimeoutMs = 100, cloudTimeoutMs = 20)
                    .respond(BrainRequest("hello", allowCloud = true))
            }
        }
    }

    @Test
    fun cloudProviderCannotReturnBlankResponse() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = FakeProvider("openai", true, "   ")
        assertFailsWith<IllegalArgumentException> {
            kotlinx.coroutines.runBlocking {
                ManaBrain(local, listOf(cloud)).respond(
                    BrainRequest("hello", allowCloud = true)
                )
            }
        }
    }

    @Test
    fun oversizedPromptIsRejectedBeforeInference() {
        val prompt = "x".repeat(ManaBrain.MAX_PROMPT_CHARS + 1)
        assertFailsWith<IllegalArgumentException> { BrainRequest(prompt) }
    }

    @Test
    fun blankProviderIdentifiersAreRejected() {
        val local = FakeProvider("", true, "answer")
        assertFailsWith<IllegalArgumentException> { ManaBrain(local) }
        val validLocal = FakeProvider("litert-local", false, "")
        val blankCloud = FakeProvider(" ", true, "answer")
        assertFailsWith<IllegalArgumentException> { ManaBrain(validLocal, listOf(blankCloud)) }
    }

    @Test
    fun inferenceTimeoutsMustBePositive() {
        val local = FakeProvider("litert-local", false, "")
        assertFailsWith<IllegalArgumentException> { ManaBrain(local, emptyList(), localTimeoutMs = 0) }
        assertFailsWith<IllegalArgumentException> { ManaBrain(local, emptyList(), cloudTimeoutMs = -1) }
    }

    @Test
    fun cloudFallbackRequiresOwnerPermissionAndLocalUnavailable() {
        val local = FakeProvider("litert-local", false, "")
        val cloud = FakeProvider("cloud", true, "cloud answer")

        val result = kotlinx.coroutines.runBlocking {
            ManaBrain(local, cloud).respond(BrainRequest("hello", allowCloud = true))
        }

        assertEquals("cloud answer", result.text)
        assertEquals(false, result.local)
        assertEquals(1, cloud.calls)
    }
}
