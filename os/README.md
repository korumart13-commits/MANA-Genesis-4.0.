# MANA OS 4.0 — Platform source foundation

## Product direction

MANA OS is an Android 16 AOSP-derived operating-system project for the realme RMX5566RU (RE645F). It is not a Linux distribution that can replace Android with a standalone kernel. The platform consists of:

1. AOSP Android userspace and framework.
2. A device-specific Linux kernel and Qualcomm vendor integration.
3. MANA system services and privileged system applications.
4. MANA System UI, launcher, settings, avatar runtime and local-first AI.
5. Verified, signed OTA/update and recovery integration.

The current repository is the project coordination and source-foundation repository. It does not yet contain a complete AOSP checkout, a validated device tree, vendor blobs, a matching kernel source/configuration, or a bootable image.

## Required architecture

- `platform/`: AOSP manifest and product integration, once the exact source branch is pinned.
- `device/realme/RMX5566RU/`: device tree, BoardConfig, fstab, init, VINTF, overlays and SELinux policy; create only from verified device data.
- `kernel/realme/RMX5566RU/`: exact matching kernel source, defconfig, DTS/DTBO, modules and build configuration.
- `vendor/realme/RMX5566RU/`: legally obtained proprietary blobs and extraction records.
- `system/mana/`: MANA framework services, system applications, resources and product configuration.
- `ai/`: MANA Brain, LiteRT-LM runtime adapter, model lifecycle, safety and offline data.
- `security/`: AVB/signing, SELinux, permission model, update verification and threat model.

## Design principles

- Local-first operation. Cloud model providers are optional and require explicit owner opt-in.
- No embedded API keys, hidden remote access, or bypass of Android permission boundaries.
- Preserve hardware support: modem, radio, camera, audio, display, sensors, charging and power management must be validated against the exact device.
- Preserve rollback and recovery capability. Never overwrite the stock boot chain while the device is locked.
- Do not label an image bootable until it has been built from matching sources and passed image, partition, AVB and device tests.

## Current stage

Stage 0 — target identification and source audit. Device properties are recorded in `device/RMX5566RU_TARGET_PROFILE.md`. Kernel source candidates are audited in `kernel/DEVICE_KERNEL_SOURCE_AUDIT.md`.

Stage 1 — establish a reproducible Android 16 source manifest and verify the exact device kernel/device-tree/vendor inputs.

Stage 2 — integrate the MANA system layer into the Android product build.

Stage 3 — build and test the full system images in CI; no flashing during these stages.

Stage 4 — device bring-up, recovery validation, AVB/signing and only then an owner-controlled installation plan.
