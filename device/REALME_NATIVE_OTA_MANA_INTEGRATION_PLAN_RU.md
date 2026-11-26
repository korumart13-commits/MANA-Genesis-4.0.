# MANA Genesis 4.0 — Realme-native update integration plan (RU)

Date: 2026-11-26
Target: realme RMX5566RU / RMX5566, device codename RE645F, Qualcomm SM6225pro / Snapdragon 685, board bengal, Android 16 / API 36.

## Product decision

MANA Genesis 4.0 is an integrated system-update project for the existing Realme Android 16 installation. It is not a launcher-only deliverable and it is not a renamed stock OTA. The intended result is a coherent MANA system layer: MANA Brain service + local inference provider + memory and lifecycle services + system-facing UI/integration, while retaining the exact Realme hardware support stack (vendor/odm, radio, camera, sensors, audio, display, modem, boot chain and device-specific configuration).

The user-facing release label may be `MANA +1` (or `MANA Genesis 4.0 +1`). This is a MANA release identifier only. It must not be represented as an authentic Realme/OPlus release or published through Realme's update channel.

## Known device baseline (from user-supplied device audit)

- Product/model family: RMX5566RU / RMX5566; codename RE645F.
- Android 16 / API 36.
- Build fingerprint: `realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys`.
- Build: `RMX5566_16.0.5.1310(EX01)`.
- Kernel reported on device: `5.15.197-android13-8-o-01074-gdfa4acfa411e`.
- OTA profile reported: `RMX5566_11.F.03_2030_202610170441`.
- NV identifier: `00110111`.
- Realme UI / OPlus UI generation reported: V7 / V16.
- Region target: EU (Finland).
- Bootloader locked; AVB green; A/B enabled; dynamic partitions enabled; observed active slot _a.
- Security patch reported: 2026-10-01.

Treat all these as a starting profile, not a substitute for validating the exact currently installed partition metadata and OEM package response. Do not use IMEI, serial number, account credentials or private device identifiers in source control.

## Reusable external project research

### 1. OTA Pulse — preferred Android-side architectural reference

Repository: https://github.com/RemuruSama/OTA-Pulse
License: Apache-2.0 (verified from repository LICENSE).
Architecture file: https://github.com/RemuruSama/OTA-Pulse/blob/main/architecture.md

Inspected architecture describes a Kotlin Android app split into:
- device catalog and region-aware model definitions;
- OTA discovery/query and metadata parsing;
- resumable download engine;
- payload extraction and partition tooling;
- anti-rollback / ARB inspection;
- download history and persistent state;
- background workers and notifications;
- modular UI separated from OTA domain logic.

Adapt only these reusable boundaries and compatible Apache-2.0 code, retaining copyright/license notices and recording imported files. Do not copy the complete OTA Pulse product UI or branding into MANA. Its current realme device catalog does not contain RMX5566/RE645F, so add a new device entry only after independently verifying all required fields. Do not assume its generic device support means this exact model is supported.

Suggested MANA mapping:
- existing MANA Update module: discovery, release metadata, changelog, signed-package verification and install status;
- MANA Update / OTA domain: payload parser and partition inventory;
- MANA Recovery: rollback/restore workflow and recovery documentation;
- MANA Brain: request an update, explain changes, check preconditions; it must not bypass package verification or user consent;
- MANA Diary: record update history and result.

### 2. R0rt1z2/realme-ota — official Realme OTA query research tool

Repository: https://github.com/R0rt1z2/realme-ota
License: GPL-3.0.
README: https://github.com/R0rt1z2/realme-ota

The tool queries BBK/OPPO/Realme OTA services using product model, current OTA version, Realme UI version, NV identifier and region. Its documented region values include EU=3. Its README notes that since Android 11 / Realme UI 2, Realme uses component updates and a full OTA URL may not be returned. Use it as a separately executed research utility or reimplement the required protocol cleanly; do not silently merge GPL-3.0 code into the Apache-licensed Android app. Preserve GPL obligations if any GPL source is distributed.

Candidate query profile, subject to confirming the exact values from the live device:
- product model: RMX5566 (verify whether the service expects RMX5566 or RMX5566RU);
- OTA version: RMX5566_11.F.03_2030_202610170441;
- Realme UI major: 7 (verify);
- NV: 00110111;
- region: EU (3).

The public query interface is not proof that a returned component is a complete, installable full firmware image. Save response metadata and checksums; do not alter or redistribute OEM packages as MANA packages.

### 3. Android platform OTA tooling

Official AOSP references:
- update_engine: https://android.googlesource.com/platform/system/update_engine/
- A/B updates: https://source.android.com/docs/core/ota/ab
- OTA generation: https://android.googlesource.com/platform/build/+/main/tools/releasetools/ota_from_target_files.py

