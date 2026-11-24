# MANA Genesis 4.0 — GitHub Actions build

## Files to upload to the repository root
- `MANA_Genesis_4_0_MIRROR_READY_SOURCE.zip`
- `.github/workflows/android-build.yml`

## Run
1. Open the repository's **Actions** tab.
2. Select **MANA Genesis 4.0 — Android 16 build**.
3. Choose **Run workflow** (or let a push of either listed file trigger it).
4. Wait for a green successful run.
5. Download **MANA-Genesis-4.0-debug-APK** from that run's Artifacts section.

The workflow extracts the archive and builds the Android project at
`MANA_Genesis_4_0_COMPLETE/MANA_Genesis_4.0_Unified/existing_aosp/MANA_OS_3_0_Android_Project`.
It runs `clean`, `test`, and `:app:assembleDebug`, and publishes the APK only after all steps succeed.

**Important:** no APK is included or claimed to be built by this package. The APK is ready only after GitHub Actions reports success and the artifact is present. This is an Android app/launcher project, not a flashable firmware image.

If GitHub writes fail through the connected app, reconnect the GitHub integration and grant repository **Contents: Read and write** and **Actions: Read and write** permissions, then retry.
