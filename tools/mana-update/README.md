# MANA Update — Android bootstrapper prototype

This is a user-launched, transparent Android bootstrapper prototype. It is designed to minimize interaction while respecting Android and OEM security boundaries.

## Current behavior
- Reads manufacturer, brand, model, Android release/API and build fingerprint locally.
- Shows a single preflight result and checks whether the device matches the configured test target (matching is informational; it does not block unrelated OPPO devices).
- Does not request contacts, SMS, call logs, location, microphone, accessibility, device-admin, VPN, root or usage permissions.
- Does not silently install APKs, change system partitions, run shell commands, alter SELinux, unlock the bootloader or bypass AVB.
- Contains no firmware payload yet; therefore it cannot update the OS or install a system bridge.

## One-confirmation goal
The app performs read-only checks automatically at launch. A single explicit user action can open Android's system installer for a separately supplied, verified APK. Android's own confirmation remains mandatory. A Realme/OPPO OTA must be delivered through an OEM-supported updater and accepted signature; an ordinary APK cannot impersonate that mechanism.

## Build
GitHub Actions workflow `MANA Update Android prototype` builds a debug APK and publishes it as an artifact. Debug APK is for testing only, not a firmware update.

## Next required inputs for a real OTA
Exact OPPO test-device model/build, OEM OTA package format, matching firmware baseline, accepted signing path, partition/update-engine metadata and recovery procedure. Do not reuse RMX5566RU firmware on an unrelated OPPO device.