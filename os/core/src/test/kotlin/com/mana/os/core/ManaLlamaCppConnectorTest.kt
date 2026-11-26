package com.mana.os.core

import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ManaLlamaCppConnectorTest {
    private class FakeRuntime : ManaLlamaCppRuntime {
        var loads = 0
        var releases = 0
        var lastPrompt: String? = null
        override fun loadModel(modelPath: String, contextSize: Int, threads: Int): Long {
            loads++
            return 42L
        }
        override fun generate(modelHandle: Long, prompt: String, maxTokens: Int, temperature: Float): String {
            lastPrompt = prompt
            return "local response"
        }
        override fun releaseModel(modelHandle: Long) { releases++ }
    }

    private fun envelope(prompt: String, sensitive: Boolean = false) =
        "MANA-REASONING/1\nsensitive=$sensitive\nprompt-length=${prompt.length}\n$prompt"

    @Test
    fun loadsGgufOnceAndUsesLocalRuntime() = runBlocking {
        val model = File.createTempFile("mana-test-", ".gguf")
        val runtime = FakeRuntime()
        try {
            val connector = ManaLlamaCppConnector(model, runtime)
            assertEquals("local response", connector.execute("chat.respond", envelope("hello")))
            assertEquals("local response", connector.execute("chat.respond", envelope("again")))
            assertEquals(1, runtime.loads)
            assertEquals("again", runtime.lastPrompt)
            connector.close()
            assertEquals(1, runtime.releases)
        } finally {
            model.delete()
        }
    }

    @Test
    fun rejectsWrongCapability() = runBlocking {
        val model = File.createTempFile("mana-test-", ".gguf")
        try {
            val connector = ManaLlamaCppConnector(model, FakeRuntime())
            assertFailsWith<IllegalArgumentException> {
                runBlocking { connector.execute("files.delete", envelope("hello")) }
            }
        } finally {
            model.delete()
        }
    }

    @Test
    fun rejectsMalformedPromptLength() = runBlocking {
        val model = File.createTempFile("mana-test-", ".gguf")
        try {
            val connector = ManaLlamaCppConnector(model, FakeRuntime())
            assertFailsWith<IllegalArgumentException> {
                runBlocking { connector.execute("chat.respond", "MANA-REASONING/1\nsensitive=false\nprompt-length=99\nhello") }
            }
        } finally {
            model.delete()
        }
    }

    @Test
    fun rejectsNonGgufFile() {
        val model = File.createTempFile("mana-test-", ".litertlm")
        try {
            assertFailsWith<IllegalArgumentException> {
                ManaLlamaCppConnector(model, FakeRuntime())
            }
        } finally {
            model.delete()
        }
    }
}
