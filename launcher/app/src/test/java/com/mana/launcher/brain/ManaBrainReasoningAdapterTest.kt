package com.mana.launcher.brain

import com.mana.brain.InferenceProvider
import com.mana.brain.ManaBrain
import com.mana.os.core.ManaReasoningRequest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManaBrainReasoningAdapterTest {
    private class Provider(
        override val id: String,
        private val available: Boolean,
        private val result: String
    ) : InferenceProvider {
        override fun isAvailable() = available
        override suspend fun generate(prompt: String): String = "$result:$prompt"
    }

    @Test fun mapsLocalBrainResponseIntoOsReasoningPort() = runBlocking {
        val adapter = ManaBrainReasoningAdapter(
            ManaBrain(Provider("local-test", true, "local"))
        )
        val result = adapter.respond(
            ManaReasoningRequest("hello", allowCloud = false, sensitive = false, preferredCloudProvider = null)
        )
        assertEquals("local-test", result.provider)
        assertTrue(result.local)
        assertEquals("local:hello", result.text)
    }

    @Test fun forwardsExplicitCloudPermissionAndProviderSelection() = runBlocking {
        val adapter = ManaBrainReasoningAdapter(
            ManaBrain(
                Provider("local-test", false, "local"),
                listOf(
                    Provider("cloud-a", true, "a"),
                    Provider("cloud-b", true, "b")
                )
            )
        )
        val result = adapter.respond(
            ManaReasoningRequest("hello", allowCloud = true, sensitive = false, preferredCloudProvider = "cloud-b")
        )
        assertEquals("cloud-b", result.provider)
        assertEquals("b:hello", result.text)
        assertTrue(!result.local)
    }
}
