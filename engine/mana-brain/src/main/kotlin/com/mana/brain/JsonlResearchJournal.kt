package com.mana.brain

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Append-only local research history. The host chooses the private app-storage path.
 * Each line is one independently parseable JSON record, so a truncated final line
 * does not invalidate earlier observations.
 */
class JsonlResearchJournal(
    private val file: Path,
    private val gson: Gson = Gson()
) : ResearchJournal {
    private val lock = Any()
    private val entryType = object : TypeToken<ResearchJournalEntry>() {}.type

    override fun append(entry: ResearchJournalEntry) {
        synchronized(lock) {
            file.toAbsolutePath().parent?.let { Files.createDirectories(it) }
            val line = gson.toJson(entry) + "\n"
            Files.write(
                file,
                line.toByteArray(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND
            )
        }
    }

    fun readAll(): List<ResearchJournalEntry> = synchronized(lock) {
        if (!Files.exists(file)) return@synchronized emptyList()
        Files.readAllLines(file, StandardCharsets.UTF_8)
            .asSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                runCatching { gson.fromJson<ResearchJournalEntry>(line, entryType) }.getOrNull()
            }
            .toList()
    }

    override fun completedExperiments(goalId: String): Int =
        readAll().count { it.goalId == goalId && it.status == ResearchStatus.COMPLETED }
}
