package com.mana.launcher

/**
 * Deterministic retry policy for transient MANA Engine process/service failures.
 * Retries are capped and never bypass Android background-execution controls.
 */
internal class ManaServiceResiliencePolicy(
    private val maxAttempts: Int = 5,
    private val baseDelayMs: Long = 2_000L,
    private val maxDelayMs: Long = 60_000L,
    private val stableWindowMs: Long = 60_000L,
    private val failureWindowMs: Long = 10 * 60_000L
) {
    data class RetryDecision(val attempt: Int, val delayMs: Long, val exhausted: Boolean)

    private var attempts = 0
    private var lastFailureAtMs: Long? = null
    private var healthySinceMs: Long? = null

    init {
        require(maxAttempts > 0)
        require(baseDelayMs > 0L)
        require(maxDelayMs >= baseDelayMs)
        require(stableWindowMs > 0L)
        require(failureWindowMs >= stableWindowMs)
    }

    @Synchronized
    fun recordFailure(nowMs: Long): RetryDecision {
        require(nowMs >= 0L)
        val previous = lastFailureAtMs
        if (previous != null && nowMs - previous > failureWindowMs) attempts = 0
        healthySinceMs = null
        lastFailureAtMs = nowMs
        if (attempts >= maxAttempts) return RetryDecision(attempts, 0L, exhausted = true)
        attempts += 1
        val multiplier = 1L shl (attempts - 1).coerceAtMost(30)
        val delay = if (baseDelayMs > maxDelayMs / multiplier) maxDelayMs
            else (baseDelayMs * multiplier).coerceAtMost(maxDelayMs)
        return RetryDecision(attempts, delay, exhausted = false)
    }

    @Synchronized
    fun recordHealthy(nowMs: Long) {
        require(nowMs >= 0L)
        if (healthySinceMs == null) healthySinceMs = nowMs
    }

    @Synchronized
    fun resetIfStable(nowMs: Long): Boolean {
        require(nowMs >= 0L)
        val since = healthySinceMs ?: return false
        if (nowMs - since < stableWindowMs) return false
        attempts = 0
        lastFailureAtMs = null
        healthySinceMs = null
        return true
    }

    @Synchronized
    fun restore(savedAttempts: Int, savedLastFailureAtMs: Long?) {
        require(savedAttempts in 0..maxAttempts)
        require(savedLastFailureAtMs == null || savedLastFailureAtMs >= 0L)
        attempts = savedAttempts
        lastFailureAtMs = savedLastFailureAtMs
        healthySinceMs = null
    }

    @Synchronized
    fun snapshot(): Pair<Int, Long?> = attempts to lastFailureAtMs

    @Synchronized
    fun attempts(): Int = attempts
}
