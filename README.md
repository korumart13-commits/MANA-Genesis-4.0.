# MANA Genesis 4.0 — Operating-system source project

MANA Genesis 4.0 is being developed as an Android 16 AOSP-derived operating system for the realme RMX5566RU (RE645F), not as a standalone launcher APK.

## Current focus

- Establish the Android platform source and product architecture.
- Verify the exact RMX5566RU device tree, vendor interface and Linux kernel source.
- Build the MANA system layer into the OS: System UI, launcher, settings, avatar, local-first MANA Brain and system services.
- Preserve the stock boot chain until device-specific sources, recovery and signing are verified.

## Project status

This repository currently contains the project documentation, device profile, MANA Brain JVM foundation and previous application prototypes. It does **not** yet contain a complete AOSP source checkout or a validated bootable RMX5566RU ROM/kernel build.

The device profile is in [device/RMX5566RU_TARGET_PROFILE.md](device/RMX5566RU_TARGET_PROFILE.md). The OS architecture and stages are in [os/README.md](os/README.md) and [os/BUILD_SEQUENCE.md](os/BUILD_SEQUENCE.md). Kernel source findings are in [kernel/DEVICE_KERNEL_SOURCE_AUDIT.md](kernel/DEVICE_KERNEL_SOURCE_AUDIT.md).

## Safety and accuracy

The target device reports Linux 5.15.197, Qualcomm SM6225/bengal, a locked bootloader and green Verified Boot. Do not flash experimental boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA images. Do not use another phone's SM6225 kernel as a drop-in kernel.

A source project, a successful CI build, and a bootable image are separate milestones. The project will report them separately. No APK build is part of the current milestone.
