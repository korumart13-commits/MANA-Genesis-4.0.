package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class CloudProviderAdaptersTest {
    private class MemorySecrets(private val values: Map<String, String>) : ManaSecretStore {
        override fun readSecret(alias: String): String? = values[alias]
    }

    private class RecordingTransport(
        private val response: CloudHttpResponse
    ) : CloudHttpTransport {
        var request: CloudHttpRequest? = null
        override suspend fun post(request: CloudHttpRequest): CloudHttpResponse {
            this.request = request
            return response
        }
    }

    @Test
    fun openAiUsesResponsesAndXaiUsesOfficialChatCompletionsEndpoint() = runBlocking {
        val cases = listOf(
            Triple(
                CloudProviderKind.OPENAI,
                """{"output":[{"type":"message","content":[{"type":"output_text","text":"hello from cloud"}]}]}""",
                "https://api.openai.com/v1/responses"
            ),
            Triple(
                CloudProviderKind.XAI,
                """{"choices":[{"message":{"role":"assistant","content":"hello from cloud"}}]}""",
                "https://api.x.ai/v1/chat/completions"
            )
        )
        for ((kind, response, expectedUrl) in cases) {
            val transport = RecordingTransport(CloudHttpResponse(200, response))
            val provider = CloudInferenceProvider(
                CloudProviderConfig(kind, "model-test", "secret", enabled = true),
                MemorySecrets(mapOf("secret" to "test-secret-123456")),
                transport
            )
            assertEquals("hello from cloud", provider.generate("hello"))
            assertEquals(expectedUrl, transport.request?.url)
            assertEquals("Bearer test-secret-123456", transport.request?.headers?.get("Authorization"))
            val requestBody = transport.request?.body.orEmpty()
            if (kind == CloudProviderKind.OPENAI) {
                assertTrue(requestBody.contains("\\"input\\":\\"hello\\""))
            } else {
                assertTrue(requestBody.contains("\\"messages\\":[{\\"role\\":\\"user\\",\\"content\\":\\"hello\\"}]"))
            }
        }
    }

    @Test
    fun anthropicUsesMessagesApiAndExtractsText() = runBlocking {
        val transport = RecordingTransport(
            CloudHttpResponse(200, """{"content":[{"type":"text","text":"claude answer"}]}""")
        )
        val provider = CloudInferenceProvider(
            CloudProviderConfig(CloudProviderKind.ANTHROPIC, "claude-test", "secret", enabled = true),
            MemorySecrets(mapOf("secret" to "anthropic-secret-123")),
            transport
        )
        assertEquals("claude answer", provider.generate("hello"))
        assertTrue(transport.request?.url?.endsWith("/v1/messages") == true)
        assertEquals("anthropic-secret-123", transport.request?.headers?.get("x-api-key"))
        assertEquals("2023-06-01", transport.request?.headers?.get("anthropic-version"))
    }

    @Test
    fun geminiUsesGenerateContentAndExtractsText() = runBlocking {
        val transport = RecordingTransport(
            CloudHttpResponse(200, """{"candidates":[{"content":{"parts":[{"text":"gemini answer"}]}}]}""")
        )
        val provider = CloudInferenceProvider(
            CloudProviderConfig(CloudProviderKind.GEMINI, "gemini-test", "secret", enabled = true),
            MemorySecrets(mapOf("secret" to "gemini-secret-123")),
            transport
        )
        assertEquals("gemini answer", provider.generate("hello"))
        assertTrue(transport.request?.url?.contains("/models/gemini-test:generateContent") == true)
        assertEquals("gemini-secret-123", transport.request?.headers?.get("x-goog-api-key"))
    }

    @Test
    fun disabledProviderNeverSendsRequest() = runBlocking {
        val transport = RecordingTransport(CloudHttpResponse(200, """{"output":[]}"""))
        val provider = CloudInferenceProvider(
            CloudProviderConfig(CloudProviderKind.OPENAI, "model-test", "secret", enabled = false),
            MemorySecrets(mapOf("secret" to "test-secret-123456")),
            transport
        )
        assertFalse(provider.isAvailable())
        assertFailsWith<IllegalStateException> { provider.generate("hello") }
        assertEquals(null, transport.request)
    }

    @Test
    fun registryIncludesOnlyEnabledProvidersAndRejectsDuplicates() {
        val secrets = MemorySecrets(mapOf("secret" to "test-secret-123456"))
        val enabled = CloudProviderConfig(CloudProviderKind.OPENAI, "model-test", "secret", enabled = true)
        val disabled = CloudProviderConfig(CloudProviderKind.GEMINI, "model-test", "secret", enabled = false)
        assertEquals(listOf("openai"), CloudProviderRegistry.create(listOf(enabled, disabled), secrets).map { it.id })
        assertFailsWith<IllegalArgumentException> {
            CloudProviderRegistry.create(listOf(enabled, enabled), secrets)
        }
    }
}
