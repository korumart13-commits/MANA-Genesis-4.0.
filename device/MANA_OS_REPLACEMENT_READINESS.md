# MANA OS full replacement — device readiness gate

Date: 2026-11-25
Target: RMX5566RU / RE645F, Qualcomm SM6225 (bengal), Android 16, kernel 5.15.197
Status: **NOT FLASHABLE — source and boot-chain gates are not satisfied**

## Verified facts from the owner's runtime audit

- Product: RMX5566RU / RE645F
- SoC: Qualcomm SM6225, board/platform bengal
- ABI: arm64-v8a
- Android: 16 / API 36
- Kernel: 5.15.197-android13-8-o-01074-gdfa4acfa411e
- Bootloader: locked (flash.locked=1)
- Verified Boot: green
- A/B slots and dynamic partitions: enabled
- Active slot reported as _a (observation only; never hard-code)

The hardware properties indicate the Snapdragon 685 / realme C85 4G platform. Do not use RMX5253 / C85 5G / MediaTek firmware merely because the retail name was previously stated as “C85 5G”.

## Implemented in this branch

- MANA HOME has been upgraded from a plain vertical app list to a user-space launcher surface with:
  - MANA identity and dark-navy visual system;
  - localized clock and date;
  - assistant launch action with a real missing-app state;
  - searchable app grid discovered through PackageManager;
  - application launch intents;
  - app list refresh on resume.
- This launcher is an Android HOME app. It does not replace the Android framework, privileged SystemUI, lock screen, Settings provider, kernel or boot chain.
- The change is intentionally kept in a non-privileged Soong package and does not request dangerous permissions.

## What is still missing for a real OS image

1. A complete Android 16 AOSP checkout pinned to an exact manifest commit.
2. An RMX5566RU/RE645F product/device tree and matching board configuration.
3. Exact OEM kernel source and defconfig matching Linux 5.15.197 and this firmware fingerprint.
4. Matching DTB/DTBO, vendor_boot/init_boot layout, vendor modules and proprietary firmware.
5. Vendor blobs and HAL/VINTF manifests matching the installed firmware.
6. Exact partition metadata and image sizes for boot, init_boot, vendor_boot, dtbo, vbmeta, super, product, odm and userdata.
7. Verified AVB chain, signing requirements and rollback protection.
8. A verified bootloader unlock path and a tested stock restore/recovery route.
9. Device-specific integration tests for display, touch, audio, camera, modem, Wi-Fi, Bluetooth, sensors, charging, suspend/resume, biometrics and encryption.
10. A complete reproducible build and signing pipeline, followed by a test installation on a spare device or with a proven recovery method.

## SystemUI and boot branding

The current MANA SystemUI work must not be described as a complete replacement for AOSP SystemUI. A real replacement requires a source-level fork/implementation of SystemUI and privileged integration into the Android product build, including framework resources, overlays, permission allowlists, SELinux policy, signature permissions and regression testing.

A normal APK cannot replace the bootloader splash, kernel boot sequence, Android framework or privileged SystemUI. Do not write to boot/vendor_boot/dtbo/vbmeta/super or create a flashable package until all device-specific prerequisites above are satisfied.

## Release policy

- Keep the current stock OS bootable; MANA HOME may be tested as the default HOME only after the user selects it through Android's role/home chooser.
- Do not unlock, erase, flash, disable AVB or change SELinux on the owner's only phone as an experimental step.
- Do not label a source archive or debug APK as a firmware image.
- The build pipeline may compile source modules and run tests, but it must not automatically flash a phone.
- Mark a feature “implemented” only when its code is present, its automated tests pass, and the device-specific acceptance test has passed.
- No claim of 100% compatibility is allowed before full on-device testing.

## Acceptance criteria for a future flashable MANA OS

A firmware candidate is releasable only when all source and device gates are green, all product images build reproducibly, AVB and OTA verification pass, all hardware acceptance tests pass, rollback to the exact stock build is proven, and the final package is signed with the intended release keys. If any item fails, the package remains non-flashable.
