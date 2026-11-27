# RMX5566RU — MANA firmware installation and stock recovery plan

Status date: 2026-11-27
Target: realme C85 4G, RMX5566/RMX5566RU, RE645F, Qualcomm SM6225, Android 16.

## Safety decision

No MANA image is currently approved for flashing. The device has a locked bootloader and green Verified Boot. The exact RMX5566RU device tree, matching OEM kernel source/defconfig/DTS/DTBO, vendor blobs, VINTF, partition sizes and AVB chain have not been verified. A generic SM6225/bengal image is not enough evidence of compatibility.

Do not flash boot, init_boot, vendor_boot, dtbo, vbmeta, recovery, super, modem, persist, abl, xbl, or any raw partition image. Do not use EDL loaders, firehose programmers, service-account tools, test-point procedures, or commands that erase/write partitions.

## Installation paths assessed

### 1. Official realme OTA / local Recovery update — preferred stock recovery route

If the phone boots normally, use Settings → Software update. If realme provides a full signed package explicitly for RMX5566RU and the same region/build branch, follow realme support instructions for local installation. Recovery mode is entered with the phone powered off using Power + Volume Down; use Install from storage only if the matching package is accepted by the device.

This route restores or updates official stock software; it does not install MANA. Back up data first, keep battery above 40%, and do not interrupt installation.

### 2. Fastboot flashing — not currently available/approved

AOSP documents that flashing partitions requires an unlock-capable bootloader and user-confirmed unlock. Unlocking normally triggers a factory reset. The current audit only confirms that the bootloader is locked; it does not prove OEM unlocking is available or that this model accepts standard fastboot flashing unlock. No unlock command should be attempted until the owner has a verified complete stock restore package and the exact device-specific image set.

### 3. Qualcomm EDL / QFIL / MSM / Oplus service tools — not approved for owner-side use

Public third-party pages list RMX5566 stock packages and claim QFIL or authenticated Oplus service-tool paths. These are not verified by realme as an owner-supported universal flashing method. Some procedures require an authorized service account or a matching signed programmer. A wrong programmer or package can hard-brick the phone. Do not use these downloads or procedures for MANA installation.

### 4. Third-party stock firmware mirrors — reference only, not a rollback recommendation

Public listings were found for RMX5566 firmware `RMX5566export_11_15.0.2.900(EX01)` (Android 15-era branch) while the owner-provided device audit reports Android 16 build `RMX5566_16.0.5.1310(EX01)`. This is not a verified same-version restore package. Do not downgrade or flash it: anti-rollback, region, partition and signing compatibility have not been established.

## Required release gates before a MANA flashable package can exist

1. Exact RMX5566RU Android 16 product/device tree, with verified BoardConfig, product makefiles, fstab, init and partition definitions.
2. OEM kernel source and exact matching 5.15.197 defconfig, DTS/DTSI, DTBO and module ABI.
3. Legally sourced vendor blobs and firmware from the matching owner firmware branch, with hashes and provenance.
4. VINTF manifest/matrix compatibility and SELinux policy integration validated against a full Android 16 AOSP tree.
5. Exact partition map, dynamic partition metadata, A/B slot behavior, AVB/vbmeta chain and rollback-index plan.
6. Reproducible AOSP/Soong build, verified package signing, emulator tests and device-specific boot tests.
7. Complete owner-tested stock restore route and backups for all restorable critical partitions before any unlock or flash.
8. Recovery/OTA package validation, checksums, installation abort behavior and documented rollback.

## Planned delivery artifacts

- MANA source snapshot and build manifest.
- Reproducible Android 16 AOSP build instructions and CI logs.
- RMX5566RU device tree and matching kernel/vendor integration (only after sources are verified).
- Signed/unsigned engineering images for controlled testing, then a signed OTA/recovery package.
- SHA-256 manifest, compatibility report, install/rollback instructions and known-issues list.

Until all release gates pass, deliverables remain source code, app prototypes and host/Gradle test artifacts. They must not be labelled a flashable ROM.

## References

- realme Global Recovery troubleshooting: https://www.realme.com/global/support/troubleshooting
- AOSP bootloader lock/unlock requirements: https://source.android.com/docs/core/architecture/bootloader/locking_unlocking
- RMX5566 third-party firmware listing (unverified mirror; reference only): https://romprovider.com/firmware/realme/rmx5566/
- Third-party flashing-tool risk overview (not an OEM procedure): https://geekchamp.com/realme-flash-tool-is-there-an-official-universal-download/

## Additional firmware mirror research — 2026-11-27

Public mirror searches for the exact owner-reported Android 16 build `RMX5566_16.0.5.1310(EX01)` did not return a matching full OTA package. The RMX5566 catalog pages surfaced older `RMX5566export_11_15.0.2.900(EX01)` and 15.0.2.797/796/793/760 packages (Android 15 branch labels). Some third-party pages inconsistently label the RMX5566 package as “C85 5G”; that label conflicts with the target identity and is not accepted as a basis for flashing.

References reviewed:
- Solution Firmware RMX5566 catalog: https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894
- Hello Firmware RMX5566 catalog: https://hello-firmware.com/index.php?a=downloads&b=folder&id=6888
- ROMProvider RMX5566 page, which describes an authenticated Oplus service-tool/EDL path: https://romprovider.com/firmware/realme/rmx5566/
- Realme Updater, which says its catalog uses official download channels but did not expose a verified exact-build match in this search: https://realmeupdater.com/
- Open-source OPlus firmware discovery client (reference for OTA metadata discovery only; not a flashing tool): https://github.com/suddenBook/OPlus_firmware_download

Decision: no download link from a mirror is approved as a restore image for this phone. A package must match RMX5566RU, EX01 region, the owner build lineage and anti-rollback requirements; its official signature and integrity must be verified. The current Android 16 build should be preserved until a matching official recovery package is found. EDL/service-tool pages are evidence that service workflows exist, not authorization or a safe owner-side procedure.
