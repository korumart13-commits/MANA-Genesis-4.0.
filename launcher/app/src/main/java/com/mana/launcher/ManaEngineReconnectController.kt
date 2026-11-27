package com.mana.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper

/**
 * Lifecycle-scoped reconnect scheduler. It only retries while the owning app process is alive;
 * it never starts a foreground service from the background or bypasses Android restrictions.
 */
internal class ManaEngineReconnectController(
    context: Context,
    private val retry: () -> Unit
) {
    private val preferences = context.applicationContext.getSharedPreferences("mana.engine.recovery", Context.MODE_PRIVATE)
    private val handler = Handler(Looper.getMainLooper())
    private val policy = ManaServiceResiliencePolicy()
    private var scheduled = false

    init {
        val attempts = preferences.getInt(KEY_ATTEMPTS, 0).coerceIn(0, MAX_ATTEMPTS)
        val lastFailure = if (preferences.contains(KEY_LAST_FAILURE)) preferences.getLong(KEY_LAST_FAILURE, 0L) else null
        policy.restore(attempts, lastFailure)
    }

    @Synchronized
    fun onFailure(): Boolean {
        if (scheduled) return true
        val decision = policy.recordFailure(System.currentTimeMillis().coerceAtLeast(0L))
        persist()
        if (decision.exhausted) return false
        scheduled = true
        handler.postDelayed({
            synchronized(this) { scheduled = false }
            retry()
        }, decision.delayMs)
        return true
    }

    @Synchronized
    fun onConnected() {
        val now = System.currentTimeMillis().coerceAtLeast(0L)
        policy.recordHealthy(now)
        handler.removeCallbacksAndMessages(null)
        scheduled = false
        handler.postDelayed({
            synchronized(this) {
                if (policy.resetIfStable(System.currentTimeMillis().coerceAtLeast(0L))) persist()
            }
        }, STABLE_HEALTH_MS)
    }

    @Synchronized
    fun cancel() {
        handler.removeCallbacksAndMessages(null)
        scheduled = false
    }

    @Synchronized
    private fun persist() {
        val (attempts, lastFailure) = policy.snapshot()
        val editor = preferences.edit().putInt(KEY_ATTEMPTS, attempts)
        if (lastFailure == null) editor.remove(KEY_LAST_FAILURE)
        else editor.putLong(KEY_LAST_FAILURE, lastFailure)
        editor.apply()
    }

    private companion object {
        const val KEY_ATTEMPTS = "attempts"
        const val KEY_LAST_FAILURE = "last_failure_ms"
        const val MAX_ATTEMPTS = 5
        const val STABLE_HEALTH_MS = 60_000L
    }
}
