package com.mana.os.core

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManaConnectorReasoningAdapterTest {
    private class FakeConnector(
        override val descriptor: ManaConnectorDescriptor,
        private val handler: suspend (String) -> String
    ) : ManaServiceConnector {
        override suspend fun execute(capability: String, request: String): String = handler(request)
    }

    private fun descriptor(id: String, sensitive: Boolean = false) = ManaConnectorDescriptor(
        id = id,
        displayName = id,
        kind = if (id == "local.llama_cpp") ManaConnectorKind.AI_MODEL else ManaConnectorKind.AI_MODEL,
        capabilities = setOf("chat.respond"),
        state = ManaConnectorState.ENABLED,
        supportsSensitiveData = sensitive
    )

    private fun candidate(id: String, sensitive: Boolean = false, cost: Long = 0) =
        ManaProviderCandidate(id, "chat.respond", true, sensitive, cost)

    @Test
    fun ordinaryRequestUsesCheapestAuthorizedProvider() = runBlocking {
        val registry = ManaConnectorRegistry()
        registry.register(FakeConnector(descriptor("cloud.gpt")) { "cloud-answer" })
        registry.setEnabled("cloud.gpt", true)
        registry.authorize(ManaConnectorGrant("cloud.gpt", setOf("chat.respond")))
        val adapter = ManaConnectorReasoningAdapter(
            registry,
            ManaProviderCatalog { listOf(candidate("cloud.gpt", cost = 1)) }
        )

        val result = adapter.respond(ManaReasoningRequest("hello", true, false, null))
        assertEquals("cloud-answer", result.text)
        assertEquals("cloud.gpt", result.provider)
        assertEquals(false, result.local)
    }

    @Test
    fun sensitiveRequestCannotSelectCloudEvenIfCatalogOffersIt() = runBlocking {
        val registry = ManaConnectorRegistry()
        var cloudCalls = 0
        registry.register(FakeConnector(descriptor("cloud.gpt")) { cloudCalls++; "cloud" })
        registry.register(FakeConnector(descriptor("local.llama_cpp", sensitive = true)) { "local-answer" })
        registry.setEnabled("cloud.gpt", true)
        registry.setEnabled("local.llama_cpp", true)
        registry.authorize(ManaConnectorGrant("cloud.gpt", setOf("chat.respond")))
        registry.authorize(ManaConnectorGrant("local.llama_cpp", setOf("chat.respond")))
        val adapter = ManaConnectorReasoningAdapter(
            registry,
            ManaProviderCatalog {
                listOf(candidate("cloud.gpt", cost = 0), candidate("local.llama_cpp", sensitive = true, cost = 5))
            }
        )

        val result = adapter.respond(ManaReasoningRequest("private", true, true, null))
        assertEquals("local-answer", result.text)
        assertEquals(true, result.local)
        assertEquals(0, cloudCalls)
    }

    @Test
    fun cloudFailureFallsBackToAuthorizedLocalProvider() = runBlocking {
        val registry = ManaConnectorRegistry()
        registry.register(FakeConnector(descriptor("cloud.gpt")) { error("cloud offline") })
        registry.register(FakeConnector(descriptor("local.llama_cpp")) { "local-fallback" })
        registry.setEnabled("cloud.gpt", true)
        registry.setEnabled("local.llama_cpp", true)
        registry.authorize(ManaConnectorGrant("cloud.gpt", setOf("chat.respond")))
        registry.authorize(ManaConnectorGrant("local.llama_cpp", setOf("chat.respond")))
        val adapter = ManaConnectorReasoningAdapter(
            registry,
            ManaProviderCatalog { listOf(candidate("cloud.gpt", cost = 0), candidate("local.llama_cpp", cost = 10)) }
        )

        val result = adapter.respond(ManaReasoningRequest("hello", true, false, null))
        assertEquals("local-fallback", result.text)
        assertEquals("local.llama_cpp", result.provider)
        assertEquals(true, result.local)
    }

    @Test
    fun missingOwnerGrantMakesProviderUnavailable() = runBlocking {
        val registry = ManaConnectorRegistry()
        registry.register(FakeConnector(descriptor("local.llama_cpp")) { "must-not-run" })
        registry.setEnabled("local.llama_cpp", true)
        val adapter = ManaConnectorReasoningAdapter(
            registry,
            ManaProviderCatalog { listOf(candidate("local.llama_cpp")) }
        )

        assertFailsWith<IllegalStateException> {
            runBlocking { adapter.respond(ManaReasoningRequest("hello", false, false, null)) }
        }
    }
}
