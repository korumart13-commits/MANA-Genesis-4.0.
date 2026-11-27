# MANA Genesis 4.0 — GitHub Actions engineering automation

Date: 2026-11-27
Branch: `chatgpt/mana-asi-rmx5566-one-action-setup`

## Implemented

The MANA Brain JVM module now contains `GitHubActionsRestClient`, `GitHubActionsGateway` and `ManaEngineeringAutomation`.

The Android Engineering Lab exposes the same flow through `ManaGitHubActionsConnector` and `ManaEngineeringActivity`.

### Runtime sequence

1. The owner selects source files and maps each file to its exact repository-relative path.
2. The owner explicitly approves sending the selected source and task to the configured cloud engineering provider. The local-first conversational Brain routing is unchanged; code generation through the current engineering copilot requires an enabled cloud provider.
3. The owner connects GitHub using a repository-scoped fine-grained token. The app stores it with the existing AES-GCM Android Keystore credential vault; it is not embedded in source, passed to workflow inputs, or written to logs.
4. MANA reads the target branch SHA and checks it again before each attempt. A stale branch aborts before model generation or dispatch.
5. The copilot generates a complete unified diff. The production patch policy checks paths, file count, size, hunks and disallowed changes. The workflow input is capped at 60 KB.
6. MANA calls GitHub's `workflow_dispatch` endpoint with a unique `client_run_id` and the patch. It correlates the resulting run by branch, exact commit SHA, event, unique display title and creation time.
7. MANA polls the run until completion. On failure it reads failed job metadata, downloads GitHub's ZIP log redirect without forwarding the PAT to the signed blob URL, bounds the log size, and redacts common tokens, API keys and private-key blocks.
8. The redacted failure evidence is added to the next copilot objective. MANA can generate a replacement diff and repeat CI up to two times (three total attempts).
9. MANA reports every run URL, result, summary and bounded log excerpt.

## Safety boundaries

- Workflow dispatch requires an explicit owner action in the Engineering Lab.
- The selected source context is sent to the configured cloud provider only after a one-cycle confirmation. Provider usage may incur charges.
- GitHub token is stored encrypted using Android Keystore-backed AES-GCM. The UI provides a delete/disconnect action.
- Recommended fine-grained token permissions for this repository only: **Actions: read/write; Contents: read**. Do not grant administration or organization-wide access.
- The CI workflow applies the submitted patch only in its disposable GitHub-hosted checkout, then runs the full MANA Brain JVM tests and builds a JAR.
- The workflow does not push, commit, merge or publish the patch to the repository.
- This loop does not install APKs, flash partitions, unlock bootloaders or install firmware. Firmware OTA remains gated by the separate RMX5566RU bring-up and recovery requirements.
- Retries are hard-capped at two. A branch-head change aborts the task. Patches remain subject to the same source-only validation policy on every attempt.

## CI workflow contract

Workflow: `.github/workflows/mana-engineering-pipeline.yml`

Required `workflow_dispatch` inputs:
- `client_run_id`: unique correlation identifier
- `patch`: standard unified Git diff (maximum 60 KB)

The run name contains `client_run_id`, allowing the app to avoid accidentally binding to an unrelated push or concurrent dispatch. The workflow uploads:
- test report
- tested engine candidate and SHA-256 manifest
- machine-readable CI diagnostics, including task ID, commit, test/JAR outcomes, patch hash and explicit `review_only` / `firmware_install_performed:false` fields

## Verification status

The JVM automation tests cover:
- failure log returned to the next model prompt and a later successful CI attempt
- hard maximum of three total attempts
- no dispatch without owner approval
- stale branch rejection before cloud generation
- unique run identifiers

The Android app is wired to the existing Brain project module and the encrypted credential vault.

The branch push and PR workflows have been triggered for the latest changes. Their authoritative result must be read from the GitHub Actions run at the current branch-head SHA; queued, cancelled or in-progress runs are not passes.

## Not yet verified / not claimed

- A real `workflow_dispatch` from an installed MANA APK has not been executed because the app has not yet been provisioned with the owner's GitHub token and cloud model credential.
- The Android UI has not yet been verified on the RMX5566RU handset.
- No successful Android APK install or firmware OTA is implied by the JVM engineering pipeline.
- The workflow checks the MANA Brain JVM project; it is not yet a full Android/AOSP ROM build, and the repair loop does not commit accepted changes. A separately approved merge/release stage remains required.
- The user's broader MANA Genesis 4.0 requirements remain in the project backlog; this engineering integration does not replace or mark those features complete.
