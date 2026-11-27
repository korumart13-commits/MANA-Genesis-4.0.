# MANA Genesis 4.0 — Universal Device Adaptation Architecture

**Status:** Architecture requirement / implementation work required  
**Date:** 2026-11-27  
**Primary principle:** MANA Genesis is one adaptive product, not a firmware project hard-wired to one phone model.

## 1. Product direction

The original Realme RMX5566/RMX5566RU device remains the first reference device for compatibility testing. It is no longer the only supported target. MANA must be designed to install and run on as broad a range of Android devices as platform APIs, hardware capabilities, vendor restrictions, and security policy permit. A later iOS companion may share account, cloud, and Brain protocols, but must use Apple-approved capabilities rather than pretending Android-level system access exists on iOS.

This changes the product direction from “build one custom ROM for one phone first” to “build one adaptive MANA platform with device-specific capability adapters.” A custom ROM remains an optional, separately gated integration for devices where exact OEM sources and safe recovery are available.

## 2. First-run device discovery

On first launch, the MANA Device Profiler must inspect only information exposed through documented APIs or explicitly granted permissions. It creates a versioned device profile containing, where accessible:

- manufacturer, model, device/product identifiers, Android version, API level and build fingerprint;
- CPU architecture and supported instruction sets where available;
- RAM class, available storage, display size/density and relevant graphics capabilities;
- battery/thermal and power-management signals exposed by the OS;
- available sensors, camera/audio capabilities and connectivity;
- supported Android features, permissions, roles, services and background-execution restrictions;
- local inference feasibility: runtime compatibility, model format support, memory budget and storage budget;
- vendor-specific restrictions and known compatibility workarounds, each with source and confidence.

Every field has an explicit state: DETECTED, NOT_ACCESSIBLE, UNKNOWN, UNSUPPORTED, or MISMATCH. Never guess a device property or infer privileged access from a model name alone.

## 3. Capability registry and adaptive feature selection

MANA Brain must not assume every phone supports every feature. Each module declares:

- required capabilities and permissions;
- optional capabilities;
- minimum OS/API and hardware constraints;
- supported implementation adapters;
- resource budget (RAM, storage, CPU/GPU, battery and network);
- privacy and security classification;
- fallback behavior and user-visible limitations.

At runtime, the capability registry selects the best compatible adapter. Features are exposed as AVAILABLE, AVAILABLE_WITH_LIMITATIONS, NEEDS_PERMISSION, NEEDS_COMPONENT, UNSUPPORTED, or ERROR. Unsupported functions are disabled gracefully, not represented by nonfunctional buttons.

Examples:
- Local AI: choose a model/runtime only after checking ABI, runtime, memory, storage and thermal limits; otherwise use a smaller compatible model or an explicitly enabled cloud provider.
- Voice: choose supported speech recognition and TTS engines; report if offline recognition is unavailable.
- Avatar: scale rendering quality and animation complexity to GPU capability and thermal state; provide a lightweight 2D/static fallback.
- Background tasks: adapt schedules to Android background limits, battery state and manufacturer restrictions; never promise guaranteed 24/7 execution on a phone.
- Contacts, calendar, files, microphone, camera and location: activate only after the corresponding permission or official account authorization.
- System controls: use public Android APIs where available. Mark protected/privileged actions unsupported unless MANA is legitimately integrated as a signed system component for that exact build.

## 4. Self-completion without fabricated components

“Build missing parts itself” means MANA can:
1. detect a missing compatible component;
2. search approved sources or the configured official provider/catalog;
3. verify origin, signature/hash, version, compatibility and license;
4. propose or download a compatible optional module into an isolated app-managed area;
5. test it in a sandbox or test environment;
6. request user approval where installation, permissions, data access, cost or system changes are consequential;
7. enable it only after validation, and keep rollback to the previous known-good version.

MANA must never invent proprietary drivers, vendor blobs, firmware, kernel modules, signing keys, hardware features or permissions. If a required component is unavailable, it reports the exact missing dependency and provides a safe reduced-capability mode. It must not root/unlock a device, bypass Verified Boot, modify protected partitions or flash firmware automatically.

