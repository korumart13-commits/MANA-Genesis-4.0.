# MANA Genesis 4.0 — engineering pipeline implementation

Date: 2026-11-27  
Branch: `chatgpt/mana-asi-rmx5566-one-action-setup`

## Implemented in source

- `EngineeringPatchPolicy` validates proposed standard Git diffs before they can enter the build pipeline.
- The policy limits a patch to 12 files and 80,000 characters, requires standard Git/unified-diff headers and hunks, and rejects path traversal, absolute paths, file deletion, rename/copy/mode changes, symlinks, binary patches, compiled artifacts, build output, signing material, build scripts and workflow changes.
- The copilot additionally rejects any diff path that was not explicitly selected in the owner-approved task context; a model cannot silently expand its patch to another otherwise-allowed file.
- Allowed patch roots are limited to MANA source/test trees and project documentation.
- `ManaEngineeringCopilot.parseProposal` invokes the policy. A malformed or out-of-scope proposal fails closed and remains unapplied.
- `EngineeringPatchPolicyCli` exposes the same validator to automation.
- Gradle task `validateEngineeringPatch` invokes the validator against a supplied patch file.

## CI execution path

Workflow: `.github/workflows/mana-engineering-pipeline.yml`

Every pipeline run first creates a throwaway Git repository, generates a real diff, resets the working tree, and proves that the production Kotlin validator and git apply can validate and apply that patch in the disposable workspace.

On a workflow-dispatch run with a patch input, the disposable GitHub-hosted checkout:
1. Validates the patch with the production Kotlin policy.
2. Runs `git apply --check --whitespace=error-all`.
3. Applies the patch only to that ephemeral checkout.
4. Runs the full MANA Brain JVM test suite, including patch-policy and copilot tests.
5. Builds the MANA Brain JAR.
6. Retains the test report, patch, JAR and SHA-256 manifest as workflow artifacts.
7. Produces a GitHub build-provenance attestation for non-PR runs.

The job uses read-only repository contents permission, disables persisted checkout credentials, and does not receive model/API keys or Android signing secrets. The generated source is tested away from the user's phone and is never executed inside the live MANA app process.

## How to run a proposed patch

1. Open the repository's Actions tab.
2. Select **MANA Engineering Pipeline** and choose **Run workflow**.
3. Paste the complete standard Git diff into the optional `patch` input.
4. Start the workflow and inspect every validation/test step.
5. Download the candidate artifact and review its patch, test report and SHA-256 manifest before proposing or merging source changes.

A patch rejected by the policy or failing tests does not produce a candidate build.

## Exact completion boundary

This is a real patch-validation, application-in-disposable-checkout, test and build path. It is not yet a fully unattended self-repair loop:

- MANA's copilot can propose diffs, but the mobile app is not yet wired to submit workflow-dispatch requests or retrieve artifacts automatically.
- A failed CI report is retained, but MANA does not yet autonomously consume that report, generate a correction and retry without an operator initiating the next run.
- The workflow produces a tested candidate and provenance attestation, not an APK/ROM release. A provenance attestation is not an Android APK signing certificate.
- No release-signing key, production release environment, Android platform signing, OTA installer or RMX5566RU firmware flasher is configured by this change.
- Candidate publication is not permission to install or flash. Protected platform updates still require exact device compatibility evidence, a verified recovery route and explicit owner approval.

## Verification status

CI is triggered by commits touching the Brain module or this workflow. The synthetic patch-application self-test, complete JVM test suite, JAR build, SHA-256 manifest and keyless GitHub provenance attestation have passed for commit a947ab48a4721d72584eae36d09f430217681d97. The selected-source-path restriction also passed the Brain Engine tests. Successful push run: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46003079145. Candidate artifact ID: 15198548977. Test-report artifact ID: 15199630776. The workflow's manual patch-input branch is implemented but has not yet been executed with a user-supplied patch.
