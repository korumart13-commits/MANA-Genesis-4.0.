package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

class ManaSkillEvolutionTest {
    private class InMemorySkillStore : ManaSkillStore {
        private val skills = mutableMapOf<String, ManaSkill>()
        override fun latest(id: String) = skills[id]
        override fun save(skill: ManaSkill) { skills[skill.id] = skill }
    }

    private fun candidate(version: Int = 1) = ManaSkill(
        id = "summarize-source",
        version = version,
        name = "Source summarizer",
        description = "Extract claims with provenance",
        procedure = "Read source, list claims, attach source references"
    )

    @Test
    fun onlyIndependentlyVerifiedSkillsAreAccepted() = runBlocking {
        val store = InMemorySkillStore()
        val evolution = ManaSkillEvolution(
            SkillEvaluator { SkillEvaluation(true, true, listOf("fixture:test-1"), "Passed") },
            store
        )
        val result = evolution.evaluate(candidate())
        assertEquals(SkillReviewStatus.ACCEPTED, result.status)
        assertEquals(result, store.latest(result.id))
    }

    @Test
    fun unverifiedSkillRemainsRejected() = runBlocking {
        val evolution = ManaSkillEvolution(
            SkillEvaluator { SkillEvaluation(true, false, listOf("self-report"), "No independent check") },
            InMemorySkillStore()
        )
        assertEquals(SkillReviewStatus.REJECTED, evolution.evaluate(candidate()).status)
    }

    @Test
    fun skillVersionMustIncrease() = runBlocking {
        val store = InMemorySkillStore()
        store.save(candidate())
        val evolution = ManaSkillEvolution(
            SkillEvaluator { SkillEvaluation(true, true, listOf("test"), "Passed") },
            store
        )
        assertFailsWith<IllegalArgumentException> { runBlocking { evolution.evaluate(candidate()) } }
    }
}
