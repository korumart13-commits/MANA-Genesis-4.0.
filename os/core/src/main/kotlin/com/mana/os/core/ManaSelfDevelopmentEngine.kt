package com.mana.os.core

/**
 * Controlled self-development pipeline for MANA.
 *
 * Generated code is a proposal, never an in-place mutation of the running APK/OS.
 * A host implementation must provide an isolated workspace, real tests, artifact
 * signing/verification and a platform-specific installer. This class grants no
 * Android permissions, root access, bootloader access or partition-write access.
 */
enum class SelfDevelopmentPhase {
    REQUESTED, ANALYZING, PATCH_GENERATED, VALIDATING, VALIDATED,
    STAGED, AWAITING_OWNER_APPROVAL, INSTALLED, REBOOT_PENDING,
    REJECTED, FAILED
}

enum class ChangeScope { APP_MODULE, USERSPACE_SYSTEM_MODULE, PROTECTED_PLATFORM }

data class SelfDevelopmentRequest(
    val id: String,
    val goal: String,
    val targetDevice: String,
    val targetBuild: String,
    val scope: ChangeScope
) {
    init {
        require(id.matches(Regex("[A-Za-z0-9._-]{1,80}")))
        require(goal.isNotBlank() && goal.length <= 8_000)
        require(targetDevice.isNotBlank() && targetBuild.isNotBlank())
    }
}

data class GeneratedChangeSet(
    val patchId: String,
    val changedPaths: Set<String>,
    val diff: String
) {
    init {
        require(patchId.isNotBlank() && diff.isNotBlank())
        require(changedPaths.isNotEmpty())
    }
}

data class SelfDevelopmentValidation(
    val testsPassed: Boolean,
    val staticChecksPassed: Boolean,
    val compatibilityPassed: Boolean,
    val securityChecksPassed: Boolean,
    val report: String
) {
    val accepted: Boolean get() =
        testsPassed && staticChecksPassed && compatibilityPassed && securityChecksPassed
}

data class StagedSelfDevelopmentArtifact(
    val artifactId: String,
    val version: String,
    val sha256: String,
    val signatureVerified: Boolean,
    val rollbackAvailable: Boolean
) {
    init {
        require(artifactId.isNotBlank() && version.isNotBlank())
        require(sha256.matches(Regex("[a-f0-9]{64}")))
    }
}

data class SelfDevelopmentResult(
    val phase: SelfDevelopmentPhase,
    val detail: String,
    val artifact: StagedSelfDevelopmentArtifact? = null
)

/** Platform adapter. Implementations must not execute generated code in the live process. */
interface ManaSelfDevelopmentHost {
    fun analyze(request: SelfDevelopmentRequest): String
    fun generatePatch(request: SelfDevelopmentRequest, analysis: String): GeneratedChangeSet
    fun validateInIsolation(request: SelfDevelopmentRequest, changes: GeneratedChangeSet): SelfDevelopmentValidation
    fun stageSignedArtifact(request: SelfDevelopmentRequest, changes: GeneratedChangeSet): StagedSelfDevelopmentArtifact
    fun installStagedArtifact(artifact: StagedSelfDevelopmentArtifact): Boolean
    fun requestOwnerApproval(request: SelfDevelopmentRequest, artifact: StagedSelfDevelopmentArtifact): Boolean
    fun scheduleRebootAfterApproval(): Boolean
}

/**
 * Orchestrates self-editing as analyze -> generate diff -> test -> sign/stage ->
 * owner approval -> install -> reboot request. It never silently self-installs
 * protected platform changes or reboots the phone.
 */
class ManaSelfDevelopmentEngine(private val host: ManaSelfDevelopmentHost) {
    fun develop(request: SelfDevelopmentRequest): SelfDevelopmentResult {
        return try {
            val analysis = host.analyze(request)
            require(analysis.isNotBlank()) { "Self-analysis returned no actionable findings." }

            val changes = host.generatePatch(request, analysis)
            validateChangedPaths(changes.changedPaths)

            val validation = host.validateInIsolation(request, changes)
            if (!validation.accepted) {
                return SelfDevelopmentResult(
                    SelfDevelopmentPhase.REJECTED,
                    "Generated patch failed isolated validation: ${validation.report}"
                )
            }

            val artifact = host.stageSignedArtifact(request, changes)
            require(artifact.signatureVerified) { "Artifact signature verification failed." }
            require(artifact.rollbackAvailable) { "A tested rollback path is required." }

            if (request.scope == ChangeScope.PROTECTED_PLATFORM) {
                if (!host.requestOwnerApproval(request, artifact)) {
                    return SelfDevelopmentResult(
                        SelfDevelopmentPhase.AWAITING_OWNER_APPROVAL,
                        "Protected platform change is staged; explicit owner approval is required.",
                        artifact
                    )
                }
            }

            if (!host.installStagedArtifact(artifact)) {
                return SelfDevelopmentResult(
                    SelfDevelopmentPhase.FAILED,
                    "Installer rejected the staged artifact; active system was not changed.",
                    artifact
                )
            }

            if (request.scope == ChangeScope.PROTECTED_PLATFORM) {
                if (!host.scheduleRebootAfterApproval()) {
                    return SelfDevelopmentResult(
                        SelfDevelopmentPhase.INSTALLED,
                        "Update installed or queued, but reboot was not scheduled.",
                        artifact
                    )
                }
                SelfDevelopmentResult(
                    SelfDevelopmentPhase.REBOOT_PENDING,
                    "Verified update is installed; reboot is pending the platform's approved update flow.",
                    artifact
                )
            } else {
                SelfDevelopmentResult(
                    SelfDevelopmentPhase.INSTALLED,
                    "Verified user-space update installed. No device reboot was requested.",
                    artifact
                )
            }
        } catch (error: Exception) {
            SelfDevelopmentResult(
                SelfDevelopmentPhase.FAILED,
                error.message ?: error::class.java.simpleName
            )
        }
    }

    private fun validateChangedPaths(paths: Set<String>) {
        require(paths.isNotEmpty()) { "Patch contains no changed paths." }
        paths.forEach { path ->
            require(path.isNotBlank() && !path.startsWith("/") && !path.startsWith("\\")) {
                "Patch path must be relative."
            }
            val segments = path.replace('\\', '/').split('/')
            require(segments.none { it == ".." || it.isBlank() }) {
                "Patch path escapes the isolated source workspace."
            }
            require(!path.startsWith(".git/") && !path.contains("/.git/")) {
                "Patch cannot modify Git metadata."
            }
            require(!path.startsWith("out/") && !path.startsWith("vendor/keys/")) {
                "Patch cannot write build output or signing-key locations."
            }
        }
    }
}
