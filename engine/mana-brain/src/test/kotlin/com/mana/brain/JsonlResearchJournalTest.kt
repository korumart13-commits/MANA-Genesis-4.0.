package com.mana.brain

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonlResearchJournalTest {
    @Test
    fun journalPersistsAndReloadsEntries() {
        val directory = Files.createTempDirectory("mana-journal-test")
        val file = directory.resolve("research.jsonl")
        val journal = JsonlResearchJournal(file)
        val entry = ResearchJournalEntry(
            goalId = "goal-1",
            experimentId = "experiment-1",
            status = ResearchStatus.COMPLETED,
            observation = "Observed result",
            evidence = listOf("test:passed"),
            reflection = "Continue with next hypothesis"
        )

        journal.append(entry)

        assertEquals(listOf(entry), JsonlResearchJournal(file).readAll())
        assertEquals(1, JsonlResearchJournal(file).completedExperiments("goal-1"))
    }

    @Test
    fun truncatedLastLineDoesNotHideEarlierEntries() {
        val directory = Files.createTempDirectory("mana-journal-truncated")
        val file = directory.resolve("research.jsonl")
        val journal = JsonlResearchJournal(file)
        val entry = ResearchJournalEntry("g", "e", ResearchStatus.COMPLETED, "ok", listOf("proof"), "next")
        journal.append(entry)
        Files.writeString(file, Files.readString(file) + "{truncated")

        assertEquals(listOf(entry), journal.readAll())
    }
}
