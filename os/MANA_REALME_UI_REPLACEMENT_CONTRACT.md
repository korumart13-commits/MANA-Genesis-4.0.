# MANA Genesis 4.0 — Realme UI replacement contract

Date: 2026-11-26
Target: realme RMX5566RU / RE645F (Qualcomm SM6225 / bengal), Android 16
Release state: source integration contract; NOT a flashable firmware image.

## Product definition

MANA OS is the system product and user-facing operating environment. MANA Brain is its central assistant, decision and orchestration service. The Android 16 framework remains the compatible platform substrate. The goal is to replace the Realme UI experience and OEM user-facing shell with MANA components, not to replace Android with a different kernel or operating system.

The phone must identify itself to the owner as **MANA OS** wherever product branding is under MANA's control. Android version/API compatibility and legal/open-source notices remain truthful.

## Replacement matrix

| Existing Realme-facing layer | MANA target | Required integration |
|---|---|---|
| Home screen / launcher | ManaLauncher | Product HOME role/default configuration; app discovery, widgets, shortcuts, assistant entry point |
| Quick settings / notification shade | ManaSystemUI | Real SystemUI source integration or maintained fork; status bar, shade, notifications, QS tiles, keyguard integration, platform signing and SELinux |
| Settings app / OEM settings pages | ManaSettings | MANA settings front-end plus Android settings providers/panels for hardware functions that remain Android-owned |
| Control center shortcuts | ManaControlCenter | Integrated QS tiles and controls; must not pretend to replace SystemUI until integrated into the platform |
| Assistant / voice entry | ManaAssistant + ManaBrainService | Signature-protected Binder API, local-first inference, explicit capability grants and owner-visible confirmations |
| Boot animation / product branding | MANA boot assets and product properties | Product image integration after exact boot image and AVB chain are verified |
| OEM themes, icons, fonts, wallpapers | MANA visual system | Product overlays and resources; only target verified overlayable resources |
| OEM update front-end | ManaUpdateService | Signed OTA admission, compatibility checks, A/B-aware update flow and rollback validation |

## Android/OEM components deliberately retained

Do not replace these until a working, device-matched implementation exists:

- Android 16 framework, ART, PackageManager, permissions, role manager and core runtime.
- Qualcomm SM6225 kernel, device drivers, firmware and vendor HALs.
- Modem, radio interface, telephony stack, camera HAL and camera pipeline.
- Audio HAL/DSP, Wi-Fi, Bluetooth, GNSS, sensors, biometrics and charging.
- Vendor/odm partitions, VINTF contracts, init dependencies, encryption and Verified Boot chain.

MANA replaces the Realme UI layer and user-facing system applications progressively. It does not claim ownership of undocumented OEM hardware interfaces.

## MANA Brain authority model

MANA Brain is the central assistant and planner, but it is not an unrestricted root shell. It requests typed capabilities through the platform Binder contract. Each capability is checked by caller identity, signature, Android permission/role state and risk policy. Sensitive or destructive actions require an Android-mediated user confirmation. The service must fail closed if the model/provider is unavailable. Cloud providers are opt-in and must never receive secrets or private content by default.

## Integration order

1. Compile and test the existing MANA product apps and Brain service independently.
2. Integrate ManaLauncher as the product HOME implementation while preserving a tested route back to the stock launcher.
3. Integrate ManaSettings and MANA control surfaces without bypassing Android settings/permission ownership.
4. Implement ManaSystemUI against the exact pinned Android 16 SystemUI source; do not ship a mock overlay app as a SystemUI replacement.
5. Add product overlays and MANA boot branding using verified AOSP resources and the exact device product configuration.
6. Integrate the local LiteRT-LM provider into the product image and test Binder-to-inference IPC.
7. Bring up the exact RE645F product/device/vendor/kernel tree, validate VINTF, SELinux, AVB, partitions and hardware.
8. Produce a signed device-specific OTA only after the stock restore path and on-device acceptance suite pass.

## Current implementation facts

- MANA Brain core, Binder contract, Launcher, Assistant, Settings and Control Center source modules exist in the repository.
- Current CI source checks do not constitute a full AOSP product build.
- ManaControlCenter is currently an app/TileService surface, not a replacement for AOSP SystemUI.
- ManaSettings is currently a MANA-branded hub that opens Android-owned settings panels; it is not yet a replacement for every OEM settings screen.
- The RE645F device directory remains a non-buildable evidence placeholder; exact board files, matching kernel/DTBO, vendor blobs/HALs, partition metadata, AVB configuration and tested restore path remain incomplete.
- Therefore no RMX5566RU boot image, system/vendor image or flashable installer is currently releasable.

## Non-negotiable release gate

Never generate or label a guessed ZIP as firmware. A release candidate must be built from pinned AOSP and exact matching device sources, fit verified partition sizes, pass VINTF/SELinux and hardware tests, use the correct AVB/OTA signing chain, and have a proven stock restore procedure. Until then, source archives and APKs must be labelled as such and remain non-flashable.
