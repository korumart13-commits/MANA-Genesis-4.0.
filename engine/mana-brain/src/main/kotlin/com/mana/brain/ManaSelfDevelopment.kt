package com.mana.brain

enum class DevelopmentTrack { ENGINE, ANDROID_MODULES, KERNEL_PATCHSET }
enum class DevelopmentDisposition { ACCEPT_FOR_TESTING, ACCEPT_FOR_REVIEW_ONLY, REJECT }

data class ProposedCodePatch(
    val unifiedDiff: String,
    val changedPaths: List<String>,
    val containsBinaryChanges: Boolean = false,
    val deletesFiles: Boolean = false
) {
    init { require(unifiedDiff.isNotBlank()); require(changedPaths.isNotEmpty()) }
}

data class DevelopmentAdmission(val disposition: DevelopmentDisposition, val reasons: List<String>)

class ManaSelfDevelopmentPolicy(private val exactKernelSourceVerified: Boolean = false) {
    fun evaluate(track: DevelopmentTrack, patch: ProposedCodePatch): DevelopmentAdmission {
        if (patch.containsBinaryChanges || patch.deletesFiles) {
            return DevelopmentAdmission(DevelopmentDisposition.REJECT,
                listOf("Binary changes and file deletions are not accepted in autonomous candidates"))
        }
        val paths = patch.changedPaths
        if (paths.any { it.startsWith("/") || "\\" in it || it.split('/').any { part -> part == ".." } }) {
            return DevelopmentAdmission(DevelopmentDisposition.REJECT, listOf("Unsafe or traversing path"))
        }
        val allowed = when (track) {
            DevelopmentTrack.ENGINE -> paths.all {
                (it.startsWith("engine/mana-brain/src/main/kotlin/") ||
                    it.startsWith("engine/mana-brain/src/test/kotlin/")) && it.endsWith(".kt")
            }
            DevelopmentTrack.ANDROID_MODULES -> paths.all {
                (it.startsWith("launcher/app/src/") || it.startsWith("system/mana/apps/") ||
                    it.startsWith("os/core/src/") || it.startsWith("game/")) &&
                    it.substringAfterLast('.').lowercase() in setOf("kt", "java", "xml", "json", "gltf", "js", "md")
            }
            DevelopmentTrack.KERNEL_PATCHSET -> paths.all {
                it.startsWith("kernel/mana-patches/") && (it.endsWith(".patch") || it.endsWith(".md"))
            }
        }
        if (!allowed) return DevelopmentAdmission(DevelopmentDisposition.REJECT,
            listOf("One or more changed paths are outside the $track allowlist"))
        return if (track == DevelopmentTrack.KERNEL_PATCHSET) {
            DevelopmentAdmission(DevelopmentDisposition.ACCEPT_FOR_REVIEW_ONLY,
                listOf(if (exactKernelSourceVerified) "Kernel patchset admitted for expert review"
                    else "Exact RMX5566RU kernel source is unverified; patchset remains review-only"))
        } else DevelopmentAdmission(DevelopmentDisposition.ACCEPT_FOR_TESTING,
            listOf("Candidate may enter isolated build and test validation"))
    }
}

data class GeneratedCodeCandidate(
    val track: DevelopmentTrack,
    val explanation: String,
    val patch: ProposedCodePatch,
    val admission: DevelopmentAdmission
)

data class CandidateRunResult(
    val accepted: Boolean,
    val branch: String?,
    val commit: String?,
    val report: String
)

fun interface ManaCandidateWorkspace {
    suspend fun validateAndPrepare(track: DevelopmentTrack, patch: ProposedCodePatch): CandidateRunResult
}

/** Brain-generated diffs are policy-checked before the isolated workspace sees them. */
class ManaSelfDevelopmentCoordinator(
    private val brain: ManaBrain,
    private val workspace: ManaCandidateWorkspace,
    private val policy: ManaSelfDevelopmentPolicy = ManaSelfDevelopmentPolicy()
) {
    suspend fun develop(
        goal: String,
        track: DevelopmentTrack,
        sourceContext: String,
        allowCloudForCode: Boolean = false
    ): Pair<GeneratedCodeCandidate?, CandidateRunResult?> {
        require(goal.isNotBlank())
        require(sourceContext.length <= 120_000) { "Source context exceeds the review limit" }
        val prompt = buildString {
            appendLine("You are MANA AutoDev. Prepare a minimal, reviewable unified diff.")
            appendLine("Track: $track")
            appendLine("Goal: $goal")
            appendLine("Change only files required for this goal. Do not edit build scripts, workflows,")
            appendLine("secrets, device partitions, boot chain, OEM drivers or permission boundaries.")
            appendLine("Return an explanation followed by one fenced diff block labelled diff.")
            appendLine("If safe completion is impossible from the supplied context, explain why; do not invent files.")
            appendLine("--- SOURCE CONTEXT ---")
            appendLine(sourceContext)
        }
        val response = brain.respond(BrainRequest(
            prompt = prompt,
            allowCloud = allowCloudForCode,
            sensitive = !allowCloudForCode
        ))
        val diff = extractDiff(response.text)
            ?: return null to CandidateRunResult(false, null, null, "Brain did not return a unified diff")
        val paths = Regex("(?m)^diff --git a/.+ b/(.+)$")
            .findAll(diff).map { it.groupValues[1] }.toList()
        if (paths.isEmpty()) return null to CandidateRunResult(false, null, null, "Unified diff contains no file paths")
        val patch = ProposedCodePatch(diff, paths)
        val admission = policy.evaluate(track, patch)
        val explanation = response.text.substringBefore("diff --git").take(4000)
        val candidate = GeneratedCodeCandidate(track, explanation, patch, admission)
        if (admission.disposition == DevelopmentDisposition.REJECT) {
            return candidate to CandidateRunResult(false, null, null, admission.reasons.joinToString("; "))
        }
        return candidate to workspace.validateAndPrepare(track, patch)
    }

    private fun extractDiff(text: String): String? {
        val fence = 96.toChar().toString().repeat(3)
        val fenced = Regex("(?s)" + fence + "diff\\s*(.*?)" + fence).find(text)?.groupValues?.get(1)
        if (!fenced.isNullOrBlank()) return fenced.trim() + "\n"
        val start = text.indexOf("diff --git ")
        return if (start >= 0) text.substring(start).trim() + "\n" else null
    }
}
