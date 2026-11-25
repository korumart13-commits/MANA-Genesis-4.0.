# MANA Genesis 4.0 — integrated Android 16 build sequence

This sequence targets a complete Android 16-based operating system, not a launcher-only APK. The product architecture baseline is defined in [ANDROID16_PRODUCT_ARCHITECTURE.md](ANDROID16_PRODUCT_ARCHITECTURE.md).

## Workstream A — Android platform and reproducible source

- Pin a supported Android 16 AOSP manifest and toolchain.
- Establish reproducible repo sync and source integrity checks in CI.
- Record the exact AOSP revision and all external project revisions.
- Create the MANA product configuration and system package list only after the AOSP tree is present and module names are verified.
- Integrate MANA system services and apps as product modules, not as a standalone launcher APK.
- Build system, system_ext, product and related images in CI only after a valid RE645F product target exists.

## Workstream B — stock Android applications and device bring-up

- Prefer the matching Android 16/AOSP system applications for Camera, Dialer/Phone, Contacts, Messaging and basic utilities. Do not substitute mock MANA screens.
- Verify the actual module names and availability against the pinned Android 16 source before adding PRODUCT_PACKAGES entries.
- Camera operation additionally requires the exact camera provider/HAL, sensor configuration, firmware and permissions.
- Phone operation additionally requires the matching telephony stack, modem/vendor components, audio routing and carrier/IMS support where applicable.
- Verify exact device tree and vendor interface compatibility.
- Obtain matching proprietary blobs through a documented lawful extraction process.
- Verify VINTF manifests/matrices, SELinux policy, init scripts, fstab and dynamic partition metadata.
- Validate camera, audio, display, touch, radio, Wi-Fi, Bluetooth, GPS, sensors, charging and suspend/resume on the exact device.

## Workstream C — kernel

- Do not import a generic SM6225 kernel.
- Obtain and verify the matching OEM 5.15.197 source/config/DTS.
- Reproduce the stock kernel build before patching.
- Add MANA kernel changes only when there is a concrete kernel-level requirement; most MANA features belong in Android userspace.
- Build kernel artifacts in CI, but never flash them automatically.

## Workstream D — MANA system experience

- MANA System Core and privileged service lifecycle.
- Samsung One UI-inspired MANA Launcher: configurable home grid, dock, folders, widgets, app drawer, search, gestures and adaptive layouts; preserve Android app compatibility and accessibility.
- Real platform-level MANA SystemUI integration for status bar, notification shade, quick settings, lock screen and recents. A normal APK cannot replace these privileged platform surfaces.
- MANA Settings and Control Center integrated with Android's real settings and permission controllers.
- MANA Brain routing core is wired through Soong AIDL/Binder; package the LiteRT-LM provider in Soong or complete a protected authenticated IPC bridge to the companion runtime, then validate local-first inference.
- Model manager with explicit model selection, license acknowledgement, storage/RAM checks and offline operation.
- Avatar renderer integrated as a system assistant surface with an accessible non-overlay fallback.
- Diary, Life Manager, Contact Intelligence, receptionist and game integrations behind Android permission gates.
- Update manager with signed-package verification and rollback safeguards.

## Workstream E — security and release

- SELinux enforcing; least-privilege service domains and explicit Binder service policy.
- AVB and release signing designed around the device's actual boot chain.
- No hard-coded private keys, API keys or owner credentials.
- Reproducible build records, SBOM, license notices and test results.
- OTA package only after partition layout, signing and recovery are proven.
- No user-device flashing while bootloader is locked or recovery is unverified.

## Definition of done

A source tree, a successful CI build, a bootable image, and a device-accepted release are separate milestones. The product is not complete until it boots on the exact RMX5566RU and camera, telephony, audio, connectivity, sensors, security and assistant functions pass acceptance tests.
