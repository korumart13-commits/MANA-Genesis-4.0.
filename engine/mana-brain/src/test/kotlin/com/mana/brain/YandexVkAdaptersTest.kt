package com.mana.brain

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class YandexVkAdaptersTest {
    private class Secrets(private val values: Map<String, String>) : ManaSecretStore {
        override fun readSecret(alias: String): String? = values[alias]
    }
    private class JsonTransport(private val response: CloudHttpResponse) : CloudHttpTransport {
        var request: CloudHttpRequest? = null
        override suspend fun post(request: CloudHttpRequest): CloudHttpResponse {
            this.request = request
            return response
        }
    }
    private class VkTransport(private val response: CloudHttpResponse) : VkFormTransport {
        var url: String? = null
        var fields: Map<String, String>? = null
        override suspend fun postForm(url: String, fields: Map<String, String>): CloudHttpResponse {
            this.url = url
            this.fields = fields
            return response
        }
    }

    @Test fun yandexUsesOfficialAiStudioCompletionEndpointAndIamToken() = runBlocking {
        val transport = JsonTransport(CloudHttpResponse(200, """{"alternatives":[{"message":{"text":"Привет от Yandex"}}]}"""))
        val provider = YandexAiStudioProvider(
            YandexAiStudioConfig("folder-123", "gpt://folder-123/yandexgpt/latest", "yandex.iam", enabled = true),
            Secrets(mapOf("yandex.iam" to "iam-secret-value")), transport
        )
        assertTrue(provider.isAvailable())
        assertEquals("Привет от Yandex", provider.generate("Привет"))
        assertEquals("https://ai.api.cloud.yandex.net/foundationModels/v1/completion", transport.request?.url)
        assertEquals("Bearer iam-secret-value", transport.request?.headers?.get("Authorization"))
        assertEquals("folder-123", transport.request?.headers?.get("x-folder-id"))
        assertFalse(transport.request!!.body.contains("iam-secret-value"))
    }

    @Test fun yandexRejectsModelUriFromAnotherFolder() {
        assertFailsWith<IllegalArgumentException> {
            YandexAiStudioConfig("folder-123", "gpt://another-folder/yandexgpt/latest", "token")
        }
    }

    @Test fun vkProfileLookupUsesOnlyReadOnlyUsersGetAndPostsTokenInBody() = runBlocking {
        val transport = VkTransport(CloudHttpResponse(200, """{"response":[{"id":42,"first_name":"Mana","last_name":"Owner","screen_name":"mana_owner"}]}"""))
        val client = VkReadOnlyClient(Secrets(mapOf("vk.token" to "vk-secret-value")), "vk.token", enabled = true, transport = transport)
        val profile = client.getUserProfile(42)
        assertEquals(42L, profile.id)
        assertEquals("Mana", profile.firstName)
        assertEquals("mana_owner", profile.screenName)
        assertEquals("https://api.vk.com/method/users.get", transport.url)
        assertEquals("vk-secret-value", transport.fields?.get("access_token"))
        assertFalse(transport.url!!.contains("vk-secret-value"))
    }

    @Test fun disabledVkConnectorFailsClosed() {
        val client = VkReadOnlyClient(Secrets(mapOf("vk.token" to "secret")), "vk.token", enabled = false, transport = VkTransport(CloudHttpResponse(200, "{}")))
        assertFalse(client.isAvailable())
        assertFailsWith<IllegalStateException> { runBlocking { client.getUserProfile(42) } }
    }
}
