# MANA Genesis 4.0 — Full ROM release target

Status: authoritative scope clarification; implementation and device bring-up are not complete.
Target: realme RMX5566RU / RE645F, Qualcomm SM6225 (bengal/qcom), Android 16 / API 36.

## 1. What the requested release is

The requested deliverable is a complete, independently bootable MANA OS ROM for the exact target device. It is not:
- a Google/realme OTA update;
- an incremental patch that preserves realme UI as the product shell;
- a launcher or assistant APK;
- a generic GSI or a ROM image for another C85 variant;
- a ZIP containing documentation or source code presented as firmware.

The intended final deliverable is a device-matched, signed, reproducible full ROM package with documented installation prerequisites, verified boot/recovery behavior, and a tested stock restore route. Until all gates below pass, repository ZIPs and CI artifacts must be labelled source bundles, never flashable firmware.

## 2. Product identity — do not simplify or split it

The full ROM must implement one MANA Genesis system:
- MANA: one persistent identity, personality, memory and intelligence.
- Shogun MANA: central reasoning, planning and orchestration.
- MANA Hydra: permission-bounded specialist capabilities.
- MANA Engine: model/runtime routing, services, tools and integration contracts.
- MANA OS: Android 16/AOSP-derived system body, framework integration, System UI, launcher, settings, services and recovery/update layer.
- Phone: MANA's physical home and resource environment.
- MANA Microworld: persistent local-first world model, knowledge graph, creation/exploration and sandbox simulation.

The existing MANA features remain in scope: local/offline AI, optional user-configured cloud providers, voice/avatar, memory, adaptive knowledge, diary, Life Manager, contacts/receptionist, Yandex/VK, games/Minecraft, paired devices, diagnostics, backups, recovery and controlled updates. A catalog entry or prototype does not count as an implemented feature.

## 3. Required build inputs

### Android platform
- Pin a specific Android 16 AOSP manifest/tag and toolchain.
- Maintain a reproducible full source checkout and product build.
- Integrate MANA modules into the product configuration; unify the existing Gradle prototype and Soong product modules rather than shipping two competing implementations.
- Build and test framework, system, system_ext, product and required boot-related artifacts as applicable to the exact partition layout.

### Exact RMX5566RU device support
Required before a device ROM can be assembled:
- OEM-matched Qualcomm SM6225/bengal kernel source corresponding to the running 5.15.197 build, including defconfig and toolchain.
- Matching DTS/DTSI/DTBO, vendor kernel modules and KMI/symbol information.
- Exact vendor/odm/proprietary blobs and HAL/VINTF manifests and compatibility matrices.
- Exact boot, init_boot, vendor_boot, dtbo, vbmeta, super/dynamic-partition metadata and A/B slot layout.
- OEM AVB chain/signing constraints and a lawful, tested recovery/stock-restore route.

A similarly named C85 5G MediaTek tree, a generic SM6225 kernel, or a random Android 16 GSI is not a valid substitute. Runtime identity RMX5566RU / RE645F / SM6225 / bengal / qcom / Linux 5.15.197 controls compatibility.

## 4. Engineering sequence

1. Preserve the existing tested JVM core and current feature prototypes.
2. Unify Gradle and Soong generations behind stable MANA Engine and Android service contracts.
3. Implement Shogun orchestration, Hydra capability registry/permission broker, persistent Microworld data model and local inference adapters.
4. Pin and sync Android 16 AOSP; create the MANA product configuration and compile the integrated product in cloud CI.
5. Acquire and verify exact OEM device/kernel/vendor artifacts; bring up display, touch, audio, camera, radio, Wi-Fi, Bluetooth, sensors, charging and suspend.
6. Implement SELinux enforcing domains, Binder caller checks, Android permissions, AVB signing, A/B update and rollback.
7. Run CTS/VTS and device-specific smoke, thermal, battery, storage, encryption and recovery tests.
8. Produce a signed full ROM only after all gates pass; test it on a spare or safely recoverable device before any daily-phone installation.

## 5. Device safety and current blocker

The recorded phone has a locked bootloader and green Verified Boot. Do not attempt to bypass the bootloader, defeat AVB, exploit root, or silently modify protected partitions. Do not flash experimental boot/vendor_boot/dtbo/vbmeta/recovery/super images.

The repository audit currently says the exact matching Qualcomm OEM kernel/device tree/vendor package, full pinned AOSP product build, AVB signing and recovery/rollback path are not yet verified. Therefore there is no installable MANA Genesis 4.0 ROM today. This is a technical release blocker, not a change in the requested goal.

## 6. Acceptance and truthful release labels

- Source foundation: source files exist.
- CI-validated module: the named module's tests/build pass.
- AOSP product build: complete platform product compiles.
- Device bring-up: exact hardware boots and critical peripherals work.
- Flashable ROM: signed, device-matched package passes integrity and recovery checks.
- Release-ready ROM: installation, security, functionality and rollback are tested on the exact target.

Never promote one milestone to the next without evidence. No package may be called a full ROM until it is a verified, installable image for RMX5566RU / RE645F.
