# MANA Genesis 4.0 — full project execution status

Date: 2026-11-27
Target: realme C85 4G, RMX5566/RMX5566RU, RE645F, Qualcomm SM6225, Android 16.

## Executive status

MANA Genesis 4.0 remains a full Android 16 AOSP-derived ROM project. The repository has an Android app prototype, a service-owned MANA Engine, Brain/Core code, platform Binder service contracts, Soong definitions, AOSP packaging scripts, device audits and a flashing/recovery plan. It is not yet a complete AOSP checkout, a bootable ROM, an OTA package or a phone-installable firmware.

## Work completed in source

- Canonical MANA Brain/Core JVM modules and Shogun/Hydra integration foundation.
- Local ARM64 llama.cpp JNI build pipeline; a previous native build and ELF architecture verification succeeded.
- Android app with local-first engine routing, encrypted memory/Microworld persistence, provider adapters, voice/avatar UI, diagnostics and recovery-copy preservation.
- Dedicated `ManaEngineService` and shared `ManaEngineProcessOwner`; Activity now binds to the service instead of constructing or shutting down inference/memory resources.
- Platform AIDL interfaces, signature-permission-gated `ManaBrainService`, bounded request executor, consent policy and host tests.
- AOSP import/signing staging script for the Gradle release APK; the Soong stub is not mistaken for the full Kotlin app.
- Bounded Engine retry policy: five attempts by default, exponential delay capped at 60 seconds, bounded jitter, persistent retry budget, stable-health reset, and handling for Binder disconnect, binding death and null binding.
- Activity reconnect scheduling is restricted to the resumed lifecycle; retries are cancelled on pause. CI guards against duplicate lifecycle overrides.
- RMX5566 read-only audit analysis, target profile, source discovery report, and stock firmware/restore safety plan.

## CI evidence and current verification

- App CI run `45914698793` completed successfully for commit `a82205fc8dcec2e77e6c0135720b7a05c2f2951a`. Canonical Brain/Core tests, Android debug assembly, app unit tests, release lint (`lintVitalAnalyzeRelease` / `lintVitalRelease`) and unsigned `assembleRelease` all passed. Lint remains enabled.
- The AGP 8.13.2 / Gradle 8.13 toolchain change resolved the previously observed Lifecycle lint-detector crash in this CI run.
- Verified artifacts from run `45914698793`: source project ZIP `15161902217` (17,462,443 bytes; expires 2026-12-27); debug prototype ZIP `15161708101` (49,707,561 bytes; expires 2026-12-11); unsigned release APK ZIP `15161513522` (45,822,738 bytes; expires 2026-12-11). These are GitHub Actions artifacts, not ROM images. The release artifact is unsigned and still requires AOSP platform signing/product integration.
- The separate source-bundle workflow `45914717027` also completed successfully; it packages tracked source files only.

## Device-specific ROM blockers

1. Exact RMX5566RU/RE645F Android 16 device tree and product configuration are not verified.
2. OEM kernel source matching the reported `5.15.197-android13` release, exact defconfig, DTS/DTSI, DTBO and module ABI are not verified.
3. Matching vendor blobs and firmware from the current `RMX5566_16.0.5.1310(EX01)` branch are not verified.
4. VINTF compatibility, full partition map, dynamic partition metadata, A/B behavior and AVB/vbmeta rollback/signing chain are not verified.
5. No matching full official Android 16 restore package has been confirmed in public mirror research. Public listings surfaced older 15.0.2.* packages; they are not approved for downgrade or restore.
6. A full Android 16 AOSP checkout, Soong product build, SELinux service policy integration, emulator Binder tests and device boot tests have not been completed.
7. A tested stock recovery path is required before any bootloader unlock or partition write.

## Installation route decision

- Preferred stock recovery: Settings → Software update, or the realme Recovery local-install route only with a signed package explicitly matching the exact RMX5566RU region/build branch.
- Fastboot: not approved until OEM unlock eligibility, exact image set, partition map and restore package are verified.
- Qualcomm EDL/QFIL/Oplus service tools: not approved for owner-side MANA flashing; third-party instructions may require authenticated service access and a mismatched programmer can brick the device.
- Third-party firmware mirrors: discovery evidence only, not a trusted flash recommendation.
- MANA ROM: no flashable package may be created or installed until all device-specific gates pass.

## Next engineering sequence

1. Inspect the successful unsigned release artifact: APK manifest/package identity, signing state, SHA-256 and provenance. Do not treat it as a ROM.
2. Replace the Java-only `ManaLauncher` Soong stub with a correctly staged Gradle release APK import (`android_app_import`) while retaining the platform Binder service as a separately signed component; validate certificate and privileged-permission assumptions in a real Android 16 AOSP checkout.
3. Build the AIDL interface, `ManaBrainService`, imported launcher APK and product inclusion in a real AOSP tree; then add exact SELinux service contexts/domain and least-privilege Binder rules against that tree.
4. Emulator-test authorization, callbacks, reconnect, overload and consent boundaries before any device integration.
5. Continue read-only RMX5566 audit and exact OEM source/firmware discovery. Keep unknown fields unknown; do not infer partition layouts.
6. Only after matching OEM sources and a tested restore path are available, create the RMX5566 product/device tree, build and test images, generate checksums and prepare a signed recovery/OTA package.

## Release naming rules

- Source archive: tracked repository files only.
- Debug APK: application prototype.
- Unsigned release APK: AOSP prebuilt candidate, requiring platform signing and product integration.
- AOSP system image: platform build artifact, not necessarily device compatible.
- Bootable test image: only after emulator/device boot tests.
- Installable ROM/OTA: only after exact device compatibility, signing, recovery and rollback validation.

The project must preserve the full MANA product requirements in `docs/MASTER_PRODUCT_REQUIREMENTS.md`. Missing APIs, credentials, cloud hosting, device sources or permissions must be recorded as blockers, never simulated as completed features.
