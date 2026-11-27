# MANA Brain platform integration status — 2026-11-27

## Implemented source components

- Shared AIDL contracts: `system/mana/interfaces`, including the platform Brain API and app-engine backend API.
- Android Binder service endpoint: `system/mana/services/ManaBrainService`.
- Gradle application-side `ManaEngineBridgeService` backend endpoint.
- Signature-level bind permission: `com.mana.permission.BIND_BRAIN`, with permission checks repeated inside Binder entry points.
- Platform service binds explicitly to `com.mana.launcher/.ManaEngineBridgeService`, negotiates protocol version 1, forwards requests and relays asynchronous results/errors.
- Reference-counted `ManaEngineProcessOwner` shares one local model, Brain, encrypted memory and Microworld runtime between the foreground service and bridge.
- Saved-model bootstrap runs asynchronously and only attempts a model already saved/selected locally. It does not silently download a model or accept a license.
- Per-request cloud/sensitive-data policy checks, provider allowlist, 32,768-character prompt limit and separate consent flags for personal-memory export.
- Dedicated bounded 32-request executor with explicit overload behavior.
- Product fragment `system/mana/mana_product.mk` declares `ManaBrainService`.
- Dependency-free Java host policy tests and Soong host-test source target.

## Latest verification evidence

GitHub Actions run [45909435510](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45909435510) completed successfully:

- MANA Brain and OS core tests passed.
- Platform request-policy host tests passed.
- AIDL, bridge, permission, ownership and AOSP-manifest source assertions passed.
- Embedded glTF validation passed.
- Android Gradle `:app:assembleDebug` and `:app:testDebugUnitTest` passed.
- Source-project and debug-prototype artifacts were uploaded separately.

This confirms source-level and Gradle app compilation. It does **not** confirm a Soong/AOSP build, a complete ROM, successful Binder connection on a device, or RMX5566 hardware compatibility.

## Integration blockers

1. The existing Soong `ManaLauncher` module is a separate minimal Java home-app stub. It does not package the Gradle Kotlin app or `ManaEngineBridgeService`.
2. The bridge app must be packaged into the AOSP product as package `com.mana.launcher` and signed with a certificate compatible with the platform service's signature permission. The standalone debug APK is not platform-signed.
3. `ManaBrainService` and `mana-brain-interface` have not yet been built in a full Android 16 AOSP/Soong checkout.
4. SELinux service type, service_contexts mapping and least-privilege policy rules have not yet been integrated/tested against the actual AOSP policy tree.
5. The product fragment has not been included by a verified device product makefile; no emulator or phone Binder instrumentation test has been run.
6. RMX5566RU device tree, exact matching kernel source/config/DTS, vendor blobs, VINTF, partition/AVB metadata and stock restore procedure remain unverified.

Until those blockers are resolved, the platform service must fail closed and report backend unavailable/disconnected rather than fabricate an answer.

## Next implementation sequence

1. Determine and implement a reproducible way to package the Gradle Kotlin engine/bridge into an AOSP product with the required package identity and platform signing. Do not assume the debug APK can simply be dropped into the product.
2. Establish an Android 16 AOSP checkout and pin its exact manifest revision.
3. Build the AIDL interface and `ManaBrainService` with Soong; resolve compile errors from actual Soong output.
4. Add and test the SELinux service context/domain and least-privilege access rules.
5. Add Binder instrumentation tests for authorization, callback delivery, service death/reconnect, overload, sensitive/cloud rejection and separate memory-export consent.
6. Verify the service and app bridge on an emulator before device integration.
7. Continue the RMX5566RU read-only hardware/firmware audit using the newly confirmed device profile. The first owner-provided runtime audit is now documented in `device/RMX5566RU_RUNTIME_AUDIT_ANALYSIS_2026-11-27.md`. Do not create or flash device images until exact OEM sources, blobs, VINTF and AVB/restore data are verified.

## Latest RMX5566 runtime audit evidence

