# MANA Genesis 4.0 — Operating-system source project

MANA Genesis 4.0 is being developed as an Android 16 AOSP-derived operating system for the realme RMX5566RU (RE645F), not as a standalone launcher APK.

## Current release scope (revised 2026-11-27)

The immediate deliverable is a MANA OS Design/Update APK plus device self-audit and a forward-compatible system bridge. The current APK release explicitly defers the AI app, Gemma/LLM model downloads, and drivers/kernel/vendor changes. It includes MANA HOME, app-level MANA Shade, Lock Screen Preview, Settings, system information cards, Device Audit, Firmware Readiness reporting, and MANA System Bridge API v1 for future MANA Brain integration.

First-run setup explains and requests necessary Android permissions sequentially, verifies each result, and ends with a user-controlled reboot suggestion. It must not silently reboot, bypass permissions, claim to replace protected System UI/lock screen, or flash partitions. Visual direction combines iOS 13 clarity, Samsung One UI ergonomics, and familiar Android behavior in the dark-blue MANA identity.

The APK audits only information exposed by Android APIs, marks inaccessible/unknown values explicitly, and generates a compatibility/missing-components report for the later RMX5566RU AOSP firmware stage. It cannot obtain proprietary OEM sources or build a full ROM on-device. Full AOSP/ROM development remains the later goal after exact device tree, kernel/vendor sources, VINTF, AVB/signing and recovery are verified. See [the revised release scope and self-audit specification](docs/MANA_OS_REVISED_SCOPE_AND_SELF_AUDIT_RU.md).

## Later platform goal

- Verify exact RMX5566RU device tree, vendor interface and Linux kernel source.
- Build the MANA system layer into AOSP after the device-specific source and recovery gates pass.
- Preserve the stock boot chain until device-specific sources, recovery and signing are verified.

## Core architecture

The requested Shogun MANA brain, MANA Hydra capability orchestration and MANA Engine integration plan are documented in [docs/SHOGUN_MANA_HYDRA_MANA_ENGINE_ARCHITECTURE.md](docs/SHOGUN_MANA_HYDRA_MANA_ENGINE_ARCHITECTURE.md). Existing MANA Brain code is preserved as the initial inference-routing foundation.

Multi-model dialogue federation (OpenAI, Anthropic, Google, xAI and local inference) is specified in [docs/MANA_MULTI_GPT_DIALOGUE_ARCHITECTURE.md](docs/MANA_MULTI_GPT_DIALOGUE_ARCHITECTURE.md). This is an architecture specification; provider adapters and live multi-model dialogue still require implementation and testing.

## Project status

The full implementation status, CI evidence, ROM blockers and next engineering stages are tracked in [docs/PROJECT_EXECUTION_STATUS_2026-11-27.md](docs/PROJECT_EXECUTION_STATUS_2026-11-27.md).

This repository contains the MANA product requirements, Android application/runtime source, MANA Brain JVM foundation, platform Binder service contracts, AOSP packaging scripts, device profile and flashing/recovery research. The launcher workflow was corrected after run 1011 failed on an incorrect Gemma download filename; run 1012 is rebuilding with an official Hugging Face file URL and compiled-manifest checks for dialer removal. See [the CI remediation and RMX5566RU firmware gate report](docs/CI_REMEDIATION_AND_RMX5566RU_GATE_2026-11-27.md). The repository does **not** contain a complete AOSP source checkout or a validated bootable RMX5566RU ROM/kernel build.

The device profile is in [device/RMX5566RU_TARGET_PROFILE.md](device/RMX5566RU_TARGET_PROFILE.md). The OS architecture and stages are in [os/README.md](os/README.md) and [os/BUILD_SEQUENCE.md](os/BUILD_SEQUENCE.md). Kernel source findings are in [kernel/DEVICE_KERNEL_SOURCE_AUDIT.md](kernel/DEVICE_KERNEL_SOURCE_AUDIT.md).

## Safety and accuracy

The target device reports Linux 5.15.197, Qualcomm SM6225/bengal, a locked bootloader and green Verified Boot. Do not flash experimental boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA images. Do not use another phone's SM6225 kernel as a drop-in kernel.

A source project, a successful Gradle APK build, a full AOSP/Soong product build, and a bootable image are separate milestones. The current pipeline distinguishes source archives, debug prototypes and unsigned release APKs. None of those is a flashable ROM. The target remains blocked on exact device tree, OEM kernel/vendor sources, VINTF, partition/AVB metadata and a verified stock restore path.


## Unified MANA self-audit and firmware-preparation product

The bootstrapper is part of this same MANA repository and is not a separate product. Its device profile, audit scripts and tests live under `bootstrapper/`; the Android-facing integration lives in the existing `launcher/app` module. The OTA capability probe is integrated at `launcher/app/src/main/java/com/mana/launcher/ManaOtaCapabilityProbe.kt` (merged from PR #19). The intended user experience is one MANA application installed normally, with the audit, firmware-readiness and future update workflow presented inside MANA.

### Unified workflow target

1. On first launch, MANA identifies the device using Android public APIs and gathers only information the OS exposes; unknown or restricted values remain explicitly unknown.
2. MANA runs the existing device audit and compares the result with `device/RMX5566RU_TARGET_PROFILE.md` and firmware gates.
3. MANA checks update capabilities, signature/hash validation and available trusted sources. The OTA capability probe is read-only; it does not grant itself Device Owner or elevated privileges.
4. MANA can prepare source changes and run tests/builds through the repository's CI/build workers when configured. The Android app itself is not an AOSP build host and must not claim it can manufacture proprietary vendor files.
5. A future RMX5566RU image is eligible for installation only after exact-device compatibility, trusted signing, OTA support and a verified stock-recovery path are all proven. Otherwise the app reports BLOCKED with the missing gates.

### Important product boundary

There is one MANA project and one intended MANA app experience; no separate Bootstrapper app is required. The current `bootstrapper/bootstrap.sh` is a developer/CI audit utility, not a user-facing installation requirement. The Android app integration is in progress and must be built and tested before an installable APK can be claimed. A normal APK cannot silently replace protected Android partitions, unlock the bootloader, bypass Verified Boot, or reboot/flash without explicit user confirmation. These limits do not change the long-term goal of evolving this same repository into the RMX5566RU MANA firmware.
