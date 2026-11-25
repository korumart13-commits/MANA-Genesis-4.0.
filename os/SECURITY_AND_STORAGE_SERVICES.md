# MANA OS 4.0 — Security and Storage service contracts

## MANA Security Service

The JVM-hosted policy engine uses exact principal/resource/action capabilities. Access is denied unless an explicit matching grant exists. Grants may expire; revocation and explicit denial are supported; every authorization decision is recorded in an audit snapshot. Secret data is blocked from SHARE and NETWORK actions by this baseline policy.

This is a host-testable policy model only. It does not enforce Android runtime permissions, Android Binder caller identity, SELinux, filesystem permissions, encryption, or kernel isolation. Those require the Android 16 AOSP integration and device-specific policy work.

## MANA Storage Service

The current implementation stores metadata only. It can inventory registered items, recommend reversible archiving for inactive unpinned non-secret items, and move metadata into ARCHIVED or QUARANTINED state only after explicit user approval. Restore is supported and operations are recorded.

It does not scan, open, move, or delete actual user files. Permanent deletion is deliberately disabled, including when a caller supplies approval. No item is automatically archived or quarantined merely because a recommendation exists.

## Required Android integration

- Bind policy decisions to verified Binder caller identity and Android permission checks.
- Design SELinux domains and labels together with init/Binder services; never use permissive mode as a shortcut.
- Use app-private storage and Android Storage Access Framework grants; do not bypass scoped storage.
- Add encrypted metadata, integrity checks, crash-safe transactions, and restore verification before touching real files.
- Keep permanent deletion outside the automatic cleanup path; require a separate user-visible review and recovery design.
- Add instrumentation tests on the target AOSP build before declaring either service integrated.

## Current status

These are JVM service contracts with unit tests. They are not yet Android system services and do not make a bootable MANA OS image.
