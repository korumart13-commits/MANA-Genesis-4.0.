# MANA Genesis 4.0 — Operating-system source project

MANA Genesis 4.0 is being developed as an Android 16 AOSP-derived operating system for the realme RMX5566RU (RE645F), not as a standalone launcher APK.

## Current focus

- Establish the Android platform source and product architecture.
- Verify the exact RMX5566RU device tree, vendor interface and Linux kernel source.
- Build the MANA system layer into the OS: System UI, launcher, settings, avatar, local-first MANA Brain and system services.
- Preserve the stock boot chain until device-specific sources, recovery and signing are verified.

## Project status

This repository contains project documentation, the device profile, MANA Brain JVM foundation and previous application prototypes. It does **not** yet contain a complete AOSP source checkout or a validated bootable RMX5566RU ROM/kernel build.

Core references:
- [Device profile](device/RMX5566RU_TARGET_PROFILE.md)
- [OS architecture](os/README.md)
- [Build sequence](os/BUILD_SEQUENCE.md)
- [AOSP 16 build foundation](os/AOSP16_BUILD_FOUNDATION.md)
- [Port gate matrix](os/PORT_GATE_MATRIX.md)
- [RE645F device-tree foundation](device/realme/RE645F/README.md)
- [RE645F vendor integration](vendor/realme/RE645F/README.md)
- [Kernel source audit](kernel/DEVICE_KERNEL_SOURCE_AUDIT.md)
- [Kernel integration plan](kernel/RE645F_KERNEL_INTEGRATION.md)

## Safety and accuracy

The target device reports Linux 5.15.197, Qualcomm SM6225/bengal, a locked bootloader and green Verified Boot. Do not flash experimental boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA images. Do not use another phone's SM6225 kernel as a drop-in kernel.

A source project, a successful CI build, and a bootable image are separate milestones. The project reports them separately. No APK build is part of the current milestone.
