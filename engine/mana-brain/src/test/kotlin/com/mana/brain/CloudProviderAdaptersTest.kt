package com.mana.brain

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudProviderAdaptersTest {
    private class Secrets(private val values: Map<String, String>) : ManaSecretStore {
        override fun readSecret(alias: String) = values[alias]
    }
    private class CaptureTransport(private val response: String) : CloudHttpTransport {
        var request: CloudHttpRequest? = null
        override suspend fun post(request: CloudHttpRequest): CloudHttpResponse {
            this.request = request
            return CloudHttpResponse(200, response)
        }
    }

    @Test fun openAiUsesResponsesApiAndBearerCredential() = runBlocking {
        val transport = CaptureTransport("""{"output":[{"type":"message","content":[{"type":"output_text","text":"hello from GPT"}]}]}""")
        val provider = CloudInferenceProvider(CloudProviderConfig(CloudProviderKind.OPENAI, "gpt-model", "openai.key", true), Secrets(mapOf("openai.key" to "secret-value")), transport)
        assertEquals("hello from GPT", provider.generate("hello"))
        assertEquals("https://api.openai.com/v1/responses", transport.request?.url)
        assertEquals("Bearer secret-value", transport.request?.headers?.get("Authorization"))
        assertFalse(transport.request!!.body.contains("secret-value"))
    }

    @Test fun anthropicUsesMessagesEndpointAndVersionHeader() = runBlocking {
        val transport = CaptureTransport("""{"content":[{"type":"text","text":"hello from Claude"}]}""")
        val provider = CloudInferenceProvider(CloudProviderConfig(CloudProviderKind.ANTHROPIC, "claude-model", "claude.key", true), Secrets(mapOf("claude.key" to "claude-secret")), transport)
        assertEquals("hello from Claude", provider.generate("hello"))
        assertEquals("https://api.anthropic.com/v1/messages", transport.request?.url)
        assertEquals("2023-06-01", transport.request?.headers?.get("anthropic-version"))
    }

    @Test fun geminiUsesGenerateContentAndHeaderCredential() = runBlocking {
        val transport = CaptureTransport("""{"candidates":[{"content":{"parts":[{"text":"hello from Gemini"}]}}]}""")
        val provider = CloudInferenceProvider(CloudProviderConfig(CloudProviderKind.GEMINI, "gemini-model", "gemini.key", true), Secrets(mapOf("gemini.key" to "gemini-secret")), transport)
        assertEquals("hello from Gemini", provider.generate("hello"))
        assertTrue(transport.request!!.url.endsWith("/v1beta/models/gemini-model:generateContent"))
        assertEquals("gemini-secret", transport.request?.headers?.get("x-goog-api-key"))
        assertFalse(transport.request!!.url.contains("gemini-secret"))
    }

    @Test fun xAiUsesResponsesApi() = runBlocking {
        val transport = CaptureTransport("""{"output":[{"type":"message","content":[{"type":"output_text","text":"hello from Grok"}]}]}""")
        val provider = CloudInferenceProvider(CloudProviderConfig(CloudProviderKind.XAI, "grok-model", "xai.key", true), Secrets(mapOf("xai.key" to "xai-secret")), transport)
        assertEquals("hello from Grok", provider.generate("hello"))
        assertEquals("https://api.x.ai/v1/responses", transport.request?.url)
    }

    @Test fun disabledOrMissingCredentialsAreUnavailable() {
        val disabled = CloudInferenceProvider(CloudProviderConfig(CloudProviderKind.OPENAI, "gpt-model", "missing", false), Secrets(emptyMap()))
        assertFalse(disabled.isAvailable())
        assertFailsWith<IllegalStateException> { runBlocking { disabled.generate("hello") } }
    }

    @Test fun registryCreatesOnlyOwnerEnabledProviders() {
        val providers = CloudProviderRegistry.create(
            listOf(
                CloudProviderConfig(CloudProviderKind.OPENAI, "gpt", "gpt", enabled = true),
                CloudProviderConfig(CloudProviderKind.XAI, "grok", "grok", enabled = false)
            ), Secrets(emptyMap()), CaptureTransport("{}")
        )
        assertEquals(listOf("openai"), providers.map { it.id })
    }
}
