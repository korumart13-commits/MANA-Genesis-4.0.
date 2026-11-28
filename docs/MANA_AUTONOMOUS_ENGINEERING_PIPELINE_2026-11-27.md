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

The Android engineering workspace now connects to the GitHub REST API through `ManaGitHubActionsConnector` and `GitHubActionsRestClient`:

1. The owner saves a fine-grained GitHub token. The app stores it encrypted with AES-GCM; its key is held by Android Keystore. The token is not sent to the model, patch, workflow inputs, or CI logs.
2. The connector fetches the exact branch HEAD and the selected repository source files through GitHub Contents API. It reads the branch HEAD again after fetching and rejects a moving snapshot.
3. MANA Brain generates a complete replacement unified diff from the selected source snapshot. It uses local LiteRT-LM first; enabled cloud providers are fallback only when the owner approves the engineering cycle.
4. Before each attempt the app checks that the branch HEAD still equals the source snapshot. It then dispatches `workflow_dispatch` with a unique `client_run_id` and the bounded patch.
5. The REST client correlates the run by branch, SHA, dispatch event, unique run name and creation time, then polls the run to completion.
6. On failure it fetches failed job metadata, downloads GitHub's ZIP log archive without forwarding the PAT to the signed blob URL, extracts bounded text and redacts common credential/private-key patterns.
7. The failure evidence is added to the next MANA Brain engineering prompt. MANA generates a complete replacement diff against the original selected files and repeats. The retry budget is hard-capped at two repairs (three CI attempts total).
8. The CI workflow validates the diff, applies it only in the ephemeral GitHub-hosted checkout, runs the MANA Brain JVM tests and builds the JAR. Test reports, diagnostics, candidate and SHA-256 manifest are retained as artifacts.

The Android UI exposes token setup/removal, repository path mapping, exact source refresh, and a separate confirmation before running the engineering cycle. The token requires a fine-grained permission set limited to this repository: **Actions: read/write** and **Contents: read**. No repository write/merge permission is requested.

Every pipeline run also creates a throwaway Git repository, generates a real diff, resets the working tree, and proves that the production Kotlin validator and `git apply` can validate and apply that patch in the disposable workspace.

The workflow job uses read-only repository contents permission, disables persisted checkout credentials, and does not receive model/API keys or Android signing secrets. Generated source is tested away from the user's phone and is never executed inside the live MANA app process.

## CI test coverage

- `ManaEngineeringAutomationTest`: verifies local-first provider invocation, CI dispatch sequencing, failure-log handoff to the next proposal, stale-HEAD rejection, owner approval and the hard retry cap.
- `GitHubActionsRestClientTest`: runs a local HTTP fixture to exercise branch SHA retrieval, repository source download, workflow dispatch payload, run correlation, completion polling and bounded ZIP-log extraction.
- The production workflow applies a real synthetic diff in an isolated checkout and runs the full Gradle JVM test suite.

## Running a proposed patch

1. In MANA Engineering Lab, connect a fine-grained GitHub token scoped to this repository.
2. Enter one or more repository-relative source paths (one per line) and use **Загрузить точные исходники из GitHub**. Selecting local files and mapping their paths is optional. MANA fetches the exact branch contents and records the base SHA.
3. Describe the engineering task and confirm the model request.
4. Confirm **Запустить CI и автоисправление**. MANA can make at most two repair retries. It does not commit, merge, install or flash the candidate.
5. Review each run URL, result, summary and failure excerpt. A successful CI candidate still requires separate review and release authorization.

The GitHub workflow-dispatch API requires the workflow file to exist on the repository's default branch. At this checkpoint, `mana-engineering-pipeline.yml` exists on the development branch but not on `main`; therefore the app's live dispatch will return a clear HTTP 404 explanation until PR #22 (or an equivalent reviewed change) places that workflow on the default branch. Push/PR CI for this branch remains available now. No token was available in this ChatGPT session, so no live authenticated dispatch from the phone app has been claimed.

## Exact completion boundary

This is a real patch-validation, application-in-disposable-checkout, test and build path. It is not yet a fully unattended self-repair loop:

- MANA's copilot can propose diffs, but the mobile app is not yet wired to submit workflow-dispatch requests or retrieve artifacts automatically.
- A failed CI report is retained, but MANA does not yet autonomously consume that report, generate a correction and retry without an operator initiating the next run.
- The workflow produces a tested candidate and provenance attestation, not an APK/ROM release. A provenance attestation is not an Android APK signing certificate.
- No release-signing key, production release environment, Android platform signing, OTA installer or RMX5566RU firmware flasher is configured by this change.
- Candidate publication is not permission to install or flash. Protected platform updates still require exact device compatibility evidence, a verified recovery route and explicit owner approval.

## Verification status

CI is triggered by commits touching the Brain module or this workflow. The synthetic patch-application self-test, complete JVM test suite, JAR build, SHA-256 manifest and keyless GitHub provenance attestation have passed for commit a947ab48a4721d72584eae36d09f430217681d97. The selected-source-path restriction also passed the Brain Engine tests. Successful push run: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46003079145. Candidate artifact ID: 15198548977. Test-report artifact ID: 15199630776. The workflow's manual patch-input branch is implemented but has not yet been executed with a user-supplied patch.
