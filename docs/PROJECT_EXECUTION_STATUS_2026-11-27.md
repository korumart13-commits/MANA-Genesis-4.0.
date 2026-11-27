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
- Verified artifacts from run `45914698793`: source project ZIP `15161902217` (17,462,443 bytes; expires 2026-12-27); debug prototype ZIP `15161708101` (49,707,561 bytes; expires 2026-12-11); unsigned release APK ZIP `15161513522` (45,822,738 bytes; expires 2026-12-11). These are GitHub Actions artifacts, not ROM images. The release artifact is unsigned and still requires AOSP platform signing/product integration. The downloaded ZIP SHA-256 is `02f4f22c84e245b409aa4ba233d439d73e7b4bd86c14d17685f69fa5476da396`; contained APK SHA-256 is `b669e656345d8986098529fddc277dc8109eccb2ed5d279bc0741e1829ff862f` (85,405,030 bytes). `jarsigner -verify` reports `jar is unsigned`. ZIP inspection confirms an Android manifest, three DEX files, and ARM64 LiteRT-LM JNI ELF (`EM_AARCH64`); this archive does not prove installation or runtime inference on RMX5566.
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


## AOSP app import and platform-signature integration preparation

- Updated `tools/stage_mana_aosp_prebuilt.sh` to stage the unsigned Gradle APK as a Soong
  `android_app_import` module named `ManaLauncher`, with `presigned: false` and
  `certificate: "platform"`. It requires `apksigner` to confirm the input is unsigned, validates package `com.mana.launcher`, the engine bridge
  service and the signature permission, then copies the APK into the AOSP source tree.
- The imported launcher and `ManaBrainService` are now product-specific, non-privileged
  applications. Both declare `certificate: "platform"`; they can share the custom
  signature-level `com.mana.permission.BIND_BRAIN` without granting either package
  unnecessary privileged-app status.
- Added `tools/verify_mana_platform_signature.sh`. After a real AOSP build it verifies APK
  signature validity, package identity and exact SHA-256 certificate equality with the
  product's platform certificate. Production private signing keys must remain outside Git.
- CI now checks the import/signing declarations and signature-verifier script. The first
  CI attempts exposed a stale assertion expecting `privileged: true`; that assertion was
  corrected. The app workflow `45916033709` for the single-launcher product snapshot has passed the Brain policy, platform Binder boundary, AOSP staging/signature-script static checks, RMX5566 profile checks and Brain/Core tests; Android app compilation is still in progress. Newer runs for the unsigned-input guard and final manifest snapshot are also in progress. No Soong build or signed APK certificate comparison has been executed.

- Product packaging now includes one central `ManaLauncher` app plus the separate `ManaBrainService`; the legacy Java-only `ManaAssistant` stub is excluded from `PRODUCT_PACKAGES` to avoid a second assistant entry point. `ManaSettings` and `ManaControlCenter` remain separate OS configuration surfaces.
- Source-level Binder route: platform-signed `ManaBrainService` declares the signature
  permission and binds explicitly to `com.mana.launcher/.ManaEngineBridgeService`;
  the imported launcher requests that permission, protects its bridge with it, repeats
  caller checks, and delegates to the single `ManaEngineProcessOwner`.
- Still not completed: a real Android 16 AOSP/Soong build, final signed-APK certificate
  comparison, SELinux service context/domain and Binder policy, emulator Binder tests,
  product-image boot, and RMX5566 device tests. No flashable firmware is produced by this
  integration-preparation step.


## Binder integration test and Soong execution handoff — 2026-11-27

- Extended the canonical device-side instrumentation test module:
  `system/mana/tests/ManaBrainBinderIntegration`.
  It binds to the exported platform `ManaBrainService`, verifies the signature permission
  and AIDL protocol, waits for the app-owned `ManaEngineBridgeService` backend to connect,
  then sends a local-only request and requires a backend callback. A disconnected backend
  is a test failure; a truthful model-not-ready/inference diagnostic is acceptable.
- The existing Soong module `ManaBrainBinderIntegrationTests` now also checks the signature permission,
  protocol handshake, backend connection and asynchronous local-only request callback. The existing
  guarded runner is `tools/run_mana_soong_integration.sh`. The runner builds
  `mana-brain-interface`, `ManaBrainService`, staged `ManaLauncher` and the integration
  test using `m`, then optionally runs `atest` against a connected image where both MANA
  services are already installed. It contains no flashing, unlock, wipe or partition-write
  operation.
- Added CI static checks and shell syntax validation for the instrumentation module/runner.
  These checks prove the test contract is present, not that Soong compiled it or that Binder
  worked on a device.
- The earlier Soong/Binder contract run `45916543920` failed because its workflow expected the test to reference a constant rather than the explicit action string; the assertion was corrected. Contract runs `45916526548` and `45916548420` completed successfully, while the latest post-fix run is to be checked separately. Launcher/source bundle runs continue independently.
- No full AOSP Android 16 checkout or verified lunch target is available in this repository
  workspace. Therefore the actual Soong module build, APK platform-certificate equality
  check, SELinux integration, Tradefed Binder run and product boot remain unexecuted.
  The next real build must run on a provisioned AOSP build host using the pinned local
  manifest and a verified product configuration.
- Device gate unchanged: no RMX5566 firmware build/flash until exact matching stock restore,
  device tree, OEM kernel sources, vendor blobs, VINTF, partition map and AVB chain are
  independently verified.


## Latest verification snapshot — 2026-11-27

- Confirmed successful Gradle app pipeline: run `45916217043` completed with no failed job
  steps and uploaded the unsigned release APK (artifact `15161949345`), debug prototype
  (artifact `15162029283`) and source bundle (artifact `15161959377`). This confirms the
  Android app release build, not a platform-signed APK or ROM.
- Confirmed successful Soong/Binder source-contract runs `45916643525` and
  `45916693175`. The latter checks the canonical instrumentation test after it was extended
  to send a local-only request and require an Engine Bridge callback. The stale workflow
  assertion from failed run `45916543920` was corrected.
- The latest Gradle run observed during this check, `45916745681`, was still in progress at
  the Android SDK setup step. Earlier superseded runs were cancelled by concurrency; they are
  not counted as successful or failed source validation.
- AOSP's upstream `android_app_import` implementation documents that an imported APK must
  select exactly one signing mode: certificate, presigned, or default development certificate.
  MANA uses unsigned APK + `certificate: "platform"`; the real product build must still
  prove both MANA APKs carry the same final signer certificate.
- The Soong runner and device test are ready in source, but no AOSP checkout/lunch target is
  available here. No actual Soong compile, final APK signature comparison, SELinux integration,
  Tradefed run or device Binder handshake is claimed.
