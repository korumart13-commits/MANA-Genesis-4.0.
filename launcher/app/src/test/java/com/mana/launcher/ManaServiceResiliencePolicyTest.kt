package com.mana.launcher

import org.junit.Assert.*
import org.junit.Test

class ManaServiceResiliencePolicyTest {
    @Test fun exponentialBackoffIsBounded() {
        val policy = ManaServiceResiliencePolicy(maxAttempts = 6, baseDelayMs = 1_000, maxDelayMs = 5_000, jitter = { 0L })
        assertEquals(1_000L, policy.recordFailure(0).delayMs)
        assertEquals(2_000L, policy.recordFailure(1_000).delayMs)
        assertEquals(4_000L, policy.recordFailure(3_000).delayMs)
        assertEquals(5_000L, policy.recordFailure(7_000).delayMs)
    }

    @Test fun retryBudgetEventuallyExhausts() {
        val policy = ManaServiceResiliencePolicy(maxAttempts = 2, jitter = { 0L })
        assertFalse(policy.recordFailure(0).exhausted)
        assertFalse(policy.recordFailure(1).exhausted)
        val exhausted = policy.recordFailure(2)
        assertTrue(exhausted.exhausted)
        assertEquals(2, exhausted.attempt)
        assertEquals(0L, exhausted.delayMs)
    }

    @Test fun stableHealthResetsRetryBudgetOnlyAfterWindow() {
        val policy = ManaServiceResiliencePolicy(maxAttempts = 1, stableWindowMs = 10, jitter = { 0L })
        policy.recordFailure(0)
        policy.recordHealthy(5)
        assertFalse(policy.resetIfStable(14))
        assertTrue(policy.resetIfStable(15))
        assertEquals(0, policy.attempts())
        assertFalse(policy.recordFailure(16).exhausted)
    }

    @Test fun failuresOutsideWindowStartNewBudget() {
        val policy = ManaServiceResiliencePolicy(maxAttempts = 1, stableWindowMs = 50, failureWindowMs = 100, jitter = { 0L })
        assertFalse(policy.recordFailure(0).exhausted)
        assertTrue(policy.recordFailure(50).exhausted)
        assertFalse(policy.recordFailure(151).exhausted)
    }

    @Test fun failureClearsPendingHealthyWindow() {
        val policy = ManaServiceResiliencePolicy(maxAttempts = 3, stableWindowMs = 50, jitter = { 0L })
        policy.recordFailure(0)
        policy.recordHealthy(10)
        policy.recordFailure(20)
        assertFalse(policy.resetIfStable(100))
        assertEquals(2, policy.attempts())
    }
}
