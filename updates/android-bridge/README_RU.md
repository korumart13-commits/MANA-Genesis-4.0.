# Realme MANA +1 — updater handoff APK

This is a small, standalone Android APK with no declared Android permissions and no Google Play Services dependency.

## What it does

1. When the owner opens it, it attempts to open Android's OEM system-update settings action: `android.settings.SYSTEM_UPDATE_SETTINGS`.
2. If the OEM does not expose that activity, it falls back to the main Android Settings screen.
3. The owner can then inspect and install only updates that Realme itself offers and accepts for this exact device.

## What it does not do

- It is not a Realme-signed OTA and is not a full MANA OS update.
- It cannot inject a MANA payload into Realme's updater, impersonate an OEM signature, unlock a locked bootloader, or write boot/system/vendor partitions.
- It does not make itself a privileged/system app and does not silently grant permissions.
- It does not claim MANA +1 is installed after opening Settings.

## Target

- RMX5566RU / RE645F
- Android API 36
- User-reported base fingerprint is kept unchanged; the OEM updater remains authoritative.

## Build

From this directory:

```sh
gradle --no-daemon :app:assembleDebug
```

Output:

`app/build/outputs/apk/debug/app-debug.apk`

The CI artifact is a debug APK for initial handoff testing. It has not yet been installed or tested on the physical RMX5566RU. The OEM settings intent must be verified on the phone before claiming a working redirect.

## Actual MANA system update

A true MANA system OTA still requires an exact device-matched Android 16 target-files build and a trust/signing route accepted by the device. The current locked stock Realme updater accepts OEM-trusted firmware only. The handoff APK cannot turn an independently signed MANA archive into a trusted OEM update.
