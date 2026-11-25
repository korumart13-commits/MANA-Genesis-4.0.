# MANA OS 4.0 — Platform source foundation

## Product direction

MANA OS is an Android 16 AOSP-derived operating-system project targeting the exact runtime hardware identity **realme RMX5566RU / RE645F, Qualcomm SM6225 / bengal / qcom**. The runtime SoC and kernel fingerprints are the engineering source of truth; do not substitute C85 5G / RMX5253 / MediaTek resources.

MANA is intended to be one local-first OS experience: MANA Brain, launcher, avatar, voice, memory, personal modules, games, settings, control center, update and recovery services. The current repository contains a source foundation and Android prototypes, **not a complete bootable ROM**.

## Current implementation

- os/core: JVM-tested system lifecycle, module catalog, default-deny security policy, storage planner, encrypted Diary/Memory/Future Updates and adaptive-knowledge services.
- engine/mana-brain: tested local-first provider routing; cloud is optional and requires explicit per-request permission.
- launcher/: Android Gradle prototype with LiteRT-LM runtime adapter, pinned Gemma 4 E2B downloader and SHA-256 checks, multi-provider cloud gateway, Yandex AI Studio/Search, read-only VK connector, encrypted local memory/knowledge, Diary/Life Manager/Future Updates, contacts, call-screening/default-dialer prototype, SAF file quarantine, paired-device dashboard, voice interaction and SceneView/Filament 3D avatar with a user-enabled floating overlay.
- game/minecraft-bedrock: official Script API Behavior Pack source and CI-packaged .mcpack world-side companion. It is not yet connected to the Android app through an authenticated bridge.
- system/mana/apps: Soong source modules for MANA Launcher, Assistant, Settings and Control Center, plus a user-addable Quick Settings tile. These are non-privileged product shells, not a replacement for AOSP SystemUI.
- os/IMPLEMENTATION_LEDGER.md: authoritative feature-by-feature status and acceptance gates.

The Gradle launcher and Soong product apps are still separate source generations. The launcher prototype's inference, avatar and personal modules are **not yet integrated into the Soong/AOSP product**. Successful JVM tests or Gradle/Soong compilation validate only those source targets; they do not prove device behavior.

## Device bring-up foundation

- [RE645F device-tree foundation](../device/realme/RE645F/README.md) — directory contract and evidence gates; intentionally not buildable yet.
- [RE645F vendor integration](../vendor/realme/RE645F/README.md) — blob provenance, VINTF and HAL requirements; no proprietary blobs included.
- [RE645F kernel integration](../kernel/RE645F_KERNEL_INTEGRATION.md) — exact 5.15.197 source requirements and rejected mismatched candidates.
- [AOSP 16 build foundation](AOSP16_BUILD_FOUNDATION.md) — reproducible platform baseline and CI stages.
- [Port gate matrix](PORT_GATE_MATRIX.md) — confirmed, partial, missing and blocked components.

## Device and platform blockers

The repository does not yet contain:
- a complete, pinned Android 16 AOSP checkout and reproducible product build;
- the exact OEM Qualcomm kernel source matching Linux 5.15.197, defconfig, DTS/DTBO and vendor modules for RMX5566RU / RE645F;
- verified VINTF/HAL/vendor blobs and partition metadata;
- a tested AVB signing, SELinux, A/B OTA, recovery and stock-restore path;
- a bootable MANA OS image tested on the target phone.

A public realme C85 5G kernel repository was audited and rejected because it is a different MediaTek configuration and Linux 5.15.180. An older SM6225 candidate reports Linux 4.19.157. Neither is an approved replacement for the target's OEM kernel.

## Design and safety principles

- Local inference is preferred; cloud providers remain optional and require explicit owner permission.
- API keys are never embedded in source. Android credentials are encrypted using Android Keystore.
- Personal memory and saved web knowledge are local and encrypted. Web snippets remain untrusted reference data, not instructions.
- File cleanup scans only an owner-selected SAF folder. It quarantines and hashes a copy first; deleting an original requires a second explicit approval and matching hashes.
- Smart Receptionist's optional 10-second auto-answer requires the owner to make MANA the default dialer and enable the setting. It is not enabled by default. Android does not expose ordinary third-party apps to the two-way PSTN audio stream, so AI conversation, recording and transcript summaries are not implemented.
- Minecraft actions use the official Script API and owner-authorized commands; no anti-cheat bypass or hidden input injection.
- Bluetooth and car actions remain constrained to Android-supported settings and explicitly paired devices; no manufacturer-specific vehicle control is claimed.
- No firmware is considered installable until the exact device sources, verified boot chain, recovery and rollback are tested.

## Build and validation

CI currently covers:
- JVM core and Brain tests;
- Android Gradle launcher source compilation and unit tests;
- non-privileged Soong UI source validation;
- glTF structural checks and avatar variant validation;
- Minecraft Behavior Pack syntax checks and .mcpack packaging.

These are source/CI milestones, not a full AOSP build or on-device certification.

## Build sequence

1. Obtain and verify the exact OEM kernel/device/vendor source package for RMX5566RU / RE645F.
2. Pin and sync a reproducible Android 16 AOSP source tree.
3. Unify the Gradle prototype features with the Soong product services and apps.
4. Implement real Android Binder services, permissions, SELinux domains and product SystemUI integration.
5. Build the complete AOSP product and verify AVB/partition/OTA metadata.
6. Test on a spare or safely recoverable target device, including hardware, thermal, battery, security and rollback tests.

See [implementation ledger](IMPLEMENTATION_LEDGER.md), [owner requirements audit](OWNER_FEATURE_AUDIT_2026-11-25.md), [security review](SECURITY_REVIEW_2026-11-25.md) and [device/kernel audit](DEVICE_KERNEL_SOURCE_AUDIT_2026-11-25.md).
