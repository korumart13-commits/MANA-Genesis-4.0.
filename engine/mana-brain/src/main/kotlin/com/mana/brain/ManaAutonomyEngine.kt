package com.mana.brain

/**
 * MANA's autonomous research loop.
 *
 * It gives MANA initiative over internal research, experiments, reflection and
 * versioned skill proposals. Concrete side effects still have to be provided by
 * registered capabilities; this engine cannot manufacture Android privileges.
 */
enum class AutonomyDomain {
    INTERNAL_SANDBOX,
    PUBLIC_READ_ONLY,
    DEVICE_API,
    EXTERNAL_SIDE_EFFECT
}

enum class OwnerRuleEffect { ALLOW, FORBID, REQUIRE_APPROVAL }

data class OwnerRule(
    val id: String,
    val capabilityId: String,
    val effect: OwnerRuleEffect
) {
    init {
        require(id.isNotBlank())
        require(capabilityId.isNotBlank())
    }
}

data class AutonomyCapability(
    val id: String,
    val domain: AutonomyDomain,
    val runtimePermissionGranted: Boolean = false
) {
    init { require(id.isNotBlank()) }
}

enum class AutonomyDecision { ALLOW, FORBID, REQUIRE_APPROVAL }

data class AutonomyDecisionRecord(
    val capabilityId: String,
    val decision: AutonomyDecision,
    val reason: String
)

/**
 * With no owner rules, MANA is maximally autonomous in its internal sandbox and
 * for public read-only research. Device APIs are available when Android grants
 * them. Irreversible/external side effects remain an OS/owner boundary.
 */
class ManaAutonomyPolicy(ownerRules: List<OwnerRule> = emptyList()) {
    private val rules = ownerRules.groupBy { it.capabilityId }

    init {
        require(ownerRules.map { it.id }.distinct().size == ownerRules.size) {
            "Owner rule ids must be unique"
        }
    }

    fun decide(capability: AutonomyCapability): AutonomyDecisionRecord {
        val explicit = rules[capability.id].orEmpty().lastOrNull()
        if (explicit != null) {
            return AutonomyDecisionRecord(
                capability.id,
                when (explicit.effect) {
                    OwnerRuleEffect.ALLOW -> AutonomyDecision.ALLOW
                    OwnerRuleEffect.FORBID -> AutonomyDecision.FORBID
                    OwnerRuleEffect.REQUIRE_APPROVAL -> AutonomyDecision.REQUIRE_APPROVAL
                },
                "Applied owner rule: ${explicit.id}"
            )
        }

        return when (capability.domain) {
            AutonomyDomain.INTERNAL_SANDBOX ->
                AutonomyDecisionRecord(capability.id, AutonomyDecision.ALLOW, "Autonomous internal research is enabled")
            AutonomyDomain.PUBLIC_READ_ONLY ->
                AutonomyDecisionRecord(capability.id, AutonomyDecision.ALLOW, "Public read-only research is enabled")
            AutonomyDomain.DEVICE_API ->
                if (capability.runtimePermissionGranted) {
                    AutonomyDecisionRecord(capability.id, AutonomyDecision.ALLOW, "Android granted the required runtime capability")
                } else {
                    AutonomyDecisionRecord(capability.id, AutonomyDecision.FORBID, "Android runtime permission is not granted")
                }
            AutonomyDomain.EXTERNAL_SIDE_EFFECT ->
                AutonomyDecisionRecord(capability.id, AutonomyDecision.REQUIRE_APPROVAL, "External side effects require owner/OS authorization")
        }
    }
}

enum class ResearchStatus { QUEUED, RUNNING, COMPLETED, FAILED, NEEDS_REVIEW }

data class ResearchGoal(
    val id: String,
    val description: String,
    val priority: Int = 0,
    val status: ResearchStatus = ResearchStatus.QUEUED
) {
    init {
        require(id.isNotBlank())
        require(description.isNotBlank())
    }
}

data class ResearchExperiment(
    val id: String,
    val goalId: String,
    val hypothesis: String,
    val method: String,
    val successCriteria: String
) {
    init {
        require(id.isNotBlank() && goalId.isNotBlank())
        require(hypothesis.isNotBlank() && method.isNotBlank() && successCriteria.isNotBlank())
    }
}

data class ExperimentOutcome(
    val success: Boolean,
    val verified: Boolean,
    val observation: String,
    val evidence: List<String> = emptyList()
)

data class ResearchJournalEntry(
    val goalId: String,
    val experimentId: String,
    val status: ResearchStatus,
    val observation: String,
    val evidence: List<String>,
    val reflection: String
)

interface ResearchPlanner {
    suspend fun propose(goal: ResearchGoal): ResearchExperiment
    suspend fun reflect(goal: ResearchGoal, experiment: ResearchExperiment, outcome: ExperimentOutcome): String
}

fun interface ResearchRunner {
    suspend fun run(experiment: ResearchExperiment): ExperimentOutcome
}

interface ResearchJournal {
    fun append(entry: ResearchJournalEntry)
    fun completedExperiments(goalId: String): Int
}

data class AutonomyCycleReport(
    val goalId: String,
    val experimentId: String?,
    val status: ResearchStatus,
    val summary: String
)

/**
 * Runs one bounded research cycle at a time. MANA can repeatedly call cycle()
 * from its scheduler; every outcome is journaled and unverified results never
 * become successful learning.
 */
class ManaAutonomyEngine(
    private val planner: ResearchPlanner,
    private val runner: ResearchRunner,
    private val journal: ResearchJournal
) {
    suspend fun cycle(goal: ResearchGoal): AutonomyCycleReport {
        val experiment = planner.propose(goal)
        require(experiment.goalId == goal.id) { "Planner returned an experiment for another goal" }

        return try {
            val outcome = runner.run(experiment)
            val status = when {
                !outcome.success -> ResearchStatus.FAILED
                !outcome.verified -> ResearchStatus.NEEDS_REVIEW
                else -> ResearchStatus.COMPLETED
            }
            val reflection = planner.reflect(goal, experiment, outcome)
            journal.append(
                ResearchJournalEntry(
                    goal.id,
                    experiment.id,
                    status,
                    outcome.observation,
                    outcome.evidence,
                    reflection
                )
            )
            AutonomyCycleReport(
                goal.id,
                experiment.id,
                status,
                if (status == ResearchStatus.COMPLETED) reflection
                else "Result was recorded but not accepted as verified learning: ${outcome.observation}"
            )
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val message = error.message?.take(240) ?: "Research cycle failed"
            journal.append(
                ResearchJournalEntry(
                    goal.id, experiment.id, ResearchStatus.FAILED, message, emptyList(),
                    "Retry only after diagnosing the recorded failure"
                )
            )
            AutonomyCycleReport(goal.id, experiment.id, ResearchStatus.FAILED, message)
        }
    }
}
