# MANA Code Agent — Termux bridge

Status: prototype implementation; device validation pending.

## Purpose
The Android companion can hand off a small, fixed set of owner-approved project operations to the Termux app on the same phone. This is the first execution bridge for a future MANA code-generation workflow.

## Current allowlist
- Show Git working-tree status and branch.
- Run `git diff --check`.
- Run the same JVM test suites used by CI: `gradle -p os/core --no-daemon clean test` and `gradle -p engine/mana-brain --no-daemon clean test`.
- Build and unit-test the debug Android companion APK with `gradle -p launcher --no-daemon :app:assembleDebug :app:testDebugUnitTest` (requires Android SDK).
- Collect a read-only device identity report with `bash ./device/collect_rmx5566_identity.sh`; this records product/build/kernel/project-ID candidates, Verified Boot state, memory and storage to `$HOME/mana_rmx5566_audit/oem_project_identity.txt`.

Every action is shown with its exact command and working directory, then requires a separate owner confirmation. The bridge does not accept model-generated arbitrary shell text, does not invoke `su`/root, does not write to Android system partitions, and does not install APKs or firmware.

## Termux setup
1. Install Termux from its official distribution channel and open it once.
2. Create a project checkout under Termux HOME, e.g. `$HOME/MANA-Genesis-4.0`.
3. In Termux, the owner may enable the documented external-command gate by adding `allow-external-apps=true` to `~/.termux/termux.properties`, then running `termux-reload-settings`.
4. In Android App Info for MANA, grant the additional **Run commands in Termux environment** permission.
5. The owner must also accept Termux's own external-app setting. Both gates are required by Termux.
6. Configure the project path in MANA Code Agent. The checked-in repository does not provide a root `./gradlew`; install Gradle and Java 17 in Termux. Android APK builds additionally require a correctly configured Android SDK.

Official Termux documentation: https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent

## AutoDev candidate workflow

The engine now has a Brain-driven code-candidate coordinator and a separate host runner:
- Engine: `python tools/mana_autodev.py --repo . --patch "$HOME/.mana/autodev/incoming-engine.patch" --track engine`
- Android apps/modules: use `--track apps`; the runner builds the debug APK and runs unit tests.
- Kernel patchset: use `--track kernel`; this is review-only and never compiles or flashes a kernel.

The runner requires a clean Git checkout, creates a detached temporary worktree, applies a unified diff, enforces path allowlists, runs fixed tests/builds, and creates a local candidate branch only after success. It does not push, merge, sign releases, install APKs, create OTAs or flash the device.

The current activity can validate a staged patch file but does not yet transfer generated patch bytes from the local LiteRT-LM companion into Termux automatically. That handoff and device validation are still required before the full self-development loop can be called autonomous end-to-end.

Kernel candidates may add new MANA driver source files only through reviewable patchsets under `kernel/mana-patches/`, targeting new files below `drivers/misc/mana/`. Exact matching RMX5566RU OEM source is still missing, so applying or compiling such patches remains blocked. Existing kernel files, boot chain, DTS, vendor modules and partitions are not writable by this track.

## Important limitations
- The bridge runs fixed checks/builds, a read-only device audit and a gated candidate runner. The Brain-to-Termux patch transfer remains a separate integration task.
- A future patch workflow must stage changes in a review area, show a unified diff, verify the target stays inside the selected project, make a backup/branch, and require owner approval before applying.
- Termux must have Java/Gradle/Android SDK components needed for the selected build. A successful GitHub Actions build does not imply the same toolchain exists on the phone.
- The bridge has not yet been tested on the owner's Realme RMX5566RU / Android 16 firmware.
- It runs as the ordinary Termux app user. Android's app sandbox and SELinux remain in force.
