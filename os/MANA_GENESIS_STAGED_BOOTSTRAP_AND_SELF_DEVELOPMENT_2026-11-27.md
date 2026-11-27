# MANA Genesis staged bootstrap and self-development design — 2026-11-27

## Decision

Adopt a staged delivery architecture so that MANA application/OS engineering does not wait for discovery of a complete factory OTA package. This does **not** waive device bootability, compatibility, signing, or recovery requirements. A self-developing system cannot repair a device that cannot boot far enough to run its update agent.

## The bootstrap principle

The first bootable release must contain everything required to boot and maintain a safe system on the target hardware. It may defer optional MANA features and later software modules, but it may not defer essential hardware support, storage integrity, verified boot/recovery, or the updater required to install future updates.

The bootstrap is intentionally small:
- Android 16-compatible base and exact RMX5566 hardware integration, or a separately validated supported base;
- matching kernel, DTB/DTBO, vendor interface and firmware dependencies;
- AOSP product integration for MANA Brain service and the single MANA launcher/engine bridge;
- minimal local-first MANA Brain runtime with diagnostic mode if no local model is installed;
- encrypted local state with preservation-first recovery;
- signed, versioned update agent with rollback/fallback;
- owner-facing status, consent and recovery controls.

Optional AI providers, local model weights, voice/avatar packs, knowledge packs, game skills and other nonessential features can be installed after first boot as independently verified modules.

## Three delivery stages

### Stage 0 — Build and verify before device installation
- Continue Android app/engine, Brain, AIDL/Binder, update-agent and module-manifest work in the existing repository.
- Build/test AOSP components in a reproducible Android 16 environment and emulator/Cuttlefish where available.
- Clearly label emulator and source-contract results; they are not RMX5566 hardware tests.
- Produce signed update metadata and reproducible module packages. Keep production private signing keys outside GitHub.
- Do not create a purported RMX5566 flash image from guessed partitions, guessed kernel settings, unrelated vendor blobs or another phone's tree.

### Stage 1 — Minimal bootable MANA Genesis base
- Preserve the exact compatible OEM boot chain, kernel, vendor/firmware and hardware interfaces where technically and legally possible.
- Replace or integrate the Android system/product layers only after exact device compatibility and partition/AVB requirements are verified.
- Include MANA Brain platform service and its authenticated app bridge in the product; no duplicate assistant entry point.
- Boot, test radios, calls, audio, cameras, sensors, storage, encryption, suspend/resume, charging, update/recovery and SELinux on actual RMX5566 hardware.
- Keep a verified stock restoration route and a tested fallback/rollback path before any installation.
- If the required OEM base and device-specific inputs cannot be obtained, Stage 1 remains blocked; do not relabel an APK or generic GSI as a complete device ROM.

### Stage 2 — Post-boot self-development
MANA may then:
- discover device capabilities through documented Android APIs and read-only diagnostics;
- search approved sources for compatible drivers, firmware metadata, model packs and software modules;
- compare module manifests against exact device/build/API/ABI requirements;
- download into staging, verify publisher/signature, SHA-256, size, version, dependency graph and compatibility;
- install only app-level or explicitly supported modular updates without changing protected partitions;
- run unit/integration/sandbox tests, health checks and resource budgets;
- propose changes, maintain a local changelog and roll back failed modules;
- ask the owner before privileged system changes, boot-chain changes, firmware/driver updates, destructive operations, data migration, payments or external disclosure.

It may autonomously research, draft code, build sandbox artifacts and test noncritical modules within explicit resource and network limits. It must not self-grant permissions, bypass Android/AVB/SELinux, silently unlock the bootloader, replace keys, flash unknown images, or rewrite partitions without a separately approved and verified update transaction.

## OTA / factory package distinction

A full factory OTA package is not necessarily a mandatory input to every possible AOSP build, but exact hardware support and a reliable restore path are mandatory for a safe RMX5566 installation. Existing device partitions may only be reused if their exact compatibility, ownership, firmware dependencies, AVB chain and installation method are established. A component/incremental OTA is not a substitute for missing kernel source, device tree, vendor blobs or partition metadata.

The removed realme-ota client is not a dependency of the MANA updater. MANA's updater should use a provider-neutral signed manifest protocol and approved distribution endpoints. It must not scrape or redistribute proprietary firmware or pretend that a metadata lookup provides a complete restore image.

## Current RMX5566 blockers

As of this design decision, exact RMX5566 kernel source/config/DTS/DTBO, complete device tree, matching vendor blobs, AOSP product mapping, partition/AVB metadata and a tested stock restore path remain unverified. The current CI artifacts are Gradle app/source outputs, not boot/system/vendor images or a flashable ROM.

## Acceptance gates

1. Reproducible AOSP build and successful emulator boot.
2. Exact RMX5566 device configuration and matching hardware dependencies.
3. Static and runtime SELinux/AVB/signature validation.
4. Verified update package signatures, anti-rollback/version policy and atomic staging.
5. Actual-device smoke tests for all critical hardware and encrypted data.
6. Independent tested stock recovery/rollback route.
7. Explicit owner approval for the initial installation and any later protected-partition/boot-chain update.

Only after all applicable gates pass may an RMX5566 firmware image be called installable. Until then, report precise blockers and keep the work moving on the independently testable MANA components.
