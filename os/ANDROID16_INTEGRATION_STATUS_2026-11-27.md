# MANA Genesis 4.0 — Android 16 integration status

Audit date: 2026-11-27
Target reported by the project: RMX5566RU / RE645F / Qualcomm SM6225 (bengal) / Android API 36.

## Verified source wiring

The current Android application source already composes the shared MANA runtime:

- `launcher/app/src/main/java/com/mana/launcher/ManaEngineService.kt` and `ManaEngineBridgeService.kt` acquire the shared `ManaEngineProcessOwner`, which owns local inference, Brain routing, encrypted memory and Microworld runtime. `MainActivity.kt` binds to the service and consumes a narrow Binder API; it no longer constructs or shuts down those engine resources.
- `launcher/app/src/main/java/com/mana/launcher/ManaBrainBridge.kt` delegates routing to the canonical `com.mana.brain.ManaBrain`; local inference is attempted first and cloud use is controlled per request.
- `launcher/app/src/main/java/com/mana/launcher/ManaEngineAndroidRuntime.kt` constructs `ManaEngineRuntime`, `ManaSystemCore`, `ManaMicroworldService`, `ManaSecurityService`, `ManaHydraRegistry`, and `ManaShogun`.
- The Android composition root registers `engine.memory`, `engine.microworld`, `engine.brain`, and `engine.shogun`; the Shogun module exposes both Shogun and Hydra in the system service context.
- `os/core/src/main/kotlin/com/mana/os/core/ManaEngineRuntime.kt` routes reasoning through a `ManaReasoningPort`, applies memory-consent rules, and submits authorized tool tasks to Shogun.

This is service-owned application-process composition. The foreground service requests Android's `specialUse` foreground-service type and shows a persistent notification, subject to Android policy, user settings, background-start limits, Doze and process management. It is not yet a privileged Android system daemon, a replacement SystemUI, or a flashed operating system. True 24/7 execution is not guaranteed.

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


## Follow-up lifecycle hardening — 2026-11-27

- Updated `ManaBrainBridge` to make shutdown idempotent and reject new requests after shutdown.
- Asynchronous `respond()` now completes its callback with a failure if shutdown races with coroutine launch/cancellation; completion is guarded so the callback is delivered at most once.
- Added `ManaBrainBridgeTest.requestAfterShutdownCompletesCallbackWithFailure` as a regression test.
- This is a lifecycle correctness fix inside the existing application-process integration. It does **not** move MANA Engine into a foreground/system service and does not provide persistent background execution.
- The regression test has been committed but has not yet been executed in a verified Gradle/CI run.
\n\n## Service resilience implementation update — 2026-11-27\n\nThe MANA Engine is now owned by a dedicated app foreground service/process owner rather than Activity-local construction. A bounded retry policy, persisted retry budget, exponential backoff with jitter, stable-health reset, and Binder disconnection/binding-death/null-binding handling are implemented in source. Retry attempts are limited to a resumed Activity to respect foreground-service start restrictions. The Kotlin policy and Android app integration still require the current CI run and eventual emulator/device tests.\n