# MANA Genesis 4.0 — Adaptive Device Discovery and Firmware Engineering

Status: required architecture and implementation specification.

## Product principle

MANA must not treat one phone model as the only possible target and must not stop at a manually maintained device profile. MANA Brain coordinates an adaptive device-engineering pipeline: inspect the actual device, build a verified capability and compatibility profile, discover matching software components, adapt build configuration, compile and test, and only then prepare a device-specific firmware candidate.

Self-development means MANA can improve its own tooling, compatibility database, build recipes, device support and userspace modules through a controlled research/development/test/release cycle. It does not mean inventing proprietary binaries, kernel source, signing keys or hardware interfaces that are not available.

## Required pipeline

1. **Read-only device discovery**
   - Collect model/product/device identifiers, manufacturer, SoC/platform, board, ABI, Android/API and build fingerprint.
   - Inspect kernel release/config exposure, boot properties, VINTF manifests/matrices, HAL declarations, package/features, memory, storage, display, sensors, radios, partitions and A/B/dynamic-partition metadata where Android permissions expose them.
   - Record source, timestamp, confidence, access limitations and hashes for every observation.
   - Never claim that inaccessible partition contents or protected hardware data were scanned.

2. **Capability and compatibility registry**
   - Normalize observations into a versioned device profile.
   - Distinguish confirmed, inferred, unavailable and conflicting facts.
   - Match components by exact device/product, SoC, board, kernel ABI/version, Android release, vendor interface and partition layout—not by marketing name alone.
   - Maintain a compatibility graph for kernels, device trees, vendor blobs, HALs, firmware, recovery, signing and build tools.

3. **Component discovery and provenance**
   - Search configured official OEM sources, AOSP/common-kernel sources, trusted upstream projects and explicitly approved mirrors.
   - Verify repository identity, target identifiers, release/commit, licenses, signatures/checksums and provenance.
   - Reject look-alike device trees or kernels with mismatched SoC, board, kernel version, ABI, DTS, module symbols or vendor interface.
   - Never silently substitute another model's proprietary blobs, firmware, kernel, recovery or partition map.
   - If a required component is unavailable, create a precise acquisition task and continue independent userspace work.

4. **Adaptive configuration and integration**
   - Generate candidate product/device configuration from verified profile data and compatible sources.
   - Select only modules and HAL integrations supported by evidence.
   - Resolve dependencies, adapt build flags and module variants, and record every generated change.
   - Keep generic Android application/runtime builds separate from AOSP platform builds and device firmware builds.

5. **Build, test and repair loop**
   - Run static checks, dependency/license checks, unit tests, API-level emulator tests, Soong/Gradle builds and device-specific compatibility checks.
   - Diagnose failures, propose or implement bounded fixes, rerun the failing test and regression suite.
   - Track test evidence by exact commit, toolchain, target profile and artifact hash.
   - Do not mark a stage passed merely because a prior stage compiled.

6. **Firmware release gate**
   - A device-specific image/OTA candidate may be produced only when exact device tree, kernel source/config/DTS, matching vendor blobs and firmware, VINTF, partition metadata, AVB/signing path and a tested stock-restoration route are verified.
   - Validate boot-chain, rollback, encryption, SELinux, AVB and A/B update requirements.
   - First produce a non-flash validation candidate and reports. Flashing requires explicit owner approval and a documented recovery path.
   - Never unlock, erase, repartition or flash the owner's phone automatically.

7. **Continuous learning and safe self-update**
   - Store verified findings in a local knowledge base with source provenance and expiry/revalidation rules.
   - MANA may autonomously research, profile, compare, compile in an isolated workspace and run tests.
   - Changes to its own production code/configuration must pass tests and be staged as a reviewable update with rollback.
   - Payments, credentials, destructive operations, bootloader changes and installation/flash actions require explicit owner confirmation.

## Required MANA modules

- Device Discovery Agent: read-only Android and hardware inspection.
- Device Profile Registry: normalized facts, confidence, provenance and capability flags.
- Compatibility Resolver: exact matching across SoC, board, kernel, vendor, HAL, Android and partition metadata.
- Source and Component Scout: official/upstream discovery, license and provenance verification.
- Adaptive Build Planner: generates reproducible product configurations and dependency graphs.
- Build/Test/Repair Orchestrator: isolated builds, tests, failure diagnosis and bounded repair cycles.
- Firmware Readiness Gate: machine-readable pass/fail/blocked decisions with evidence.
- Knowledge and Update Manager: verified learning, staged updates and rollback.

## Required output for every target phone

MANA must generate:
- device profile JSON and human-readable audit;
- supported/unsupported capability matrix;
- matched components with exact source URLs, commits, versions, hashes and licenses;
- missing-component list with reason and acquisition route;
- generated configuration diff;
- build and test reports;
- firmware readiness decision: READY, NOT_READY or BLOCKED, with evidence;
- artifact manifest and rollback/recovery instructions when a release candidate exists.

## RMX5566RU initial application

Use the owner's verified runtime identity as the initial target: RMX5566RU / RE645F, Qualcomm SM6225 (bengal), ARM64, Android 16 / API 36, kernel 5.15.197. Do not select RMX5253 / MediaTek C85 5G sources. Existing missing kernel/device-tree/vendor/recovery inputs become discovery tasks for the Component Scout and Compatibility Resolver—not a reason to abandon adaptive development and not permission to fabricate or substitute them.

## Acceptance criteria

The feature is complete only when MANA can:
1. profile a supported Android phone without root and clearly report inaccessible data;
2. distinguish two devices with conflicting model names using runtime identifiers and SoC/board evidence;
3. find and validate exact-match components or explain why none are available;
4. generate a reproducible candidate configuration from verified inputs;
5. build and test in isolation, retain logs and hashes, and repair confirmed software failures;
6. block firmware packaging when any mandatory device-specific gate is unresolved;
7. repeat the process for a newly encountered phone without hard-coded model-specific assumptions;
8. request owner approval for consequential installation or flashing actions.

A successful generic APK or AOSP module build is not evidence of a bootable device firmware.
