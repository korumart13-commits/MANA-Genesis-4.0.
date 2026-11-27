# MANA Genesis 4.0 — GitHub Actions engineering automation

Date: 2026-11-27
Branch: `chatgpt/mana-asi-rmx5566-one-action-setup`

## Implemented

The MANA Brain JVM module now contains `GitHubActionsRestClient`, `GitHubActionsGateway` and `ManaEngineeringAutomation`.

The Android Engineering Lab exposes the same flow through `ManaGitHubActionsConnector`, `ManaBrainEngineeringProvider` and `ManaEngineeringActivity`. The provider adapter calls `ManaBrainBridge.respondDetailed`, so engineering requests use the same local-first Brain routing as other MANA tasks.

### Runtime sequence

1. The owner selects source files and maps each file to its exact repository-relative path.
2. The owner explicitly approves one engineering cycle. The engineering adapter routes through the central MANA Brain: local LiteRT-LM is attempted first; only if local inference is unavailable may an enabled cloud provider be used as fallback. Cloud use is covered by the one-cycle confirmation and may incur charges.
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

The Android app is wired to the existing Brain project module and the shared encrypted credential vault. `ManaBrainEngineeringProvider` routes code generation through the central Brain and tries the local LiteRT-LM runtime before optional cloud fallback.

Verified push CI on commit `cb1625553907124d1f02791a7302efdec5decc06` succeeded: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46005104570. The synthetic patch policy/apply test, complete MANA Brain JVM test suite, JAR build, SHA-256 manifest, GitHub provenance attestation, machine-readable diagnostic artifact and candidate upload all completed successfully. Artifacts: test report `15201365713`, diagnostics `15201191143`, candidate JAR bundle `15200691836`.

This successful push run validates the engine-side implementation and CI harness. The separate Android launcher Gradle build is being rerun for the app-side connector/UI; it must also pass before claiming the APK integration compiles. The workflow_dispatch API itself remains blocked until the workflow is present on the default branch and the installed app is provisioned with the owner's GitHub token. Queued, cancelled or in-progress runs are not passes.

## GitHub dispatch activation blocker

GitHub requires a workflow file configured for `workflow_dispatch` to exist on the repository's **default branch** before the REST dispatch endpoint will run it. The new `mana-engineering-pipeline.yml` currently exists only on this feature branch; the repository default branch is `main`, and the workflow file is not present there. Therefore the Android connector is implemented, but a live app-originated dispatch cannot succeed until PR #22 (or an equivalent owner-approved change) adds this workflow to `main`. This work deliberately does not write to `main` or merge the PR on the owner's behalf. The connector returns a clear error for this condition rather than claiming that a run started.

## Not yet verified / not claimed

- A real `workflow_dispatch` from an installed MANA APK has not been executed because the app has not yet been provisioned with the owner's GitHub token and cloud model credential.
- The Android UI has not yet been verified on the RMX5566RU handset.
- No successful Android APK install or firmware OTA is implied by the JVM engineering pipeline.
- The workflow checks the MANA Brain JVM project; it is not yet a full Android/AOSP ROM build, and the repair loop does not commit accepted changes. A separately approved merge/release stage remains required.
- The user's broader MANA Genesis 4.0 requirements remain in the project backlog; this engineering integration does not replace or mark those features complete.


A second, fuller PR pipeline run passed on the updated engineering loop and HTTP adapter tests: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/46005966915 (PR run conclusion: success). This run includes the loopback HTTP integration tests for dispatch correlation, exact branch source fetch, status polling, ZIP log download and bounded failure evidence. It also passed the synthetic isolated patch/apply test, all MANA Brain JVM tests, JAR build, SHA-256 manifest and diagnostic/candidate artifact uploads. Artifacts: test report `15202041733`, diagnostics `15202086789`, candidate `15202276655`.

One earlier test attempt failed because the fake HTTP server omitted the GET response for the completed failed run; the fixture was corrected to return that run before log retrieval, and the subsequent PR pipeline passed. A later change only lengthens the run-poll timeout to the workflow's 25-minute maximum; CI for the newest branch head is pending.

Android launcher compile verification is still pending/queued for the connector and UI. The workflow_dispatch activation blocker on default branch `main` remains.
