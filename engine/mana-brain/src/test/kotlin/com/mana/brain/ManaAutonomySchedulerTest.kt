package com.mana.brain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ManaAutonomySchedulerTest {
    @Test
    fun schedulerStartsOnlyOneWorkerAndCanBeStopped() {
        val scope = CoroutineScope(Dispatchers.Unconfined + Job())
        val scheduler = ManaAutonomyScheduler(
            engine = ManaAutonomyEngine(
                planner = object : ResearchPlanner {
                    override suspend fun propose(goal: ResearchGoal) =
                        ResearchExperiment("e", goal.id, "h", "m", "c")
                    override suspend fun reflect(goal: ResearchGoal, experiment: ResearchExperiment, outcome: ExperimentOutcome) = "r"
                },
                runner = ResearchRunner { ExperimentOutcome(true, true, "ok", listOf("test")) },
                journal = object : ResearchJournal {
                    override fun append(entry: ResearchJournalEntry) = Unit
                    override fun completedExperiments(goalId: String) = 0
                }
            ),
            goals = ResearchGoalSource { null }
        )

        assertTrue(scheduler.start(scope))
        assertFalse(scheduler.start(scope))
        assertTrue(scheduler.isRunning())
        scheduler.stop()
        assertFalse(scheduler.isRunning())
        scope.coroutineContext[Job]?.cancel()
    }
}
