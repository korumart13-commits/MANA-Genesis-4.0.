package com.mana.brain

import com.google.gson.Gson
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Local persistent store for reviewed skill descriptions/procedures.
 * Skill ids are restricted to safe filename characters; stored procedures are
 * data and are never executed by this class.
 */
class JsonFileManaSkillStore(
    private val directory: Path,
    private val gson: Gson = Gson()
) : ManaSkillStore {
    override fun latest(id: String): ManaSkill? {
        val file = fileFor(id)
        if (!Files.exists(file)) return null
        return runCatching {
            gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), ManaSkill::class.java)
        }.getOrNull()
    }

    override fun save(skill: ManaSkill) {
        val destination = fileFor(skill.id)
        Files.createDirectories(directory)
        val temporary = Files.createTempFile(directory, "${skill.id}-", ".tmp")
        try {
            Files.writeString(
                temporary,
                gson.toJson(skill),
                StandardCharsets.UTF_8,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            )
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    fun all(): List<ManaSkill> {
        if (!Files.isDirectory(directory)) return emptyList()
        return Files.list(directory).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .map { path ->
                    runCatching {
                        gson.fromJson(Files.readString(path, StandardCharsets.UTF_8), ManaSkill::class.java)
                    }.getOrNull()
                }
                .filter { it != null }
                .map { it!! }
                .toList()
        }
    }

    private fun fileFor(id: String): Path {
        require(id.matches(Regex("[A-Za-z0-9._-]{1,100}"))) { "Skill id contains unsupported characters" }
        return directory.resolve("$id.json")
    }
}
