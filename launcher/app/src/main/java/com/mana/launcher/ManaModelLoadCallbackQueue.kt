package com.mana.launcher

/**
 * Coalesces concurrent model-bootstrap requests and delivers the same completion
 * to every caller. In particular, a missing model is a completed failure, not a
 * silent early return that leaves the one-action consent flow waiting forever.
 */
internal class ManaModelLoadCallbackQueue {
    private val lock = Any()
    private var inFlight = false
    private val callbacks = mutableListOf<(Result<String>) -> Unit>()

    /** Returns true only to the caller responsible for starting the shared load. */
    fun joinOrStart(callback: ((Result<String>) -> Unit)?): Boolean = synchronized(lock) {
        callback?.let(callbacks::add)
        if (inFlight) {
            false
        } else {
            inFlight = true
            true
        }
    }

    /** Completes the shared operation once and notifies all waiting callers. */
    fun complete(result: Result<String>) {
        val pending = synchronized(lock) {
            inFlight = false
            callbacks.toList().also { callbacks.clear() }
        }
        pending.forEach { listener -> runCatching { listener(result) } }
    }
}
