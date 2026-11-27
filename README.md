# MANA Genesis 4.0 — Operating-system source project

MANA Genesis 4.0 is being developed as an Android 16 AOSP-derived operating system for the realme RMX5566RU (RE645F), not as a standalone launcher APK.

## Current focus

- Establish the Android platform source and product architecture.
- Verify the exact RMX5566RU device tree, vendor interface and Linux kernel source.
- Build the MANA system layer into the OS: System UI, launcher, settings, avatar, local-first MANA Brain and system services.
- Preserve the stock boot chain until device-specific sources, recovery and signing are verified.

## Core architecture

The requested Shogun MANA brain, MANA Hydra capability orchestration and MANA Engine integration plan are documented in [docs/SHOGUN_MANA_HYDRA_MANA_ENGINE_ARCHITECTURE.md](docs/SHOGUN_MANA_HYDRA_MANA_ENGINE_ARCHITECTURE.md). Existing MANA Brain code is preserved as the initial inference-routing foundation.

Multi-model dialogue federation (OpenAI, Anthropic, Google, xAI and local inference) is specified in [docs/MANA_MULTI_GPT_DIALOGUE_ARCHITECTURE.md](docs/MANA_MULTI_GPT_DIALOGUE_ARCHITECTURE.md). This is an architecture specification; provider adapters and live multi-model dialogue still require implementation and testing.

## Project status

The full implementation status, CI evidence, ROM blockers and next engineering stages are tracked in [docs/PROJECT_EXECUTION_STATUS_2026-11-27.md](docs/PROJECT_EXECUTION_STATUS_2026-11-27.md).

This repository contains the MANA product requirements, Android application/runtime source, MANA Brain JVM foundation, platform Binder service contracts, AOSP packaging scripts, device profile and flashing/recovery research. CI builds the source archive and verifies the Android application source; the latest release APK build is still under investigation. The repository does **not** contain a complete AOSP source checkout or a validated bootable RMX5566RU ROM/kernel build.

The device profile is in [device/RMX5566RU_TARGET_PROFILE.md](device/RMX5566RU_TARGET_PROFILE.md). The OS architecture and stages are in [os/README.md](os/README.md) and [os/BUILD_SEQUENCE.md](os/BUILD_SEQUENCE.md). Kernel source findings are in [kernel/DEVICE_KERNEL_SOURCE_AUDIT.md](kernel/DEVICE_KERNEL_SOURCE_AUDIT.md).

## Safety and accuracy

The target device reports Linux 5.15.197, Qualcomm SM6225/bengal, a locked bootloader and green Verified Boot. Do not flash experimental boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA images. Do not use another phone's SM6225 kernel as a drop-in kernel.

A source project, a successful Gradle APK build, a full AOSP/Soong product build, and a bootable image are separate milestones. The current pipeline distinguishes source archives, debug prototypes and unsigned release APKs. None of those is a flashable ROM. The target remains blocked on exact device tree, OEM kernel/vendor sources, VINTF, partition/AVB metadata and a verified stock restore path.
