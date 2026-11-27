# MANA OS — Revised Release Scope and Self-Audit
**Date:** 2026-11-27 | **Target:** realme RMX5566RU / RE645F, Android 16

## 1. Current scenario — overrides the older immediate-release plan

The immediate release is **MANA OS Design/Update + Device Self-Audit**, not the earlier AI-enabled APK. The full MANA Genesis 4.0 and ROM vision remains the long-term goal.

**Include now**
- One installable MANA OS APK and a source/project archive.
- MANA HOME launcher.
- MANA Shade as an app-level quick-actions/notification design surface.
- Lock Screen Preview (a preview, not a claim of replacing Android's protected lock screen).
- MANA Settings and system information cards.
- Device Audit and Firmware Readiness report.
- MANA System Bridge API v1 as the future integration point for `com.mana.genesis` / MANA Brain.
- Dark-blue MANA identity combining iOS 13 clarity, Samsung One UI ergonomics and familiar Android interaction patterns.

**Defer from this immediate release**
- Separate MANA AI app/UI, Gemma/LLM downloads and model runtime.
- Kernel/device drivers, vendor blobs and firmware changes.
- Any built-in dialer, number entry, call UI or call handlers (prohibited).
- Replacing protected System UI, notification shade or secure lock screen from an ordinary APK.

## 2. First-run installation experience

1. User installs and opens MANA.
2. Explain each permission's purpose and request only permissions needed by a real current feature.
3. Request permissions sequentially, one at a time, using Android's real permission dialogs.
4. Verify each grant/denial; continue gracefully when denied and state which feature is unavailable.
5. After setup, show a completion screen and a reboot suggestion.
6. Never reboot silently, bypass permission prompts, request broad permissions without purpose, or claim system privilege the APK does not have. The user decides whether to reboot.

## 3. Device self-audit

The installed APK inspects only information exposed by documented Android APIs:
- manufacturer/model/product/device identifiers where available;
- Android version/API level, ABI, build fingerprint and accessible system properties;
- memory/storage/battery/network information exposed to the app;
- app permissions and available capabilities.

Every field must be labelled `DETECTED`, `NOT_ACCESSIBLE`, `UNKNOWN` or `MISMATCH`. Generate a machine-readable `MANA_DEVICE_AUDIT.json` plus a readable report. Do not use root, exploit protected interfaces, read restricted partitions, or infer missing values as facts.

Compare the result with the target profile RMX5566RU / RE645F. Do not confuse it with the C85 5G / MediaTek model. Device properties previously recorded (Android 16/API 36, Qualcomm SM6225/Bengal, ARM64, kernel 5.15.197, locked bootloader, green Verified Boot) are reference facts to re-check where possible, not values to blindly claim as newly detected.

## 4. Firmware readiness and project continuation

The project should use audit results to produce a concrete gap report and backlog for the later AOSP firmware stage. Track:
- exact device tree, BoardConfig, product makefiles, fstab, init and overlays;
- matching OEM kernel source, release, defconfig, DTS/DTBO and module ABI;
- exact vendor/ODM blobs and firmware;
- VINTF manifest and compatibility matrix, HAL coverage;
- partition layout, A/B/dynamic partition metadata;
- AVB chain, signing/rollback data and a verified stock restore path.

Each item must have evidence and a status: `VERIFIED`, `MISSING`, `UNKNOWN` or `INCOMPATIBLE`. The project may generate tasks and propose patches, but changes must be reviewed and tested in a development branch. Do not fabricate blobs/drivers or claim a successful firmware build from a source stub.

The same codebase can later grow to include the missing device components and AOSP/Soong build orchestration. A full Android build requires a proper Linux/CI build environment and exact source inputs; an ordinary APK cannot compile itself into a bootable ROM on the phone or obtain proprietary OEM components by itself.

## 5. MANA System Bridge API v1

Define a versioned capability interface for:
- capability discovery and API version;
- permitted device information queries;
- permission/consent checks;
- safe supported Android actions;
- diagnostics and audit events;
- future connection to MANA Brain / `com.mana.genesis`.

For the eventual system build, define a signature-protected Binder/AIDL contract. The APK prototype must not pretend it already has platform signature or system privileges.

## 6. Acceptance criteria for the immediate release

- APK is built, installable and signed appropriately; record package ID and SHA-256.
- Target-device installation is tested, or explicitly marked untested.
- Permission prompts are sequential and denial is handled.
- Completion/reboot suggestion is shown; no silent reboot.
- MANA HOME, Settings, app-level Shade and Lock Screen Preview operate within public Android APIs.
- Device Audit generates the JSON/readable report and labels inaccessible data.
- Firmware Readiness report identifies missing/unknown OEM components without inventing results.
- Bridge API v1 and contract tests are included.
- No dialer/call UI or call permissions are included.
- Source project and instructions are packaged.
- The deliverable is called a Design/Update APK and diagnostic project, never a flashable ROM.

## 7. Long-term features preserved, not cancelled

MANA Brain, local inference, Gemma, cloud providers, voice/avatar, persistent memory, Microworld, Cloud Home, Google/Yandex/VK integrations, Life Manager, Diary and other previously agreed modules remain on the long-term roadmap. They are intentionally not bundled into this immediate update APK. They should connect through the bridge and be added in later verified stages.

## 8. Safety gates for the eventual ROM

Do not unlock, root, wipe, write partitions or flash boot/init_boot/vendor_boot/dtbo/vbmeta/recovery/super/OTA images until exact device sources, AVB/signing, partition layout and a tested stock recovery route are verified. Only after full AOSP/Soong build, VINTF/SELinux/AVB checks, emulator tests and real-device hardware tests may a package be called an installable RMX5566RU ROM.
