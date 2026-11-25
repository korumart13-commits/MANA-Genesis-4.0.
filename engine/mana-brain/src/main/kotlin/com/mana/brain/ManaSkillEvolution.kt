package com.mana.brain

/**
 * A skill is a versioned, reviewable procedure, not hidden executable code.
 * Runtime adapters decide how approved skills are exposed as capabilities.
 */
enum class SkillReviewStatus { PROPOSED, ACCEPTED, REJECTED }

data class ManaSkill(
    val id: String,
    val version: Int,
    val name: String,
    val description: String,
    val procedure: String,
    val basedOnEvidence: List<String> = emptyList(),
    val status: SkillReviewStatus = SkillReviewStatus.PROPOSED
) {
    init {
        require(id.isNotBlank() && name.isNotBlank())
        require(version > 0)
        require(description.isNotBlank() && procedure.isNotBlank())
    }
}

data class SkillEvaluation(
    val passed: Boolean,
    val independentlyVerified: Boolean,
    val evidence: List<String>,
    val notes: String
)

fun interface SkillEvaluator {
    suspend fun evaluate(candidate: ManaSkill): SkillEvaluation
}

interface ManaSkillStore {
    fun latest(id: String): ManaSkill?
    fun save(skill: ManaSkill)
}

/**
 * MANA may propose and test new skills autonomously. A candidate becomes
 * available to the runtime only after an independent passing evaluation.
 */
class ManaSkillEvolution(
    private val evaluator: SkillEvaluator,
    private val store: ManaSkillStore
) {
    suspend fun evaluate(candidate: ManaSkill): ManaSkill {
        val previous = store.latest(candidate.id)
        require(previous == null || candidate.version > previous.version) {
            "A skill update must have a higher version than the stored version"
        }
        require(candidate.status == SkillReviewStatus.PROPOSED) {
            "Only proposed skills can enter evaluation"
        }

        val result = evaluator.evaluate(candidate)
        val accepted = result.passed && result.independentlyVerified && result.evidence.isNotEmpty()
        val reviewed = candidate.copy(
            basedOnEvidence = result.evidence,
            status = if (accepted) SkillReviewStatus.ACCEPTED else SkillReviewStatus.REJECTED
        )
        store.save(reviewed)
        return reviewed
    }
}
