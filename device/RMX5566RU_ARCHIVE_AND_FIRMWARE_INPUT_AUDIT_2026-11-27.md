# RMX5566 device and legacy archive audit — 2026-11-27

## Confirmed target

The owner-provided screenshots and Termux archive identify the target as realme C85 4G:
- Model/product: RMX5566 / RMX5566RU
- Device: RE645F
- Board/platform: bengal
- Qualcomm Snapdragon 685 family / SM6225, Adreno 610
- Android 16 / API 36, realme UI 7.0
- Build: RMX5566_16.0.5.1310(EX01)
- Hardware revision: RMX5566_11
- Kernel: 5.15.197-android13-8-o-01074-gdfa4acfa411e
- 8 GB physical RAM; 16 GB extension is virtual memory
- 256 GB storage

Do not confuse this device with C85 5G / RMX5253 / MediaTek Dimensity 6300.

## Legacy archive inspection

The following Library archives were inspected by reading their ZIP entry lists and selected source files:

- `MANA_RMX5566_source.zip` (463,393 bytes; SHA-256 `6934ebab223c6a9cc22e7d11d1b5e024f28f71e541b1b254baf9d80faa70e8be`): 351 entries. It contains MANA app/platform source and a device-tree skeleton. It is explicitly not an OS image or flashable package. Its README incorrectly labels the target as “C85 5G”; this is stale and wrong for the owner's C85 4G. The skeleton's `BoardConfig.mk` contains TODOs, `proprietary-files.txt` is intentionally empty, and there is no verified matching kernel tree, vendor blob set, partition map, AVB metadata or signing key. Do not treat it as a valid RMX5566 device tree.
- `MANA_Genesis_4.0_REALME_OTA_SOURCE_PACKAGE.zip` (744,271 bytes; SHA-256 `f328e755e0d930c1ac24b337582cdca12ed2be1a0cd895cc1437957326df1720`): 408 entries; a source package based on the MANA project, not a built OTA payload.
- `MANA_Genesis_4.0_DEVICE_UPDATE_PACKAGE.zip` (743,417 bytes; SHA-256 `71f5951e6377480893094314629416f29a630e003681381b7f6ec60c60ccefd4`): 409 entries; source/update project files, not a device-flashable image.
- `MANA_Genesis_4.0_READY.zip` (738,327 bytes; SHA-256 `7d6f0e1540d22a99dd26966437967a2927f09720faffd8e42ae505927dd41292`): 405 entries; source project, not a ROM.
- `MANA_Genesis_4.0_RMX5566RU_Preflight.zip` is a small nested preflight package, not firmware.
- Termux audit v3 archive `mana_rmx5566_audit_20261127_043744.zip` was generated read-only and contains 130 entries, including build properties, kernel command line, partition metadata attempts, mounts, SELinux state and VINTF XML. It does not contain partition images or kernel source.

Older ZIPs must not be renamed or represented as a flashable ROM merely because their names contain READY, OTA, DEVICE_UPDATE or RMX5566.

## Current GitHub CI artifacts

The latest verified launcher workflow at the time of this report is run 945:
- Run: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45924071701
- Commit: `a7e362a0fd480e8f94887aa0bbb729f1c712264c`
- Conclusion: success.
- Source project artifact: `MANA-Genesis-4.0-source-project`, artifact ID `15166005382`, 17,496,062 bytes, expires 2026-12-27.
- Debug APK artifact: `MANA-Android-debug-prototype-NOT-ROM`, artifact ID `15165714197`, 49,717,832 bytes, expires 2026-12-11.
- Unsigned release APK artifact: `MANA-Android-release-unsigned-for-AOSP-platform-signing`, artifact ID `15165334495`, 45,819,562 bytes, expires 2026-12-11.

These are Gradle app/source artifacts. The APK release is unsigned and intended for later AOSP platform signing. None is a boot.img, vendor_boot.img, dtbo.img, super.img, recovery image, OTA payload or flashable ROM.

## Public source and stock-firmware search

Searches of the public realme kernel-open-source GitHub organization and GitHub-indexed code for RMX5566, RE645F, bengal and SM6225 5.15.197 did not identify a verified exact-match kernel source or device tree.

References checked:
- realme kernel-open-source organization: https://github.com/realme-kernel-opensource
- device profile index (third-party, not OEM authority): https://www.deviceinfo.app/devices/realme/c85/
- older RMX5566 stock firmware listing: https://firmwarefile.com/realme-c85-rmx5566/
- another RMX5566 firmware listing: https://romprovider.com/firmware/realme/rmx5566/

The public firmware listings found an older `RMX5566export_11_15.0.2.900(EX01)` Android 15 package, not the owner's Android 16 build `RMX5566_16.0.5.1310(EX01)`. These third-party listings are not verified recovery sources for the current phone and must not be flashed.

AOSP documents the Android common kernel branches and includes android13-5.15 as a supported branch for Android 16, but that is only a branch-level compatibility statement; it does not supply the exact OEM source, device tree, vendor modules or guarantee bootability:
https://source.android.com/docs/core/architecture/kernel/android-common

## Remaining blockers before a real device ROM can be built

1. Exact OEM kernel source/commit for this RMX5566 firmware, including matching defconfig, DTS/DTSI and DTBO source/build instructions.
2. Verified device tree: product makefiles, BoardConfig, fstab, init scripts, partition definitions and lunch target.
3. Matching vendor and ODM proprietary blobs with provenance and hashes.
4. Complete partition sizes/layout, boot chain and AVB/vbmeta metadata.
5. Exact-build stock restore package and a tested recovery route.
6. AOSP 16 build of the product, VINTF/SELinux validation, boot tests and device-specific functional tests.

The owner audit is read-only and does not provide partition images or bypass Android access restrictions. Do not infer missing hardware data or use blobs from another model. Preserve locked bootloader, Verified Boot, SELinux and encryption until a matching build and tested restore path exist.

## Decision

Continue MANA userspace, app, platform-service and build-system development. Keep the RMX5566 device tree explicitly marked as a skeleton until exact inputs are verified. No complete or safe RMX5566 flashable firmware has been produced by the legacy ZIP archives or current Gradle CI.
