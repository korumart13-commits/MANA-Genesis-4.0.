package com.mana.launcher

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

/** A cancellation handle tied to one inference request, never another queued request. */
class LocalInferenceCancellation {
    private val cancelled = AtomicBoolean(false)
    private val job = AtomicReference<Job?>(null)

    internal fun attach(activeJob: Job) {
        job.set(activeJob)
        if (cancelled.get()) activeJob.cancel(CancellationException("MANA inference request cancelled"))
    }

    fun cancel() {
        if (cancelled.compareAndSet(false, true)) {
            job.get()?.cancel(CancellationException("MANA inference request cancelled"))
        }
    }

    fun isCancelled(): Boolean = cancelled.get()
}

/**
 * Backend-neutral contract for local model execution.
 * Implementations must serialize access to native/runtime state and never block the UI thread.
 */
interface LocalInferenceRuntime {
    fun isReady(): Boolean
    fun modelName(): String?
    fun load(modelFile: File, callback: (Result<String>) -> Unit)
    fun generate(prompt: String, onChunk: (String) -> Unit, callback: (Result<Unit>) -> Unit): LocalInferenceCancellation
    /** Cancels all outstanding work and releases native resources. */
    fun shutdown()
}