Use the official AOSP target-files -> ota_from_target_files / payload generation pipeline for a MANA-built target image. A/B payloads require correct partition operations, metadata and cryptographic signatures. Never patch the metadata/version label of an OEM payload and call it a MANA OTA.

### 4. Similar firmware discovery utilities

- OTA Multi Tools: https://github.com/Devone127/OTA-Multi-Tools — public README says core application logic is closed-source; do not copy opaque code.
- OPlus firmware downloader: https://github.com/suddenBook/OPlus_firmware_download — useful reference for querying official component OTA sources and parsing returned metadata.
- Payload dumper: https://github.com/manojanasuri16/payload_dumper — useful reference for extracting and hash-verifying payload partitions; review its license before copying code.

## Kernel / vendor compatibility gate

Candidate OEM source family:
https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-kernel-source

This is a family-level lead, not a verified exact RMX5566RU kernel source match. Its inspected `build.config.msm.bengal` contains Bengal/GKI build settings, boot image header v4, init_boot, vendor_dlkm, system_dlkm and DT overlay support. Earlier audit identified a source Makefile kernel version around 5.15.180, while the user's device reports 5.15.197. Do not compile/flash this tree as the device kernel until the exact source commit, defconfig, DTS/DTBO, module ABI, vendor modules and release match are proven.

Candidate family vendor source:
https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-vendor-source

Must verify exact project ID / overlay / vendor blobs for RE645F. Family resemblance is not compatibility proof.

## Installer and update-channel reality

A true stock-Recovery / stock-System-Updater OTA for a locked bootloader must satisfy the device's trusted signing and verification chain. A package signed with MANA's own keys will not become trusted by the stock Realme recovery merely by adopting the OEM ZIP layout, filename, version string or update UI. Do not spoof OEM metadata or reuse Realme signatures.

Two valid product tracks:
1. **OEM-authorized native OTA track**: obtain an OEM-authorized signing/integration route or vendor cooperation. Only this can be installed as a normal trusted Realme OTA while the stock bootloader remains locked.
2. **Owner-controlled custom firmware track**: use the device's supported bootloader-unlock/custom-recovery route, exact matching device tree and vendor stack, owner-controlled AVB keys, verified restore package, then publish signed MANA full/incremental OTA updates through a MANA updater integrated into that custom build. This changes the trust root and must be explicitly disclosed.

If neither route is available on this device, do not promise a system-partition update. A companion app can provide MANA Brain and user-granted Android integrations, but it cannot silently acquire platform-signature privileges or unrestricted control over the OS.

## Required release gates (must all pass)

1. Capture official OTA response for the exact EU RMX5566RU baseline and save sanitized metadata, component IDs, hashes, sizes, and source URLs. Do not store IMEI or GUID.
2. Acquire and inventory the full stock firmware/OTA payload if the service makes it available; extract partition images and verify hashes.
3. Confirm exact device tree, product makefiles, BoardConfig, fstab, init scripts, VINTF manifests/matrices, sepolicy, kernel config, DTS/DTBO, vendor/odm blobs and proprietary module ABI.
4. Confirm partition map, dynamic partition groups, slot behavior, AVB descriptors, rollback indexes and stock restore path.
5. Build MANA platform services as actual system components (not just app shells): protected MANA Brain Binder service, local model provider, encrypted memory, permissions/role model, update service, UI integration.
6. Build full target-files from the exact matching product configuration.
7. Generate signed full OTA using AOSP releasetools and the correct owner/OEM-authorized key route.
8. Verify payload signatures, metadata, partition compatibility, anti-rollback constraints, recovery/boot fallback and restore.
9. Test on an expendable matching device or safe test setup before any owner device installation.
10. Publish release as `MANA +1` with accurate changelog, hashes, source attribution, install requirements and rollback instructions.

## Non-negotiable safety rules

- Never claim a source-only CI run or companion APK is a flashable ROM.
- Never invent a lunch target, partition map, VINTF, vendor blob, kernel config or signing key.
- Never replace boot/vendor/modem/radio partitions with guessed images.
- Never remove AVB, SELinux or anti-rollback protections as a shortcut.
- Never claim absolute AI access. MANA Brain must use explicitly granted Android roles/permissions and a protected, auditable system-service boundary.
- Keep an owner-recoverable stock firmware path before any device modification.

## Current status

This document records researched architecture and a safe integration path. It does not assert that an official RMX5566RU full OTA was retrieved, that an exact device tree/vendor set exists, or that a MANA flashable OTA has been built. Those remain hard release gates.
