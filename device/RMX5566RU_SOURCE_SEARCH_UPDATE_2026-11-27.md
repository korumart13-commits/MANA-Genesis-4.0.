# RMX5566 source search update — 2026-11-27

This is a continuation of `RMX5566RU_SOURCE_DISCOVERY_2026-11-27.md` and `RMX5566RU_ARCHIVE_AND_FIRMWARE_INPUT_AUDIT_2026-11-27.md`.

## New exact-target search pass

Search terms included:
- `RMX5566`
- `RE645F`
- `SM6225 5.15.197`
- `RMX5566 kernel` scoped to the official `realme-kernel-opensource` GitHub organization
- `SM6225` scoped to the official organization
- `RE645F device tree`

Result: no exact RMX5566/RE645F kernel repository, defconfig, DTS/DTSI, DTBO source or Android device tree was found in the returned public code-search results. This is a bounded search result, not proof that no private, unindexed, or separately hosted OEM source exists.

### Similar but rejected public candidates

1. Official `realme_12-AndroidV-vendor-source` contains Qualcomm/Oplus devicetree overlay fragments whose names reference SM6225 (for example `muses_overlay_common.dtsi`, `AMG-4G_overlay_common.dtsi`, and related fingerprint/touchpanel fragments). These are for other named Realme 12 product variants. No evidence maps them to RMX5566/RE645F, its `bengal` board, its exact kernel release, panel, camera, radio, sensors, power/charging or partition configuration. Treat only as Qualcomm/Oplus source-format reference; do not copy into an RMX5566 device tree or use to build images.
   - Repository: https://github.com/realme-kernel-opensource/realme_12-AndroidV-vendor-source
2. Official `realme_9i_9-4G-AndroidT-kernel-source` and `realme_9_AndroidS-kernel-source` contain SM6225-related SoC support code, but their names/releases target older and different products. No exact RMX5566 board mapping or 5.15.197 build identity was established. Not a kernel source candidate for this phone.
   - https://github.com/realme-kernel-opensource/realme_9i_9-4G-AndroidT-kernel-source
   - https://github.com/realme-kernel-opensource/realme_9_AndroidS-kernel-source
3. Community Xiaomi SM6225-AD 5.15 device-tree project is explicitly WIP and for Xiaomi hardware, not Realme C85. It is not a valid RMX5566 device tree.
   - https://github.com/xiaomi-6225-AD/android_kernel_xiaomi_sm6225-devicetrees

The exact owner-reported kernel release remains:
`5.15.197-android13-8-o-01074-gdfa4acfa411e`.
A shared SoC name, Adreno 610 GPU, or Linux 5.15 major/minor version is not sufficient to establish board, vendor-module, boot-image, or DTBO compatibility.

## OTA / factory package search

The currently discoverable third-party RMX5566 listings expose `RMX5566export_11_15.0.2.900(EX01)` Android 15 packages, not the owner's Android 16 build `RMX5566_16.0.5.1310(EX01)`. These listings do not establish an official, signed, exact-build recovery package or prove an accessible download can restore this phone. Do not downgrade or flash these packages.
- https://romprovider.com/firmware/realme/rmx5566/
- https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894

An open-source OPlus firmware retrieval project documents querying the vendor OTA endpoint using model, current OTA version, ColorOS/realme UI version and regional identifiers, then checking package size and checksum when supplied. This is a possible *discovery method* for an official OTA response, not evidence that the endpoint currently offers the owner's exact build or that the response is a full restore package:
- https://github.com/suddenBook/OPlus_firmware_download
- https://github.com/R0rt1z2/realme-ota

Any retrieved package must be checked for exact model/region/build, payload type (incremental vs full), package signature, manifest target, rollback index, partition coverage and checksum. An incremental OTA that applies only over a specific source build is not a factory recovery image.

## Soong / Binder test runner review

Reviewed `tools/run_mana_soong_integration.sh`. It:
- requires a full Android 16 / API 36 AOSP checkout;
- checks that the selected product includes `ManaBrainService` and `ManaLauncher`;
- stages the exact unsigned Gradle APK and rejects an already-signed input;
- invokes Soong builds for `mana-brain-interface`, `ManaBrainService`, `ManaLauncher`, `mana-brain-request-policy-tests` and `ManaBrainBinderIntegrationTests`;
- verifies the resulting launcher and Brain service APK signatures against the selected platform certificate;
- runs host policy tests with `atest --host`;
- only runs the Binder instrumentation test when explicitly passed `--run-device-tests` and the selected image is running with both packages installed.

This is a real build/test runner, but no full AOSP checkout, product target, platform signing environment, emulator or RMX5566 test image is currently available in the CI workflow. Therefore the script's Soong build and Binder/device execution remain unverified; source-contract checks are not equivalent to executing them.

## Latest Gradle CI verification and artifacts

Workflow run 945:
- Run ID: `45924776238`
- Commit: `10cb74c5f3b73a50ea0d7a1b0b3fd65ce28832b6`
- URL: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45924776238
- Static architecture checks, Brain/Core host tests, Android launcher/test-app compilation, unsigned release APK build and artifact packaging all completed successfully.
- Artifact upload records:
  - Source ZIP `MANA-Genesis-4.0-source-project`, ID `15166510339`, 17,499,710 bytes, SHA-256 `e7c042c0a0c6cf5f04f482427dc311d1f73a8b08e9e4c6a23d287c373b322ca0`, expires 2026-12-27.
  - Unsigned release APK ZIP `MANA-Android-release-unsigned-for-AOSP-platform-signing`, ID `15166086215`, 45,819,562 bytes, SHA-256 `a74602354a3275776ea6eee8c7391f2b6896d66949418af1b9ddf36143d408ad`, expires 2026-12-11.
  - Debug APK ZIP `MANA-Android-debug-prototype-NOT-ROM`, ID `15166081382`, 49,717,830 bytes, SHA-256 `c9cba938839dd04133aa846a1690f625e800f464cb6ff500d6d342da355f127e`, expires 2026-12-11.

These are Gradle source/app artifacts. The APK artifact is unsigned and intended for later AOSP platform signing. None is a boot, vendor_boot, dtbo, vbmeta, super, recovery, OTA payload or flashable ROM.

## Current decision and next gates

- Keep the exact RMX5566 device product marked as an incomplete skeleton; do not substitute another Realme/Xiaomi device tree.
- Continue MANA Brain, Android app, platform service and host test work independently of device kernel availability.
- Request the exact GPL kernel release from Realme for RMX5566 / RMX5566RU and build `RMX5566_16.0.5.1310(EX01)`, including source commit, defconfig, DTS/DTSI, DTBO sources and build instructions.
- Seek an official OTA response for the exact build through the vendor update mechanism; verify whether it is incremental or full and whether a tested restore package exists.
- Only after exact kernel/device tree/vendor inputs and a tested recovery route are available: create a dedicated Android 16 product target, run Soong, VINTF, SELinux, AVB and boot tests, then test hardware functions on a recoverable device.

No flash operation, bootloader unlock, partition write, AVB bypass or downgrade is authorized or performed by this work.
