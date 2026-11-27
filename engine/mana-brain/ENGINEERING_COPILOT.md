# MANA Brain engineering copilot and CI loop

The Brain engineering path now includes a bounded GitHub Actions REST client and an Android Engineering Lab integration.

## Proposal and policy

- Uses the central MANA Brain provider router: local LiteRT-LM first; enabled cloud providers are fallback only when the owner approves the engineering cycle.
- Requires owner approval to share source context and explicit approval before running the engineering cycle.
- Accepts at most 12 owner-selected files, fetched from the exact GitHub branch snapshot before CI.
- Redacts common API-key, token, password and private-key patterns before prompt construction.
- Requests a structured unified diff, summary, test plan and risk notes.
- Validates diff paths against approved MANA source workspaces and rejects path traversal, kernel/device trees, signing keys, build output and workflow files.
- Returns every proposal as `REVIEW_REQUIRED`; it does not apply patches in the live app process.

## GitHub Actions integration

- `GitHubActionsRestClient` reads branch HEAD and selected repository files, dispatches `workflow_dispatch`, correlates a unique run, polls to completion and fetches failed job logs.
- GitHub job logs are downloaded as a ZIP; the PAT is not forwarded to the signed blob URL. Extracted logs are bounded and redacted before the model sees them.
- `ManaEngineeringAutomation` performs a maximum of three attempts: initial proposal plus at most two repair proposals. Each repair is a complete replacement diff against the original selected source snapshot.
- Before every attempt, the branch HEAD must still match the source snapshot. A stale base aborts the cycle.
- The workflow applies the diff only in an ephemeral CI checkout, runs tests and builds a candidate JAR. No commit, merge, app install, firmware flash or reboot occurs.

## Android host and credentials

- `ManaEngineeringActivity` provides token setup/removal, path mapping, exact source refresh, and explicit confirmation for CI and cloud-assisted repair.
- `ManaGitHubActionsConnector` uses `ManaEncryptedCredentialStore`: AES-GCM ciphertext is stored in app-private files and the key is held by Android Keystore.
- Required fine-grained token permissions are limited to this repository: Actions read/write and Contents read. Contents write, administration and workflow secrets are not required.
- The workflow file must exist on the repository default branch for GitHub's workflow-dispatch API. It is currently on the development branch only; live dispatch remains gated until the workflow is merged to the default branch.

## Verification boundary

Unit tests cover bounded retries, stale-HEAD rejection, owner approval, REST dispatch, run correlation, source retrieval and ZIP-log parsing. Push/PR CI verifies the code and workflow. No live authenticated dispatch from the phone app has been claimed because this session has no owner GitHub token. The pipeline does not yet publish a signed APK/ROM, autonomously merge source changes, or install firmware.
