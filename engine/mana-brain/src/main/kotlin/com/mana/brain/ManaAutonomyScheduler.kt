package com.mana.brain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun interface ResearchGoalSource {
    /** Return the next goal, or null when the queue is currently empty. */
    suspend fun nextGoal(): ResearchGoal?
}

/**
 * Single-worker continuous research scheduler.
 *
 * The host owns the CoroutineScope and decides when background execution may run.
 * This scheduler never creates an unbounded worker pool and always waits between
 * cycles, including when no goals are queued.
 */
class ManaAutonomyScheduler(
    private val engine: ManaAutonomyEngine,
    private val goals: ResearchGoalSource,
    private val onReport: suspend (AutonomyCycleReport) -> Unit = {},
    private val idleDelayMillis: Long = 30_000L,
    private val cycleDelayMillis: Long = 1_000L
) {
    private var worker: Job? = null

    init {
        require(idleDelayMillis >= 1_000L) { "Idle delay must be at least one second" }
        require(cycleDelayMillis >= 1_000L) { "Cycle delay must be at least one second" }
    }

    @Synchronized
    fun start(scope: CoroutineScope): Boolean {
        if (worker?.isActive == true) return false
        worker = scope.launch {
            while (isActive) {
                try {
                    val goal = goals.nextGoal()
                    if (goal == null) {
                        delay(idleDelayMillis)
                    } else {
                        onReport(engine.cycle(goal))
                        delay(cycleDelayMillis)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // Keep the worker alive after a recoverable adapter failure.
                    delay(idleDelayMillis)
                }
            }
        }
        return true
    }

    @Synchronized
    fun stop() {
        worker?.cancel()
        worker = null
    }

    @Synchronized
    fun isRunning(): Boolean = worker?.isActive == true
}
