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

## Important limitations
- This bridge currently runs fixed checks/builds and a read-only device audit only. It does not apply AI-generated source patches to the repository.
- A future patch workflow must stage changes in a review area, show a unified diff, verify the target stays inside the selected project, make a backup/branch, and require owner approval before applying.
- Termux must have Java/Gradle/Android SDK components needed for the selected build. A successful GitHub Actions build does not imply the same toolchain exists on the phone.
- The bridge has not yet been tested on the owner's Realme RMX5566RU / Android 16 firmware.
- It runs as the ordinary Termux app user. Android's app sandbox and SELinux remain in force.