## 5. One MANA Brain, device-specific adapters

Keep one central MANA Brain and stable module contracts. Device-specific differences belong behind adapters, not in duplicated product forks:

- DeviceProfiler — discovers device facts and capability evidence.
- CapabilityRegistry — resolves supported features and requirements.
- PlatformAdapter — wraps Android APIs and manufacturer-specific behavior.
- InferenceAdapter — selects local model/runtime or authorized cloud provider.
- PermissionBroker — explains, requests and verifies runtime permissions/roles.
- ResourceGovernor — budgets memory, CPU/GPU, battery, storage and thermal load.
- ModuleResolver — verifies and stages compatible optional modules.
- HealthMonitor — checks module health, crashes, connectivity and degraded states.
- CompatibilityReport — explains what works, what needs permission, what is limited and what is unsupported.

All adapters report through the same MANA Brain interface and audit trail. Modules must not bypass the permission broker or call arbitrary privileged actions.

## 6. User experience requirements

The first-run wizard should be short and understandable:
1. Detect device and OS.
2. Show a plain-language compatibility summary.
3. Enable available core functions.
4. Ask for optional permissions only when the user enables the corresponding feature.
5. Offer compatible local AI choices only after resource checks; explain cloud alternatives and costs before use.
6. Show a final “MANA is ready” screen listing working, limited and unavailable features.

Provide a persistent device status page with clear labels, a reason for each limitation, and a “recheck compatibility” action. Do not overwhelm the user with raw engineering data; keep detailed diagnostics in an expandable report. Accessibility, localization, large text, one-handed use and low-resource mode are required.

## 7. Compatibility tiers

- Tier A — App-compatible: MANA app and public Android APIs; no root or custom ROM.
- Tier B — Extended integration: optional roles, accessibility/notification/foreground services and approved companion components, each explicitly enabled by the owner.
- Tier C — System integration: signed platform/system components built for a verified device/firmware combination; requires exact device sources, AVB/SELinux/VINTF validation and recovery/rollback.
- Tier D — Unsupported: required APIs, hardware, dependencies or safe installation path are absent.

The UI must show the tier and must not describe Tier A/B as a full operating system replacement.

## 8. Verification matrix

For each supported device family and OS version, test:

- clean install, upgrade, uninstall and data migration;
- device profiling accuracy and unknown/inaccessible fields;
- permission granted, denied, revoked and one-time access paths;
- feature selection, adapter fallback and unsupported-feature behavior;
- offline mode, reconnect, provider outage and invalid credentials;
- low RAM/storage, low battery, thermal throttling and background process death;
- model download integrity, signature/hash mismatch, cancellation and rollback;
- module crash isolation, restart and audit reporting;
- accessibility, screen sizes, rotation, localization and TalkBack;
- privacy boundaries, exported components, network security and dependency/license scanning.

CI/emulator tests are necessary but do not replace testing on physical devices. Publish a compatibility matrix with exact models, OS builds, test date and pass/fail evidence.

## 9. Acceptance criteria

The universal-adaptation milestone is complete only when:
- MANA installs and starts on multiple Android device families, not only RMX5566;
- each device is profiled without false claims;
- the capability registry selects compatible implementations;
- unsupported functions fail gracefully and explain why;
- permissions can be denied or revoked without breaking unrelated modules;
- local/cloud AI selection respects resource, privacy and consent rules;
- the user can see a simple, accurate compatibility/readiness summary;
- device-specific ROM features remain gated until verified sources and recovery exist.

“Runs on any phone” is a product goal, not a claim of universal compatibility. Actual support is established model-by-model and OS-version-by-OS-version through the verification matrix.

## 10. Relationship to the existing Realme work

Keep RMX5566/RMX5566RU as the first reference profile and preserve existing work. Refactor Realme-specific assumptions into a device adapter and test profile. Do not delete the Realme compatibility work. The immediate MANA OS Design/Update APK can implement the profiler, capability report and adaptive app behavior without becoming a ROM. The long-term MANA Brain, optional Cloud Home, and custom-ROM roadmap remain intact.
