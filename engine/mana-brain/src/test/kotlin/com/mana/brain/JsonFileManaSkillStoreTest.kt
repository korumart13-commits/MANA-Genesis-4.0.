package com.mana.brain

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonFileManaSkillStoreTest {
    @Test
    fun skillSurvivesStoreRecreation() {
        val directory = Files.createTempDirectory("mana-skills")
        val skill = ManaSkill(
            id = "memory-index",
            version = 2,
            name = "Memory index",
            description = "Index durable memories",
            procedure = "Extract concepts and link evidence",
            basedOnEvidence = listOf("test:passed"),
            status = SkillReviewStatus.ACCEPTED
        )
        JsonFileManaSkillStore(directory).save(skill)

        assertEquals(skill, JsonFileManaSkillStore(directory).latest(skill.id))
        assertEquals(listOf(skill), JsonFileManaSkillStore(directory).all())
    }

    @Test
    fun skillIdCannotEscapeStorageDirectory() {
        val store = JsonFileManaSkillStore(Files.createTempDirectory("mana-skills-path"))
        assertFailsWith<IllegalArgumentException> { store.latest("../outside") }
    }
}
