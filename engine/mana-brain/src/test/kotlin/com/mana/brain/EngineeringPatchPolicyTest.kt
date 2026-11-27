package com.mana.brain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EngineeringPatchPolicyTest {
    private val sourcePath = "engine/mana-brain/src/main/kotlin/com/mana/brain/Example.kt"

    private fun diff(path: String = sourcePath) = """
        diff --git a/$path b/$path
        --- a/$path
        +++ b/$path
        @@ -1,1 +1,2 @@
         class Example
        +// approved source change
    """.trimIndent()

    @Test fun acceptsBoundedSourcePatchAndReturnsDigestiblePathSet() {
        val result = EngineeringPatchPolicy.validate(diff())
        assertEquals(setOf(sourcePath), result.changedPaths)
        assertEquals(1, result.additions)
        assertEquals(0, result.deletions)
    }

    @Test fun acceptsDocumentationPatch() {
        val path = "docs/MANA_ENGINEERING.md"
        assertEquals(setOf(path), EngineeringPatchPolicy.validate(diff(path)).changedPaths)
    }

    @Test fun rejectsGradleAndWorkflowChanges() {
        for (path in listOf("engine/mana-brain/build.gradle.kts", ".github/workflows/release.yml")) {
            assertFailsWith<IllegalArgumentException> { EngineeringPatchPolicy.validate(diff(path)) }
        }
    }

    @Test fun rejectsTraversalAndAbsolutePaths() {
        for (path in listOf("../../etc/passwd", "/tmp/payload.kt", "engine/mana-brain/src/main/../secrets.kt")) {
            assertFailsWith<IllegalArgumentException> { EngineeringPatchPolicy.validate(diff(path)) }
        }
    }

    @Test fun rejectsFileDeletionAndRename() {
        val deletion = diff().replace("--- a/$sourcePath", "--- /dev/null")
        assertFailsWith<IllegalArgumentException> { EngineeringPatchPolicy.validate(deletion) }

        val renamed = diff().replace("b/$sourcePath", "b/engine/mana-brain/src/main/kotlin/com/mana/brain/Renamed.kt")
        assertFailsWith<IllegalArgumentException> { EngineeringPatchPolicy.validate(renamed) }
    }

    @Test fun rejectsBinarySymlinkAndOversizedPatches() {
        assertFailsWith<IllegalArgumentException> {
            EngineeringPatchPolicy.validate(diff() + "\\nGIT binary patch")
        }
        assertFailsWith<IllegalArgumentException> {
            EngineeringPatchPolicy.validate(diff() + "\\nnew file mode 120000")
        }
        assertFailsWith<IllegalArgumentException> {
            EngineeringPatchPolicy.validate(diff().repeat(EngineeringPatchPolicy.MAX_DIFF_CHARS))
        }
    }

    @Test fun rejectsMissingHunksAndMismatchedHeaders() {
        assertFailsWith<IllegalArgumentException> {
            EngineeringPatchPolicy.validate(diff().substringBefore("@@"))
        }
        val mismatch = diff().replace("+++ b/$sourcePath", "+++ b/docs/other.md")
        assertFailsWith<IllegalArgumentException> { EngineeringPatchPolicy.validate(mismatch) }
    }
}
