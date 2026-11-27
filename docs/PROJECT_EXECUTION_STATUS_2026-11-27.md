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


## Binder callback delivery hardening — 2026-11-27

- Added a per-request atomic terminal-callback gate in `ManaBrainService`. The platform Binder endpoint now accepts only the first Engine Bridge result or error; duplicate result/error callbacks are ignored and logged. A synchronous Binder exception can deliver a fallback error only if no terminal callback has already won the race.
- Added source-contract assertions for the atomic gate and both duplicate-callback guards. The latest source-contract workflow run `45918556057` on commit `a86e50959064c7f7850a94294d88edad675623b3` completed successfully.
- The Gradle Android app verification run `45918549370` was still running at the last status check; no new Gradle result is claimed here. The callback change is in the platform Java service and does not constitute a real Soong compile or device Binder test.
- The actual Android 16 AOSP/Soong build, platform certificate comparison, SELinux integration, emulator instrumentation, and RMX5566 firmware build/flash remain unexecuted. No device partitions have been modified.


## MANA Brain provider routing and local-failure fallback — 2026-11-27

- Normalized provider IDs at the MANA Brain routing boundary. Gateway IDs such as `cloud-gemini` now match the official provider aliases `google` / `gemini`; `cloud-grok` maps to `xai` / `grok`. Returned provider metadata uses canonical IDs, and alias-equivalent duplicate connectors are rejected.
- Added regression tests for gateway-prefix normalization, Google/Gemini aliases and duplicate aliases.
- MANA Brain now may fall back to an available cloud provider after local inference throws or returns an empty response only when the request explicitly permits cloud use and is not sensitive. Cancellation is rethrown, and local failure never triggers cloud use without consent or for sensitive requests. Tests cover consent, sensitive-request blocking, and blank local output.
- JVM verification run `45919199972` for commit `5f84e7da` completed successfully, including the new MANA Brain tests. The broader Android launcher Gradle verification run `45919199900` was still running at the last status check; no result is claimed yet.

## One-shot Binder callback gate — 2026-11-27

- Extracted the atomic first-terminal-callback gate into `ManaBrainTerminalCallbackGate` and wired it into the platform `ManaBrainService` result, error and synchronous-failure paths.
- Added host JUnit coverage for sequential and 64-way concurrent callback attempts and included the class/tests in `mana-brain-request-policy-tests`.
- The source-contract workflow and actual Soong host-test execution for this new helper must still be confirmed. No AOSP Soong compile or device Binder test is claimed.


## Bounded inference and request-scoped native cancellation — 2026-11-27

- MANA Brain now enforces a 90-second default local inference deadline and a 60-second default cloud inference deadline. Both are constructor-configurable and must be positive. Local timeout may fall back only under the existing explicit cloud-consent and non-sensitive-request rules; cloud timeout returns a distinct `InferenceTimeoutException` and does not silently switch providers.
- The first timeout CI attempt exposed a missing `withTimeoutOrNull` import; this was corrected in commit `7479fba6001764749943d513e91aebd45e1834ff`. The initial failed run is `45919568435`; it is not counted as a passing verification.
- Added per-request cancellation handles to the local inference interface. Each handle is bound to the exact queued LiteRT generation job, so a timed-out queued request cannot cancel another request currently running on the single-thread executor. Shutdown cancels all active generation jobs. Added unit tests for cancellation isolation and cancellation-before-start.
- The final Gradle verification for these latest changes is pending at the time this note is written; no compile/test success is claimed until that run completes. No physical-device LiteRT cancellation test has been performed.


## Verified Gradle release and pinned AOSP source snapshot — 2026-11-27

