package com.mana.brain

/**
 * Fail-closed policy for patches proposed by MANA's engineering copilot.
 *
 * Only source/test files and documentation may be changed by this pipeline.
 * Build logic, CI workflows, signing material, device firmware and repository
 * metadata are deliberately outside the autonomous patch surface.
 */
object EngineeringPatchPolicy {
    const val MAX_DIFF_CHARS = 80_000
    const val MAX_FILES = 12

    private val allowedRoots = listOf(
        "engine/mana-brain/src/main/",
        "engine/mana-brain/src/test/",
        "os/core/src/main/",
        "os/core/src/test/",
        "launcher/app/src/main/",
        "launcher/app/src/test/",
        "system/mana/services/",
        "docs/"
    )

    data class Validation(val changedPaths: Set<String>, val additions: Int, val deletions: Int)

    fun validate(diff: String): Validation {
        require(diff.isNotBlank()) { "Patch is empty." }
        require(diff.length <= MAX_DIFF_CHARS) { "Patch exceeds the configured size limit." }
        require(!diff.contains("\u0000")) { "Patch contains a NUL byte." }
        require(!diff.contains("GIT binary patch") && !diff.contains("Binary files ")) {
            "Binary patches are not accepted by the source engineering pipeline."
        }
        require(!diff.lineSequence().any {
            it.startsWith("old mode ") || it.startsWith("new mode ") ||
                it.startsWith("new file mode 120000") || it.startsWith("deleted file mode") ||
                it.startsWith("rename from ") || it.startsWith("rename to ") ||
                it.startsWith("copy from ") || it.startsWith("copy to ")
        }) { "Mode changes, symlinks, renames and copies are not accepted." }

        val fileHeaders = diff.lineSequence().filter { it.startsWith("diff --git ") }.toList()
        require(fileHeaders.isNotEmpty()) { "Patch has no git file headers." }
        require(fileHeaders.size <= MAX_FILES) { "Patch changes too many files." }
        require(diff.lineSequence().any { it.startsWith("@@ ") || it.startsWith("@@-") }) {
            "Patch contains no unified-diff hunks."
        }

        val paths = linkedSetOf<String>()
        fileHeaders.forEach { header ->
            val match = Regex("^diff --git a/(.+) b/(.+)$").matchEntire(header)
                ?: throw IllegalArgumentException("Malformed or quoted git diff header.")
            val oldPath = normalize(match.groupValues[1])
            val newPath = normalize(match.groupValues[2])
            require(oldPath == newPath) { "A patch may not rename files." }
            validatePath(newPath)
            require(paths.add(newPath)) { "Patch repeats a file path." }
        }

        val oldHeaders = diff.lineSequence().filter { it.startsWith("--- ") }.toList()
        val newHeaders = diff.lineSequence().filter { it.startsWith("+++ ") }.toList()
        require(oldHeaders.size == paths.size && newHeaders.size == paths.size) {
            "Every changed file must have exactly one old and one new path header."
        }
        oldHeaders.zip(newHeaders).forEach { (oldLine, newLine) ->
            require(!oldLine.startsWith("--- /dev/null")) {
                "File deletion is not permitted in an autonomous patch."
            }
            val oldPath = normalizeHeaderPath(oldLine.removePrefix("--- "))
            val newPath = normalizeHeaderPath(newLine.removePrefix("+++ "))
            require(oldPath == newPath && oldPath in paths) {
                "Unified-diff file headers do not match the git diff headers."
            }
        }

        var additions = 0
        var deletions = 0
        diff.lineSequence().forEach { line ->
            if (line.startsWith("+") && !line.startsWith("+++ ")) additions++
            if (line.startsWith("-") && !line.startsWith("--- ")) deletions++
        }
        require(additions + deletions > 0) { "Patch contains no source changes." }
        return Validation(paths, additions, deletions)
    }

    private fun normalizeHeaderPath(raw: String): String {
        require(raw != "/dev/null") { "File deletion is not permitted." }
        require(raw.startsWith("a/") || raw.startsWith("b/")) { "Unexpected diff path prefix." }
        return normalize(raw.substring(2))
    }

    private fun normalize(raw: String): String {
        require(raw.isNotBlank() && !raw.startsWith("/") && !raw.startsWith("\\")) {
            "Patch paths must be relative."
        }
        val path = raw.replace('\\', '/')
        val segments = path.split('/')
        require(segments.none { it.isBlank() || it == "." || it == ".." }) {
            "Patch path traversal is not permitted."
        }
        return path
    }

    private fun validatePath(path: String) {
        require(allowedRoots.any { path.startsWith(it) }) {
            "Patch path is outside the approved source and documentation workspaces: $path"
        }
        require(!path.endsWith(".jar") && !path.endsWith(".class") && !path.endsWith(".so") &&
            !path.endsWith(".apk") && !path.endsWith(".aab") && !path.endsWith(".zip") &&
            !path.endsWith(".keystore") && !path.endsWith(".jks")) {
            "Compiled artifacts and signing material are not valid source patch targets."
        }
        require(!path.split('/').any { it in setOf(".git", "build", "out", "vendor", "keys") }) {
            "Patch targets a protected or generated directory."
        }
    }
}
