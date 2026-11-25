# MANA Genesis 4.0 — RMX5566RU target profile

Status: device profile recorded from the owner's previously supplied Termux audit. This file is a build/compatibility target, not proof of a bootable ROM.

## Confirmed device data
- Product/model: realme RMX5566RU (realme C85; 5G retail naming must be verified separately)
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
1. Treat this as an Android 16 / API 36 ARM64 integrated-OS target; the launcher and assistant are source modules inside the OS project.
2. Keep the stock boot chain, AVB, vendor image, modem, and partition layout untouched.
3. Do not produce or flash boot.img, vendor_boot.img, dtbo.img, vbmeta.img, recovery, super, or OTA firmware until exact device-specific sources, partition metadata, signing requirements, and an unlock/restore path are verified.
4. Do not start a separate APK workflow. Integrate MANA Launcher and MANA Assistant as Android product modules in the eventual AOSP build. They are not a replacement for the operating system or kernel.
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
- Stage 1: validate JVM system services and compile/test MANA Android product modules against a pinned Android 16 AOSP tree.
- Stage 2: integrate the modules into the verified RMX5566RU product configuration without modifying the stock phone.
- Stage 3: gather/verify OEM device sources and compatibility metadata before device-specific ROM work.
- Stage 4: only after source and recovery prerequisites are proven, evaluate a device-specific ROM build. No universal flash package is acceptable.


## Model-code clarification
Independent device listings identify RMX5566/RE645F as the Qualcomm SM6225-based realme C85, while RMX5253 is listed as the C85 5G/MediaTek variant. The device codename and SoC audit take precedence over a marketing label. Do not select RMX5253 firmware or kernel sources for RMX5566RU.