The owner-uploaded read-only archive `mana_rmx5566_audit_20261127_022142.zip` was parsed and its XML files validated for well-formedness. The full findings and archive SHA-256 are recorded in `device/RMX5566RU_RUNTIME_AUDIT_ANALYSIS_2026-11-27.md`. The archive confirms Android 16 / SDK 36, vendor API and VNDK 33, first API level 35, locked bootloader, green Verified Boot, AVB 1.2, dynamic partitions, file-based encryption, slot `_a`, and vendor manifest target FCM level 7 with SELinux policy version 33.0. Partition inventory, runtime SELinux status, kernel config, device-tree metadata and module inventory remain unknown because access was denied or the collector had a syntax defect. The collector defect was fixed in v3; rerun only if further module inventory is needed. This evidence does not establish a compatible AOSP device build.

## Device identity evidence

The owner-provided Settings screenshots confirm model RMX5566, build `RMX5566_16.0.5.1310 (EX01)`, Android 16, realme UI 7.0, hardware revision `RMX5566_11`, kernel `5.15.197-android13-8-o-01074-gdfa4acfa411e`, Snapdragon 685, 8 GB physical RAM plus 16 GB expansion and 256 GB storage. The full inventory and safe next diagnostic steps are recorded in `device/RMX5566RU_SOURCE_DISCOVERY_2026-11-27.md`.

## Release boundary

A successful Gradle build produces an Android application prototype, not an AOSP ROM. The repository source archive is not a full AOSP checkout, OTA package or flashable image. No device-specific boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA image is approved for creation or flashing at this stage.


## Packaging route decision — 2026-11-27

A concrete AOSP packaging route is documented in [AOSP_BRAIN_BRIDGE_PACKAGING_PLAN_2026-11-27.md](AOSP_BRAIN_BRIDGE_PACKAGING_PLAN_2026-11-27.md). The selected boundary is a reproducible Gradle release APK imported into the full AOSP product using `android_app_import`, then platform-signed by Soong. This is preferred over pretending the current minimal Java Soong launcher contains the Kotlin/Compose/LiteRT bridge. The import/signing behavior must be validated against the pinned AOSP branch, followed by package certificate inspection and emulator Binder tests. No full AOSP checkout is present yet, so this remains a documented implementation route, not a completed integration.

## Follow-up verification — 2026-11-27 (run 45912386792)

The newer launcher workflow is still running at the time of this update: [run 45912386792](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45912386792), commit `e7135773149790ecfe2e759844a4fa0999bb2294`. Its source assertions, Brain/core tests, glTF validation, Android launcher compilation and app unit tests have completed successfully. The unsigned release-APK build is currently in progress; source-bundle and artifact-upload steps have not yet completed. Do not describe the release APK as available until the workflow reports success and the artifact is present.

A fresh recursive repository-tree inspection found no tracked SELinux policy files or privileged-permission allowlist files (no `service_contexts`, `file_contexts`, `sepolicy` or `privapp-permissions` paths). This confirms blocker 4 is still open. No speculative allow rules have been added: the correct service type, domain, contexts and least-privilege rules must be derived and tested against the exact Android 16 AOSP policy checkout, not guessed from the OEM vendor policy version.

The staging workflow is a preparation tool only. It does not create an AOSP checkout, resolve product integration, generate platform signing keys, build a ROM, or authorize flashing.


## Local model integrity module — 2026-11-27

Implemented a dedicated `ModelArtifactIntegrity` verifier for the Gemma 4 E2B model catalog. It requires the exact published file length, recomputes SHA-256 from the complete file on every validation, and restricts the canonical file path to the app-specific download directory. The old size/mtime cache was removed because metadata equality is not cryptographic integrity evidence. Unit tests cover the trusted file, same-length content replacement with restored mtime, and a file outside the allowed directory. This is source-level implementation pending the current Gradle CI run and does not establish model performance on RMX5566RU.

## Release build investigation — 2026-11-27

The earlier release build did not fail during Kotlin compilation or APK packaging; it failed at `:app:lintVitalAnalyzeRelease`. The logged exception is an AndroidX Lifecycle custom lint detector crash (`NonNullableMutableLiveDataDetector`) with a Kotlin Analysis API binary incompatibility while analyzing `LocalModelRuntime.kt`. Lifecycle runtime dependencies were aligned from 2.8.7 to stable 2.11.1 to test whether the newer detector resolves the incompatibility. This is a targeted dependency correction, not a bypass: release lint remains enabled. The result is pending CI verification.


