# MANA Brain ↔ MANA OS control contract

Status: implementation contract plus first platform-neutral registry implementation. This is not proof of a bootable ROM or device-level control.

## Ownership model

- **MANA Brain** is the single decision-making intelligence: local-first inference, memory, planning, reasoning and selection of an intended capability.
- **MANA OS** is the operating system and trusted execution environment: it exposes a finite registry of supported capabilities, verifies authorization and invokes the matching Android platform adapter.
- **The phone** is the hardware body. Android framework services, OEM HALs and device firmware provide the actual hardware interfaces.
- **Applications** are tools and workloads. MANA can discover or launch them only through supported Android package/intent APIs and granted roles.

The Brain does not directly own Android's security kernel, SELinux, Verified Boot, Package Manager or Google service internals. It requests actions from MANA OS; MANA OS executes only actions the platform has registered and authorized.

## First implementation in this change

`engine/mana-brain/.../ManaCapabilityRegistry.kt` provides a transport-neutral capability catalog and authorization decision function. It checks, in order:

1. The capability is registered.
2. The owner has enabled it.
3. The target device supports it.
4. All required Android permissions are currently granted.
5. The owner has confirmed the action when the capability is sensitive.

The initial catalog contains app launch, opening supported settings, notification access, contacts read, call answer and signed MANA update installation. Sensitive capabilities require an explicit owner confirmation. Unknown capabilities fail closed.

The registry is intentionally not an Android permission grantor and does not execute shell commands, inject input, bypass Android prompts, or claim a permission has been granted. The Android adapter must authenticate the Binder caller, query current PackageManager/AppOps/RoleManager state, perform the exact public or platform API, and return the actual result.

## Consent model

A single first-run MANA consent screen may explain the complete set of requested capabilities and let the owner opt into groups. Android still owns runtime permission prompts, role requests, notification-listener access, overlay access and other special-access flows. MANA must launch the official system consent UI and verify the resulting state; one MANA checkbox cannot grant permissions Android reserves for the user or system.

## Platform integration sequence

1. Keep the registry in the canonical `mana-brain-core` source set shared by JVM tests and Soong.
2. Add a platform adapter behind the protected Binder service. It maps capability IDs to allowlisted Android APIs and checks the real caller UID/package/signature.
3. Add a durable audit record for each request: capability ID, requesting MANA component, decision, timestamp and result; do not log prompts, contacts, notification content or secrets by default.
4. Add Android permission declarations, privapp allowlists and SELinux rules only for features that actually need them and only after their platform modules exist.
5. Integrate MANA-owned SystemUI/Settings/Launcher as AOSP platform modules. Until then, do not describe an ordinary APK or overlay as a replacement for Realme SystemUI.
6. Integrate the exact RMX5566RU product/device configuration and validated OEM hardware support before producing images or OTA packages.

## Device and Google compatibility

The product target is realme C85 4G RMX5566RU / RE645F. Retain Android 16 framework/API and ART compatibility. Keep Google Mobile Services only through authorized compatible distribution and validate each Google function independently. Retain the exact-device OEM kernel, vendor/ODM HALs and firmware needed by hardware until a proven replacement exists.

## Verification gates

- JVM tests for registry allow/deny behavior.
- Soong compilation of the canonical Brain module.
- Binder caller-authentication and platform-adapter tests.
- Android instrumentation tests on the target build.
- Exact-device tests for permissions, settings, telephony, notifications and update/rollback.
- Full image build and first boot are separate gates; no ROM/OTA is complete before all release gates pass.


## Binder adapter progress (2026-11-26)

The protected `IBrainService` now exposes `executeCapability` using structured AIDL request/response parcelables. Its Android adapter rechecks current permission grants and relevant special-access state before asking the shared registry for a decision. It currently executes only app launch and allowlisted Settings navigation through public Android APIs.

The request's `ownerConfirmed` boolean is intentionally ignored. Sensitive actions remain disabled until consent can be obtained and verified through a trusted owner-facing system UI flow. No arbitrary shell, root, accessibility injection, or hidden API execution is exposed.

The JVM capability registry compiles and passed six local smoke assertions. Android Gradle/AIDL compilation and instrumentation tests are authored but have not yet been executed; target-device and Soong verification remain outstanding.
