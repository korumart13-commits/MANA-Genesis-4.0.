package com.mana.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ManaModelLoadCallbackQueueTest {
    @Test
    fun coalescedCallersAllReceiveMissingModelFailureAndCanRetry() {
        val queue = ManaModelLoadCallbackQueue()
        var first: String? = null
        var second: String? = null

        assertTrue(queue.joinOrStart { result -> first = result.exceptionOrNull()?.message })
        assertFalse(queue.joinOrStart { result -> second = result.exceptionOrNull()?.message })

        queue.complete(Result.failure<String>(
            IllegalStateException("No verified local model is installed.")
        ))

        assertEquals("No verified local model is installed.", first)
        assertEquals(first, second)
        assertTrue(queue.joinOrStart(null))
        queue.complete(Result.success("Gemma 4 E2B · CPU"))
    }

    @Test
    fun completionWithoutListenersDoesNotRetainOldCallbacks() {
        val queue = ManaModelLoadCallbackQueue()
        assertTrue(queue.joinOrStart(null))
        queue.complete(Result.success("Gemma"))
        assertTrue(queue.joinOrStart(null))
        queue.complete(Result.success("Gemma"))
    }
}
