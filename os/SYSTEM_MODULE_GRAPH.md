# MANA OS 4.0 — system module graph

This document describes the next integration layer after the JVM-testable system supervisor.

## Implemented in this stage

- A typed catalog of 16 MANA/platform modules.
- Explicit dependency edges and deterministic topological startup order.
- Required versus optional module classification.
- Failure policy: required services abort or enter degraded mode; optional features are disabled without taking down the core.
- Permission/data boundaries recorded for each module.

The catalog is architecture metadata consumed by tests and later integration work. It is **not yet** a set of Android Binder services, privileged APKs, or an installable OS image.

## Service graph

1. `platform.hardware` → `platform.security` → `platform.storage`
2. `mana.modelmanager` → `mana.brain`
3. `mana.brain` → avatar, diary, life manager, contacts, game intelligence
4. `mana.systemui` → launcher, settings, control center
5. contacts + brain → receptionist
6. security + storage → updater

## Security constraints

- Local inference remains the default. Cloud processing is opt-in and must not receive sensitive requests.
- Model import must validate file integrity and license metadata before activation.
- Contacts, phone, microphone and camera access remain permission-gated.
- Update installation requires signature verification and a tested rollback path.
- No root escalation, permissive SELinux policy, hard-coded API keys, or silent user-data deletion.

## Android integration gate

Before adding Android.bp, init rc, Binder interfaces or SELinux policy, the project must have a checked-out Android 16 AOSP tree and a verified target product/device configuration. AOSP init services are declared in partition-appropriate init rc files; service permissions and SELinux labels must be designed together. See the official [AOSP init documentation](https://android.googlesource.com/platform/system/core/+/android16-release/init/) and [SELinux build documentation](https://source.android.com/docs/security/features/selinux/build).

## Next work

Implement the first concrete platform adapter in a testable host module, then port the stable service contract to Android Binder once the AOSP tree and target integration are available.


## First concrete service contracts (JVM)

- `ManaSecurityService`: exact capability grants, default-deny decisions, expiry, revocation, explicit denial, and audit events.
- `ManaStorageService`: metadata inventory, conservative archive recommendations, explicit approval for reversible archive/quarantine, restore history, and no permanent deletion.
- Unit tests cover default deny, grant boundaries, expiry, secret-data restrictions, approval gates, pinned/secret file protection, and restore.

These implementations are host-JVM policy models only. They are not Android Binder services, SELinux enforcement, Android permission checks, or real filesystem operations. See [Security and Storage service contracts](SECURITY_AND_STORAGE_SERVICES.md).
