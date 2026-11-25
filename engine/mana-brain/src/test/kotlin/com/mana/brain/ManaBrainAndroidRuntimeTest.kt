package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaBrainAndroidRuntimeTest {
    @Test
    fun runtimeReportsPackagedCoreButNotMissingLocalModelAsReady() {
        val runtime = ManaBrainAndroidRuntime.get()
        assertEquals(ManaBrainAndroidRuntime.ENGINE_VERSION, runtime.engineVersion())
        assertFalse(runtime.isReady())
    }

    @Test
    fun runtimeReturnsAnExplicitUnavailableErrorInsteadOfFabricatingText() {
        val runtime = ManaBrainAndroidRuntime.get()
        var completed = false
        var result: BrainResult? = null
        var error: Throwable? = null

        runtime.submit(BrainCall("android-runtime-test", "test prompt"),
            object : BrainCompletion {
                override fun onComplete(value: BrainResult?, failure: Throwable?) {
                    result = value
                    error = failure
                    completed = true
                }
            })

        assertTrue(completed)
        assertEquals(null, result)
        assertTrue(error is InferenceUnavailableException)
    }
}
