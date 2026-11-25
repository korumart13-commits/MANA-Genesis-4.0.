# MANA OS 4.0 — integrated build sequence

This sequence replaces the separate launcher-APK-first workflow.

## Workstream A — Android platform

- Pin a supported Android 16 AOSP manifest and toolchain.
- Establish reproducible repo sync and source integrity checks.
- Create the MANA product configuration and system package list only after the AOSP tree is present.
- Integrate MANA system services and privileged apps as product modules, not as a standalone launcher APK.
- Build system, system_ext, product and related images in CI.

## Workstream B — device bring-up

- Verify exact device tree and vendor interface compatibility.
- Obtain matching proprietary blobs through a documented lawful extraction process.
- Verify VINTF manifests/matrices, SELinux policy, init scripts, fstab and dynamic partition metadata.
- Validate camera, audio, display, touch, radio, Wi-Fi, Bluetooth, GPS, sensors, charging and suspend/resume.

## Workstream C — kernel

- Do not import a generic SM6225 kernel.
- Obtain and verify the matching OEM 5.15.197 source/config/DTS.
- Reproduce the stock kernel build before patching.
- Add MANA kernel changes only when there is a concrete kernel-level requirement; most MANA features belong in Android userspace.
- Build kernel artifacts in CI, but never flash them automatically.

## Workstream D — MANA system

- MANA System Core and privileged service lifecycle.
- MANA System UI, launcher, settings and control center.
- MANA Brain with local LiteRT-LM as first provider.
- Model manager with explicit model selection, license acknowledgement, storage/RAM checks and offline operation.
- Avatar renderer and animation assets integrated into System UI.
- Diary, Life Manager, Contact Intelligence, receptionist and game integrations behind Android permission gates.
- Update manager with signed-package verification and rollback safeguards.

## Workstream E — security and release

- SELinux enforcing; least-privilege service domains.
- AVB and release signing designed around the device's actual boot chain.
- No hard-coded private keys, API keys or owner credentials.
- Reproducible build records, SBOM, license notices and test results.
- OTA package only after partition layout, signing and recovery are proven.
- No user-device flashing while bootloader is locked or recovery is unverified.

## Definition of done

A source tree, a successful CI build, and a bootable image are three different milestones. The project must report each separately. A release is not device-ready until it boots and all critical hardware/security tests pass on the exact RMX5566RU.
