package com.mana.launcher

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ManaBrainBridgeTest {
    @Test
    fun requestAfterShutdownCompletesCallbackWithFailure() {
        val runtime = object : LocalInferenceRuntime {
            override fun isReady() = false
            override fun modelName(): String? = null
            override fun load(modelFile: File, callback: (Result<String>) -> Unit) {
                callback(Result.failure(UnsupportedOperationException()))
            }
            override fun generate(
                prompt: String,
                onChunk: (String) -> Unit,
                callback: (Result<Unit>) -> Unit
            ): LocalInferenceCancellation {
                callback(Result.failure(UnsupportedOperationException()))
                return LocalInferenceCancellation().also { it.cancel() }
            }
            override fun shutdown() = Unit
        }
        val bridge = ManaBrainBridge(runtime, emptyList())
        bridge.shutdown()
        bridge.shutdown()

        val completed = CountDownLatch(1)
        var succeeded: Boolean? = null
        bridge.respond(
            prompt = "test",
            onChunk = {},
            callback = { result ->
                succeeded = result.isSuccess
                completed.countDown()
            }
        )

        assertTrue("Callback was not completed", completed.await(1, TimeUnit.SECONDS))
        assertTrue("A request after shutdown must fail", succeeded == false)
    }
}
