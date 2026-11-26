# MANA Installer — Android APK component installer

## What works
- Reads device manufacturer, model, Android API/release and build locally at launch.
- Lets the user select an APK through Android's Storage Access Framework.
- Calculates SHA-256 in a background worker and compares it with the expected 64-character hash supplied by the user.
- Refuses to launch installation if the hash does not match.
- If Android's per-source install permission is not enabled, opens the relevant Android settings page.
- Hands the selected APK to Android's package installer with a read-only content URI grant. Android controls the final install/replace confirmation.

## Important trust note
A matching hash proves only that the file matches the hash value entered. Obtain the APK and its SHA-256 from the same trusted publisher over an authenticated channel. Do not trust a hash sent by an unknown person or hosted beside an untrusted APK.

## What it does not do
- It does not flash firmware, write system/vendor/boot partitions, unlock bootloader, gain root, bypass AVB/SELinux, or silently install apps.
- It does not contain the MANA Genesis AI runtime, a Device Bridge system service, or an OPPO/realme OTA payload.
- It cannot guarantee that an arbitrary APK is compatible with the phone. Android performs package signature and compatibility checks.

## Build
GitHub Actions workflow `.github/workflows/mana-update-android.yml` builds a debug APK artifact named `MANA-Update-Android-debug`.
