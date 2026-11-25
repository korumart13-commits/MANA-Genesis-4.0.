package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class ManaAutonomyPolicyTest {
    @Test
    fun noOwnerRulesAllowsAutonomousSandboxAndPublicReadOnlyResearch() {
        val policy = ManaAutonomyPolicy()
        assertEquals(AutonomyDecision.ALLOW, policy.decide(AutonomyCapability("lab", AutonomyDomain.INTERNAL_SANDBOX)).decision)
        assertEquals(AutonomyDecision.ALLOW, policy.decide(AutonomyCapability("research", AutonomyDomain.PUBLIC_READ_ONLY)).decision)
    }

    @Test
    fun ownerCanAddAProhibitionAfterLaunch() {
        val policy = ManaAutonomyPolicy(listOf(OwnerRule("no-camera", "camera.capture", OwnerRuleEffect.FORBID)))
        assertEquals(AutonomyDecision.FORBID, policy.decide(AutonomyCapability("camera.capture", AutonomyDomain.DEVICE_API, true)).decision)
    }

    @Test
    fun deviceApisRequireTheRealAndroidGrant() {
        val policy = ManaAutonomyPolicy()
        assertEquals(AutonomyDecision.FORBID, policy.decide(AutonomyCapability("torch", AutonomyDomain.DEVICE_API)).decision)
        assertEquals(AutonomyDecision.ALLOW, policy.decide(AutonomyCapability("torch", AutonomyDomain.DEVICE_API, true)).decision)
    }

    @Test
    fun ownerAllowCannotBypassMissingAndroidPermission() {
        val policy = ManaAutonomyPolicy(listOf(OwnerRule("allow-camera", "camera.capture", OwnerRuleEffect.ALLOW)))
        assertEquals(
            AutonomyDecision.FORBID,
            policy.decide(AutonomyCapability("camera.capture", AutonomyDomain.DEVICE_API, false)).decision
        )
    }

    @Test
    fun externalSideEffectsRemainOwnerOrOsAuthorized() {
        assertEquals(
            AutonomyDecision.REQUIRE_APPROVAL,
            ManaAutonomyPolicy().decide(AutonomyCapability("send-message", AutonomyDomain.EXTERNAL_SIDE_EFFECT)).decision
        )
    }
}

class ManaAutonomyEngineTest {
    private class MemoryJournal : ResearchJournal {
        val entries = mutableListOf<ResearchJournalEntry>()
        override fun append(entry: ResearchJournalEntry) { entries += entry }
        override fun completedExperiments(goalId: String) = entries.count {
            it.goalId == goalId && it.status == ResearchStatus.COMPLETED
        }
    }

    @Test
    fun verifiedExperimentIsReflectedAndJournaled() = runBlocking {
        val journal = MemoryJournal()
        val planner = object : ResearchPlanner {
            override suspend fun propose(goal: ResearchGoal) =
                ResearchExperiment("e1", goal.id, "H", "M", "C")
            override suspend fun reflect(goal: ResearchGoal, experiment: ResearchExperiment, outcome: ExperimentOutcome) =
                "Next hypothesis"
        }
        val engine = ManaAutonomyEngine(
            planner,
            ResearchRunner { ExperimentOutcome(true, true, "Observed improvement", listOf("test:passed")) },
            journal
        )
        val report = engine.cycle(ResearchGoal("g1", "Explore a new skill"))
        assertEquals(ResearchStatus.COMPLETED, report.status)
        assertEquals("Next hypothesis", journal.entries.single().reflection)
        assertEquals(1, journal.completedExperiments("g1"))
    }

    @Test
    fun plannerFailureIsRecordedForRecovery() = runBlocking {
        val journal = MemoryJournal()
        val planner = object : ResearchPlanner {
            override suspend fun propose(goal: ResearchGoal): ResearchExperiment {
                error("planner unavailable")
            }
            override suspend fun reflect(goal: ResearchGoal, experiment: ResearchExperiment, outcome: ExperimentOutcome) = "unused"
        }
        val engine = ManaAutonomyEngine(
            planner,
            ResearchRunner { error("runner must not execute") },
            journal
        )
        val report = engine.cycle(ResearchGoal("g3", "Recover from planner failure"))
        assertEquals(ResearchStatus.FAILED, report.status)
        assertEquals("proposal", journal.entries.single().experimentId)
        assertEquals("planner unavailable", journal.entries.single().observation)
    }

    @Test
    fun unverifiedExperimentNeverCountsAsCompletedLearning() = runBlocking {
        val journal = MemoryJournal()
        val planner = object : ResearchPlanner {
            override suspend fun propose(goal: ResearchGoal) =
                ResearchExperiment("e2", goal.id, "H", "M", "C")
            override suspend fun reflect(goal: ResearchGoal, experiment: ResearchExperiment, outcome: ExperimentOutcome) = "Review"
        }
        val engine = ManaAutonomyEngine(
            planner,
            ResearchRunner { ExperimentOutcome(true, false, "No independent verification") },
            journal
        )
        val report = engine.cycle(ResearchGoal("g2", "Try an experiment"))
        assertEquals(ResearchStatus.NEEDS_REVIEW, report.status)
        assertEquals(0, journal.completedExperiments("g2"))
    }
}
