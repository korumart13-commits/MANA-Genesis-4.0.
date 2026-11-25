# MANA Genesis 4.0 — RMX5566RU target profile

Status: device profile recorded from the owner's previously supplied Termux audit. This file is a build/compatibility target, not proof of a bootable ROM.

## Confirmed device data
- Product/model: realme RMX5566RU (realme C85 5G)
- Device: RE645F
- SoC/platform: Qualcomm SM6225, board/platform bengal, hardware qcom
- Android: 16 / API 36
- Build fingerprint: realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys
- Incremental: V.2f025ac-8895bb-9a8318
- Earlier reported firmware build: RMX5566_16.0.5.1310(EX01)
- Earlier reported security patch: 2026-10-01
- ROM/UI: OPlus V16.0.0 / realme UI V7.0
- OTA profile: RMX5566_11.F.03_2030_202610170441
- NV ID: 00110111
- ABI: arm64-v8a, armeabi-v7a, armeabi
- Kernel: Linux 5.15.197-android13-8-o-01074-gdfa4acfa411e, aarch64
- Active slot: _a
- A/B updates: enabled
- Dynamic partitions: enabled
- Boot device: 4744000.sdhci
- Verified Boot: green
- Bootloader: locked (flash.locked=1)
- Previously reported /data: approximately 218 GB total, 101 GB used, 117 GB free

## Build constraints
1. Treat this as an Android 16 / API 36 ARM64 application target first.
2. Keep the stock boot chain, AVB, vendor image, modem, and partition layout untouched.
3. Do not produce or flash boot.img, vendor_boot.img, dtbo.img, vbmeta.img, recovery, super, or OTA firmware until exact device-specific sources, partition metadata, signing requirements, and an unlock/restore path are verified.
4. Build and test the MANA Android application/launcher as an ordinary APK first. An APK is not a replacement for the operating system or kernel.
5. Preserve Android permission boundaries. No root, system privileges, Device Owner status, or AVB bypass may be assumed.
6. Local LiteRT-LM inference is the preferred AI path; cloud model providers are optional online extensions. Model selection must respect available RAM, storage, thermal limits, and licensing.
7. The active slot _a is an observation only; never hard-code it for installation or updates.

## Still unverified / unavailable from the prior audit
- Exact RAM configuration and memory available to applications
- Exact partition sizes and contents, including boot, init_boot, vendor_boot, dtbo, vbmeta, super, product, odm, and recovery
- Device-specific kernel source/config, device tree, vendor blobs, HAL manifests, VINTF compatibility matrix, proprietary firmware
- Bootloader unlock eligibility and OEM unlock state
- AVB key/chain details and the device's exact OTA payload format

## Required build stages
- Stage 1: compile, unit-test, and package the Android APK using the repository's Android 16 workflow.
- Stage 2: install and validate the APK without modifying system partitions.
- Stage 3: gather/verify OEM device sources and compatibility metadata before any ROM/device-tree work.
- Stage 4: only after source and recovery prerequisites are proven, evaluate a device-specific ROM build. No universal flash package is acceptable.