- Gradle workflow run `45921372734` (run 937), commit `18b84fcb65f09b12975db4e5bd20d88aaddac29f`, completed with overall conclusion `success`. All Brain/Core tests, Android debug assembly and unit tests, unsigned release APK build, manifest/package checks, source archive packaging and artifact uploads passed.
- Published artifacts:
  - Source project ZIP: `MANA-Genesis-4.0-source-project`, artifact `15164820555`, 17,491,491 bytes, SHA-256 `2ef3851b5695950cf5aabcaa7ed2b7a07f79ce4882fafa5963d7bbc294b168b0`, expires 2026-12-27.
  - Debug APK prototype ZIP: `MANA-Android-debug-prototype-NOT-ROM`, artifact `15164646736`, 49,716,089 bytes, SHA-256 `6f817fa10213e5899ab28a0b371939901d574ba55c1c422a5219751a82dde271`, expires 2026-12-11.
  - Unsigned release APK ZIP: `MANA-Android-release-unsigned-for-AOSP-platform-signing`, artifact `15164646725`, 45,819,498 bytes, SHA-256 `da5f3bba523d5e6c5d723130cefbdb4175a19b3bf498551e9a6cacd4c7b58219`, expires 2026-12-11.
- These SHA-256 values are GitHub artifact archive digests, not the inner APK checksum. The release APK is unsigned and intended for later AOSP platform signing. The debug artifact is a launcher prototype; neither artifact is a ROM, OTA or installable firmware.
- Advanced `platform/local_manifests/mana.xml` to immutable MANA source snapshot `18b84fcb65f09b12975db4e5bd20d88aaddac29f` in commit `5363e087acd4dd2cfebad99786a01ccab66ed5ae`. The pin intentionally references the previously tested source snapshot, not the manifest-update commit itself.
- Call-screening source review: `ManaCallScreeningService` responds permissively to Telecom before scheduling bounded auxiliary contact lookup; it never answers, rejects or records calls. The queue is single-worker and bounded to 16. Contact lookup is local-only and only occurs with READ_CONTACTS permission. Android's screening API does not provide the requested 10-second delay; this is explicitly documented rather than simulated.
- Remaining gates: verify the manifest-update commit with source-contract CI; inspect actual APK contents/signing state if needed; build the platform modules in a real Android 16 AOSP checkout; add SELinux service policy; run Binder instrumentation on emulator/device. Device-specific tree, matching OEM kernel/vendor sources, VINTF/AVB and stock restore remain blockers. No firmware or phone flash is claimed.


## Successful Android app release verification — run 939 — 2026-11-27

- Workflow run `45921862340`, commit `df00b28b34343edc63a214254c0e98213ae6650d`, completed with overall conclusion `success` at 2026-11-27 01:23 UTC. All architecture/source-contract checks, MANA Brain/Core tests, Android debug assembly and app unit tests, unsigned release APK build, source ZIP packaging and all artifact uploads passed.
- Published artifacts:
  - Source project ZIP: `MANA-Genesis-4.0-source-project`, artifact `15165071130`, 17,492,820 bytes, SHA-256 `22207b26bdaed8a7cc1ea85f9211bd87c3a99f6465b029d2316a8857cad93407`, expires 2026-12-27.
  - Debug APK prototype ZIP: `MANA-Android-debug-prototype-NOT-ROM`, artifact `15165650185`, 49,717,503 bytes, SHA-256 `7de011d9adb6d720b0e4cdce822e55c3f0458e5b238e4856e1424c178930c4ee`, expires 2026-12-11.
  - Unsigned release APK ZIP: `MANA-Android-release-unsigned-for-AOSP-platform-signing`, artifact `15165695174`, 45,819,551 bytes, SHA-256 `bb0674981281c5201ca7cff89224e7b235d05375937e5d424249b0671e5c73e8`, expires 2026-12-11.
- Artifact digests above are GitHub ZIP digests, not checksums of the APK files inside. The release APK is explicitly unsigned and intended for later AOSP platform signing. The debug APK is an application prototype. None is a ROM, OTA or device-flashable image.
- Android launcher compilation, app unit tests, Brain/Core tests and all source-contract checks passed. This does not establish a real Soong/AOSP build, platform certificate equality, SELinux integration, Binder runtime handshake, or device operation.
- The call-screening role opt-in change is included in this verified commit. Screening and default-dialer roles remain separate explicit user approvals. The current call UI warns that PSTN two-way voice interaction is not implemented; do not describe the receptionist as capable of conversing with callers.
- Next source snapshot promotion: update the AOSP local manifest to pin this tested commit. After that manifest-only commit, run its contract CI. Continue with a real AOSP build environment, SELinux service policy and emulator Binder integration; keep RMX5566 firmware blocked pending exact device tree, kernel, vendor blobs, VINTF/AVB and matching stock restore.