## Encrypted persistence recovery module — 2026-11-27

Added ManaEncryptedDataRecovery in the Android app layer. If the encrypted memory or Microworld store cannot be opened during startup, the runtime now attempts to preserve a byte-for-byte encrypted copy under the app-private filesDir/mana/recovery directory. The source file is never deleted, decrypted, replaced, or rewritten by this recovery path. Copies are uniquely named, bounded to 64 MiB, fsynced, and verified against the source with SHA-256 before being accepted. If preservation fails, startup still fails closed and the original remains untouched.

Unit tests cover unchanged source bytes, exact copied bytes/digest, distinct recovery copies, and rejection of invalid labels/missing sources. This is a preservation mechanism, not a decryption or restore UI: recovery still requires the original Keystore key to become available or an owner-directed recovery procedure. No automatic key replacement or silent empty-memory reset is allowed.

## Android build-toolchain correction — 2026-11-27

The Lifecycle 2.11.1 trial was rejected by CI before Kotlin compilation: its transitive Compose runtime artifact requires compileSdk 37 and AGP 9.1+, while this project is intentionally still on compileSdk 36 and AGP 8.7.3. The dependency set was moved to Lifecycle 2.10.0 as a compatibility candidate. The next CI run must confirm AAR metadata, lint, compilation, tests and release packaging; no release APK is claimed yet.


## MANA health and recovery diagnostics UI — 2026-11-27

Added a read-only in-app status screen, reachable from the MANA home screen, showing observed Engine state, system supervisor phase, last startup failure, local-model readiness, encrypted memory/Microworld availability, persistence file metadata, recovery-copy inventory, Android API/device identity and Keystore-provider availability. The screen can copy a plain-text diagnostic report for troubleshooting. It does not expose memory contents, secrets, key material, or personal data, and it does not modify stores or permissions.

The home Activity now has a minimal fallback screen when the Engine cannot initialize or bind, so the owner can still open diagnostics and retry the connection rather than being left with a blank screen. This reports app-level runtime state only; it is not proof that the MANA AOSP platform service or a complete MANA OS firmware is installed.

## Source snapshot pin refresh — 2026-11-27

The AOSP local manifest now pins MANA source snapshot `4f514a53be14ab4d5ebc5cc411322138ca7f4d35`, which includes the service resilience work, Android lifecycle safeguards, and firmware research documentation. The manifest update itself is a later commit; keeping the referenced snapshot immutable avoids a self-referential moving branch. CI run 45914434084 is queued to revalidate the updated pin and app source.

## Release lint toolchain investigation — 2026-11-27

The first run after the lifecycle compile fix passed the Brain/core tests, Android debug assembly and app unit tests. The unsigned release build then failed in `:app:lintVitalAnalyzeRelease`, not in Kotlin compilation: the bundled `androidx.lifecycle.lint.NonNullableMutableLiveDataDetector` crashed with `IncompatibleClassChangeError` / `NegativeArraySizeException` while analyzing `LocalModelRuntime.kt`. Lint has not been disabled.

The build toolchain has been advanced from AGP 8.7.3 / Gradle 8.10.2 to AGP 8.13.2 / Gradle 8.13. Android Developers lists AGP 8.13 as supporting API 36.1 and requiring Gradle 8.13, which aligns the project with its compileSdk 36 target. This is a compatibility correction candidate; release packaging remains unverified until the new CI run completes. References: https://developer.android.com/build/releases/agp-8-13-0-release-notes and https://developer.android.com/build/releases/about-agp.

The AOSP local manifest is now pinned to immutable source snapshot `635e36e9eb0e99674ce8c0069b10da6461ce4a5d`, including the current app lifecycle changes and upgraded build toolchain.

The local manifest pin was subsequently advanced to `0b73650c98521769fda4d7e220b932b5012a026d`, which includes the merged Activity resume/pause handling for the Engine reconnect controller. CI now also asserts that `MainActivity` has exactly one `onResume`, `onPause` and `onDestroy` override to prevent duplicate lifecycle callbacks.
