# MANA Genesis 4.0 — Android 16 integration status

Audit date: 2026-11-27
Target reported by the project: RMX5566RU / RE645F / Qualcomm SM6225 (bengal) / Android API 36.

## Verified source wiring

The current Android application source already composes the shared MANA runtime:

- `launcher/app/src/main/java/com/mana/launcher/MainActivity.kt` creates the local inference runtime, cloud providers, `ManaBrainBridge`, encrypted memory services, and `ManaEngineAndroidRuntime`.
- `launcher/app/src/main/java/com/mana/launcher/ManaBrainBridge.kt` delegates routing to the canonical `com.mana.brain.ManaBrain`; local inference is attempted first and cloud use is controlled per request.
- `launcher/app/src/main/java/com/mana/launcher/ManaEngineAndroidRuntime.kt` constructs `ManaEngineRuntime`, `ManaSystemCore`, `ManaMicroworldService`, `ManaSecurityService`, `ManaHydraRegistry`, and `ManaShogun`.
- The Android composition root registers `engine.memory`, `engine.microworld`, `engine.brain`, and `engine.shogun`; the Shogun module exposes both Shogun and Hydra in the system service context.
- `os/core/src/main/kotlin/com/mana/os/core/ManaEngineRuntime.kt` routes reasoning through a `ManaReasoningPort`, applies memory-consent rules, and submits authorized tool tasks to Shogun.

This is application-process composition. It is not yet a privileged Android system daemon, a replacement SystemUI, or a flashed operating system. The app can be stopped by Android process management; 24/7 execution is not established by this wiring.

## Android 16 compatibility status

- Android compile SDK is 36.
- The app target SDK is currently 35, so it is not yet configured as an API-36-targeted Android 16 app.
- The device profile used by `ManaEngineAndroidRuntime` is read from Android runtime build properties. This does not prove that the app is running on the exact RMX5566RU production build.
- The repository workflow defines JVM tests for `:brain:test` and `:core:test`, Android debug assembly, and Android unit tests. No successful run for the latest integration commits has been verified in this audit.

## Existing verified test scope

Previously recorded project results:
- MANA OS Core / Microworld JVM test workflow succeeded.
- ARM64 llama.cpp JNI native compilation and ELF architecture verification succeeded.

Those results do not prove on-device LiteRT-LM or llama.cpp inference, thermal/performance suitability, Android 16 API-36 target compatibility, or ROM bootability.

## ROM release blockers

Do not produce or flash a ROM until the repository contains and validates the exact matching device materials and build configuration:

1. Exact RMX5566RU / RE645F device tree and product configuration.
2. Matching Qualcomm SM6225 kernel source/configuration, device-tree sources and DTBO build inputs.
3. Matching proprietary vendor blobs, firmware and vendor modules for the target stock build.
4. VINTF manifests/matrices and HAL compatibility checks.
5. Partition layout, dynamic partition metadata, boot/vendor_boot/init_boot/recovery image rules and slot behavior.
6. AVB/vbmeta signing and rollback-index strategy compatible with the target boot chain.
7. Reproducible AOSP/Android 16 build, image verification, boot test and tested stock recovery procedure.

No claim is made here that these materials were never supplied; this status records only what has been verified in the inspected source files and workflow.

## Next engineering steps

1. Run the existing Gradle workflow and resolve concrete compile/test failures before expanding code.
2. Complete API-36 target-SDK migration with permission and foreground-service review, then test on Android 16.
3. Move the runtime from Activity-owned application composition toward a user-consented, lifecycle-managed Android service; do not claim a system daemon without platform integration and signing.
4. Validate Shogun/Hydra tool registration and capability checks against Android permission/Binder boundaries.
5. Audit the exact RMX5566RU OEM inputs before creating any ROM product/device configuration.

## Artifact naming rule

A debug APK is an application prototype. A source ZIP is a source archive. Neither is a flashable ROM, OTA update, or recovery package.
