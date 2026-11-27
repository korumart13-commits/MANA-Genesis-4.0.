package com.mana.launcher

import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ManaCallScreeningWorkQueueTest {
    @Test
    fun queueIsBoundedAndRejectsOverflowWithoutRunningOnCallerThread() {
        val enteredWorker = CountDownLatch(1)
        val releaseWorker = CountDownLatch(1)
        val executor = ManaCallScreeningWorkQueue.create()
        try {
            executor.execute {
                enteredWorker.countDown()
                releaseWorker.await(5, TimeUnit.SECONDS)
            }
            assertTrue("worker did not start", enteredWorker.await(5, TimeUnit.SECONDS))

            repeat(ManaCallScreeningWorkQueue.MAX_PENDING_LOOKUPS) {
                executor.execute { }
            }

            assertEquals(ManaCallScreeningWorkQueue.MAX_PENDING_LOOKUPS, executor.queue.size)
            assertThrows(RejectedExecutionException::class.java) {
                executor.execute { }
            }
        } finally {
            releaseWorker.countDown()
            executor.shutdown()
            assertTrue("executor did not terminate", executor.awaitTermination(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun executorUsesOnlyOneWorkerThread() {
        val executor = ManaCallScreeningWorkQueue.create()
        try {
            assertEquals(1, executor.corePoolSize)
            assertEquals(1, executor.maximumPoolSize)
            assertEquals(ManaCallScreeningWorkQueue.MAX_PENDING_LOOKUPS, executor.queue.remainingCapacity() + executor.queue.size)
        } finally {
            executor.shutdownNow()
        }
    }
}
