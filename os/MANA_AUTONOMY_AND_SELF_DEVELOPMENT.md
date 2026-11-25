# MANA Genesis 4.0 — AI autonomy and OS control boundary

This milestone connects the MANA Brain intent layer to a typed system-action broker. It is designed for a local-first assistant that can operate approved device features while keeping firmware and security-critical changes outside free-form model output.

## Runtime flow

1. MANA Brain interprets the owner's request and proposes a typed `ManaActionRequest`.
2. `ManaAutonomyController` checks the requesting principal, registered action ID, exact parameter schema, risk level, and explicit capability grant.
3. Routine actions may reach an Android platform adapter. Disruptive actions pause for owner confirmation.
4. Every result is recorded in an in-memory audit snapshot. Production Android integration must persist the audit securely.
5. The platform adapter uses documented Android APIs and the actual Android role/permission model. The broker does not grant Android permissions by itself.

## Self-development policy

MANA may analyse logs, draft code changes, run tests, compare versions, and prepare a candidate update. A candidate must be reproducibly built, signed, verified against a trusted key, matched to the exact device/product, and admitted by the OTA verifier. Installation is a separate owner-confirmed platform operation. The language model cannot rewrite the running system, grant itself privileges, alter SELinux, unlock the bootloader, or flash partitions.

## Current implementation boundary

The broker and host-side tests are Kotlin/JVM code. They do not yet provide privileged Android services, device-owner authority, root, kernel control, a complete Android framework, or an installable RMX5566RU firmware. Those require the AOSP product integration and verified OEM device artifacts described in `device/RMX5566RU_ROM_BUILD_AND_SAFETY_PLAN.md`.

## Default action catalogue

- Launch an installed application.
- Set torch, brightness, and audio volume.
- Change Do Not Disturb only after explicit confirmation.
- Send a message only after explicit confirmation and the platform's messaging permissions/role checks.
- Firmware installation is rejected by this broker and must use the separate signed OTA admission pipeline.

Unknown actions and unexpected model-generated parameters are denied by default. No shell-command action exists.