## Android release verification and Binder test identity correction — 2026-11-27

- Android workflow run `45922765453` for commit `5f58e94addbafa452581fc8caf2950e3aadf0949` completed successfully. All static architecture checks, Brain/Core host tests, Android launcher compilation, app unit tests, unsigned release build and artifact uploads passed.
- Artifacts from this run:
  - Source project ZIP: `MANA-Genesis-4.0-source-project`, artifact `15164644226`, 17,493,937 bytes.
  - Debug APK prototype ZIP: `MANA-Android-debug-prototype-NOT-ROM`, artifact `15164977813`, 49,717,831 bytes.
  - Unsigned release APK ZIP: `MANA-Android-release-unsigned-for-AOSP-platform-signing`, artifact `15165272348`, 45,819,562 bytes.
- Artifact sizes identify GitHub ZIPs, not the APK files inside. These remain application/source artifacts and are not bootable firmware, recovery packages or OTA images.
- Corrected the device-side Binder instrumentation identity:
  - `android:targetPackage` now names `com.mana.system.brain`, the actual platform service app.
  - Permission checks and `bindService` now use `getInstrumentation().getContext()`, the test APK's own caller identity, rather than the target service app context. This prevents a false-positive permission check caused by testing with the service application's identity.
  - Extended `tools/test_mana_soong_integration.sh` to assert both requirements.
- The updated source-contract workflow `45923288853` for commit `5d5d3a26169c06bf4d37c1ccf2d55a4caed48c35` completed successfully. This confirms the manifest/context source contract only; it does not compile the instrumentation APK or prove a live Binder transaction.
- Gradle verification for the Java test-source correction is running separately as `45923286007`. No result is claimed until it completes.
- Next: build `ManaBrainBinderIntegrationTests` and the platform modules in a provisioned Android 16 AOSP checkout; verify the platform signer and run `atest ManaBrainBinderIntegrationTests` on an emulator/product image. This workspace has not yet demonstrated a full AOSP checkout or a verified RMX5566 lunch target, so the real Soong and Binder execution remain open.
- Firmware remains the project end goal. Device-specific ROM construction still depends on exact RMX5566RU/RE645F sources, matching OEM kernel/device configuration/vendor blobs, VINTF/AVB and partition metadata, plus a verified matching stock restore path. Do not mark any of those prerequisites complete by inference.


## Owner Termux audit v3 reviewed — 2026-11-27

- Reviewed the owner-provided archive `mana_rmx5566_audit_20261127_043744.zip` (read-only collector v3).
- Confirmed exact build fingerprint `realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys`, vendor fingerprint match, API 36, vendor API/VNDK 33, security patch 2026-10-01, kernel release `5.15.197-android13-8-o-01074-gdfa4acfa411e`, locked/green AVB state, active slot `_a`, dynamic partitions and file-based encryption.
- Archive includes readable vendor/ODM/system/system_ext VINTF XML and a vendor_dlkm module filename inventory. This is stronger evidence for interface discovery, not proof of framework VINTF compatibility or kernel-module ABI compatibility.
- Android access controls prevented reading `/sys/class/block`, partition sizes, SELinux status/policy version, `/proc/config.gz`, kernel headers and device-tree metadata. These remain unknown; no layout is inferred.
- Updated `device/RMX5566RU_SOURCE_DISCOVERY_2026-11-27.md` with the exact audit findings and limitations.
- Gradle workflow run `45923286007`, commit `00e04ca7f1f32d0a888178d1c5877c3deb27ae73`, completed successfully. Brain/Core host tests, Android launcher/test-app compilation, unsigned release build and all three artifact uploads succeeded. Artifacts: debug prototype ZIP `15165463406` (49,717,832 bytes), source project ZIP `15165178460` (17,494,005 bytes), unsigned release APK ZIP `15165158304` (45,819,562 bytes). They are not ROM/OTA images.
- The real AOSP/Soong build, platform-signature verification against an actual product certificate, SELinux policy integration, Binder instrumentation on an emulator/device and RMX5566 boot testing remain unexecuted.
