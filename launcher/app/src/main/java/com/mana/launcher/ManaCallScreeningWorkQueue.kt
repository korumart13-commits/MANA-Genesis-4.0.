package com.mana.launcher

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Bounded worker for non-critical caller-name lookup and owner notifications.
 * Telecom screening responses are sent before this queue is touched.
 */
internal object ManaCallScreeningWorkQueue {
    const val MAX_PENDING_LOOKUPS = 16

    val executor: ThreadPoolExecutor by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        create()
    }

    fun create(
        threadFactory: ThreadFactory = ThreadFactory { task ->
            Thread(task, "MANA-CallScreening").apply { isDaemon = true }
        }
    ): ThreadPoolExecutor = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_PENDING_LOOKUPS),
        threadFactory,
        ThreadPoolExecutor.AbortPolicy()
    )
}
