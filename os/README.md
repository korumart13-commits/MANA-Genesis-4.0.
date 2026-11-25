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

## Current progress

- Target identity and firmware facts are recorded in `device/RMX5566RU_TARGET_PROFILE.md`.
- Kernel source audit is in `kernel/DEVICE_KERNEL_SOURCE_AUDIT.md`; the currently reviewed public candidates do not match the RMX5566RU runtime kernel and are not approved.
- A JVM-testable MANA system supervisor exists in `os/core`.
- A typed 16-module system catalog now defines dependencies, required/optional criticality, failure behavior and permission boundaries. See `os/SYSTEM_MODULE_GRAPH.md`.
- MANA Brain routing contract exists and prefers local inference, but a working LiteRT-LM runtime and model weights have not yet been integrated.
- MANA Launcher and MANA Assistant now exist as Android/Soong product-source modules under system/mana/apps; they are not standalone APKs and have not yet been built against AOSP.

## Build sequence

1. Pin and sync a reproducible Android 16 AOSP source tree.
2. Verify exact device kernel, device-tree and vendor inputs for RMX5566RU.
3. Implement and test MANA framework service contracts, then integrate them as AOSP product modules.
4. Build complete system images in CI and validate image/partition/AVB metadata.
5. Perform device bring-up, recovery validation and hardware/security tests before any owner-controlled installation plan.

A source foundation, a successful JVM test, a successful AOSP image build and a bootable device image are separate milestones.


## Integrated UI source milestone

- MANA Launcher HOME activity and MANA Assistant UI shell are present as Soong source modules; product inclusion is described in system/mana/mana_product.mk.
- The assistant clearly reports that inference is not connected and does not fake responses or transmit prompts.
- These Android sources have not yet been compiled against AOSP or tested on the RMX5566RU.

## Security review

See [source-level security review](SECURITY_REVIEW_2026-11-25.md). The review is not a penetration test or device certification.

## Security and storage service milestone

The host-JVM core now contains testable MANA Security and MANA Storage policy services. Security uses default-deny capability grants with audit events. Storage only evaluates registered metadata and supports user-approved reversible archive/quarantine plus restore; permanent deletion and real filesystem mutation are not implemented. Android Binder, runtime permission, SELinux, encryption, and device integration remain future work. See [service contract and integration gates](SECURITY_AND_STORAGE_SERVICES.md).


## Full owner-requirements audit

See [OWNER_FEATURE_AUDIT_2026-11-25.md](OWNER_FEATURE_AUDIT_2026-11-25.md) for the complete feature-by-feature status, duplicate launcher/assistant source finding, game integration boundaries, security gaps and required implementation order. The catalog now represents 27 modules, but this is architecture coverage rather than completed functionality.
