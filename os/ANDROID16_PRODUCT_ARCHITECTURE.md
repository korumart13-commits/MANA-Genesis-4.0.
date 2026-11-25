# MANA Genesis 4.0 — Android 16 product architecture

Status: architecture baseline / implementation plan. This document is not evidence of a bootable ROM.

## Product definition

MANA Genesis 4.0 is intended to be a complete Android 16-based operating system for the exact realme RMX5566RU / RE645F hardware target. It is not a launcher-only APK and not a replacement set of mock applications.

The Android platform remains the compatibility and hardware foundation. MANA supplies the branded user experience, system-level assistant integration, and additional services.

## User experience

- Overall interaction model: familiar Android, with visual and organizational cues from Samsung One UI.
- Brand: deep navy and blue surfaces, restrained cyan accent, high contrast text, coherent iconography and typography.
- Home: configurable grid, dock, folders, widgets, app search, app drawer, gesture navigation compatibility and adaptive layouts.
- System surfaces: MANA-styled quick settings and notification shade only when implemented as a real platform SystemUI integration. Until then, use Android's existing system surfaces rather than pretending a normal app can replace them.
- Settings: MANA entry points grouped into a coherent settings experience, while preserving Android's real settings activities and permission controllers.
- Lock screen, recents, status bar, navigation, notifications, permission prompts and multi-window behavior must remain platform-owned or be changed only through proper AOSP integration.

## Default applications

Use the corresponding Android 16/AOSP system applications and platform intents for core functions wherever available and compatible:

- Camera: Android camera application plus the exact device Camera HAL and camera provider implementation.
- Phone and in-call UI: Android dialer/telecom stack, default-dialer role and device-compatible telephony components.
- Contacts: Android Contacts provider and contacts application.
- Messaging: Android messaging application and SMS/MMS/RCS support only where the platform and carrier components actually provide it.
- Files, clock, calculator, calendar and other basics: compatible Android 16/AOSP applications where available.

Do not replace these with MANA demo screens. Do not assume an AOSP app alone makes hardware work: camera sensors, modem, audio routing, IMS, vendor HALs, permissions and firmware must match the target device. Do not bundle Google apps or proprietary OEM applications without verified licensing and distribution rights.

## MANA AI as a system feature

- One MANA identity, not several competing assistant personas.
- Assistant entry points from the home screen, supported system gesture/button integration, and explicit app shortcuts.
- Avatar is a presentation surface for the assistant, not the operating system itself. It must have a non-overlay accessible fallback.
- Local inference is preferred. Cloud providers are optional and require explicit per-request consent.
- Memory is local, encrypted, inspectable and owner-controlled.
- System actions are allowlisted, least-privilege and confirmation-gated. No root, arbitrary shell, hidden input injection or silent permission granting.
- When inference is unavailable, report the failure honestly; never fabricate a response or claim a system action succeeded.

## Platform integration layers

1. Android 16 base: pinned AOSP manifest, reproducible source sync and build toolchain.
2. Device layer: exact RE645F product/device configuration, matching vendor/ODM blobs, VINTF, init, fstab and firmware.
3. Kernel layer: exact OEM kernel source, defconfig, DTS/DTBO and verified boot image configuration.
4. MANA platform layer: product packages, framework services, Binder/AIDL contracts, permissions, SELinux domains and signed system apps.
5. UX layer: MANA launcher, true platform SystemUI integration, settings, control center and avatar surface.
6. Assistant layer: Brain routing, local inference provider packaged in the platform or a deliberately authenticated companion IPC service, optional cloud adapters, encrypted memory and voice.
7. Validation: Soong build, unit/instrumentation tests, VINTF/SELinux checks, CTS/VTS-relevant checks, and exact-device hardware testing.

## Build and release gates

- Do not create a guessed lunch target or BoardConfig.
- Do not substitute a generic SM6225 kernel or vendor image.
- Do not mark camera, telephony, Wi-Fi, Bluetooth, audio, sensors, charging or suspend as working until tested on the exact device.
- Do not generate a flashable OTA or tell the owner to unlock/flash until AVB, partition layout, recovery and restore are verified.
- Track source build, successful image build, first boot and device acceptance as separate milestones.

## Current baseline and next engineering milestones

Existing repository work includes a tested JVM MANA core, a separate Android companion APK with a local LiteRT-LM runtime, initial Soong Brain/AIDL/Binder integration sources, and partial MANA launcher/settings/control-center sources. The companion APK is not a ROM; the Soong system Brain still lacks a packaged LiteRT-LM provider or authenticated IPC connection to the companion.

Next milestones, in order:
1. Establish and pin a reproducible Android 16 AOSP source manifest in CI.
2. Audit the existing launcher and platform UI sources against the One UI-inspired home experience; implement missing launcher behavior without changing stock Android apps.
3. Add the exact Android 16/AOSP core app modules to the verified product configuration after source sync confirms their module names and availability.
4. Complete the authenticated cross-process local inference bridge or package the provider into the platform build.
5. Integrate real MANA SystemUI changes, settings and permission/SELinux policy.
6. Obtain and validate the exact RE645F device tree, OEM kernel and matching vendor/ODM artifacts.
7. Build and test the complete Android 16 product images in CI; only then begin device acceptance.

## Current status

This is an architecture and release-gate document. It does not claim that AOSP has been synced, a RE645F lunch target exists, Android 16 stock apps are already included in a product image, or a firmware image has been built.
