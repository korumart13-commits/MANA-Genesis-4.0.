# RMX5566 OTA discovery and risk boundary — 2026-11-27

## Target identity (owner evidence)

- realme C85 4G, RMX5566 / RMX5566RU, hardware revision RMX5566_11
- device RE645F, board/platform bengal, Qualcomm SM6225 family / Snapdragon 685
- Android 16, realme UI 7.0
- build RMX5566_16.0.5.1310(EX01)
- kernel 5.15.197-android13-8-o-01074-gdfa4acfa411e
- Vendor API/VNDK 33, API 36, A/B and dynamic partitions per owner-provided read-only audit

## New public OTA research

1. The open-source OPlus Firmware Download project describes querying OPPO/realme/OnePlus OTA endpoints using the exact device model, current OTA version, UI version, region and NV ID, then parsing download URL, size and MD5. It supports downloading full packages when the backend returns one. This is a discovery method, not confirmation that the current RMX5566 build is listed:
   https://github.com/suddenBook/OPlus_firmware_download
2. The realme-ota project is another open-source implementation of the vendor OTA request protocol:
   https://github.com/R0rt1z2/realme-ota
3. The public read-only OTA catalog supports filtering by canonical model and region:
   https://roms.danielspringer.at/api/ota.php?model=RMX5566
   The endpoint could not be queried successfully through this research environment. No claim is made that it returns a package for RMX5566.
4. Public RMX5566 firmware directories found in this research show Android 15 builds such as 15.0.2.900(EX01), not the owner's Android 16 build 16.0.5.1310(EX01):
   https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894
   https://hello-firmware.com/index.php?a=downloads&b=folder&id=6888
5. A user complaint and response reproduced on a third-party Brazilian consumer complaint portal quotes a realme support representative as saying that firmware reinstallation is handled by authorized service centers using technical tools and that public firmware downloads are not provided. This is secondary evidence and region-specific; it is not a direct global realme policy statement:
   https://www.reclameaqui.com.br/realme-brasil_1031984/smartphone-realme-c85-4g-com-looping-apos-atualizacao-de-software-indisponibilidade-do-firmware-para-recuperacao_ZBVbaDrc-ZdY4sDv/

## Why an exact full package matters

A package found by model alone may be:
- incremental OTA requiring a particular source build;
- a package for another region / NV ID;
- a downgrade or rollback-protected release;
- a service-only package;
- a package missing required partitions for full restoration.

Before accepting any package, record its source and exact model, region/NV ID, target build, source build (if incremental), payload type, file size, signature status, manifest partition list, rollback indexes and checksums. A matching filename is not sufficient. Never flash an unknown or mismatched package.

## Kernel and device-tree status

No exact public source match has been established for kernel release:
`5.15.197-android13-8-o-01074-gdfa4acfa411e`.

No verified RMX5566 defconfig, DTS/DTSI, DTBO source, complete device tree, or matching vendor blob set has been located. Similar SM6225/Oplus projects remain reference material only. Do not mix in Xiaomi, Realme 9i/9/12 or C85 5G sources.

Android common kernel documentation explains how to build a kernel once the correct branch and device configuration are known. It does not provide the OEM configuration for this phone:
https://source.android.com/docs/setup/build/building-kernels

## Engineering strategy

- Proceed aggressively on reversible source-level work: MANA Brain, platform services, AOSP product integration, test harnesses, device compatibility documentation and OTA metadata parsing.
- Add an OTA discovery/verification utility only if it can report exact metadata and preserve provenance; do not make it auto-flash.
- Keep unknown device-specific values explicit as TODO/unknown. Do not invent partition sizes, DTB mappings, vendor blobs, AVB keys or kernel config.
- Use an AOSP emulator/Cuttlefish for platform integration tests where possible, clearly marking those results as emulator tests, not RMX5566 hardware validation.
- Device-specific image creation remains blocked until exact kernel/device-tree/vendor inputs and a tested restore package/route are available.

## Risk boundary

The user authorizes pursuing technically risky research and experimental source changes. That is not equivalent to consent to irreversible phone operations. No bootloader unlock, partition writes, firmware downgrade, AVB bypass, modem/radio flashing or destructive install was performed. These operations remain gated on exact artifacts, backups and a proven recovery route.

## Current artifact distinction

Gradle CI artifacts (source ZIP, debug APK, unsigned release APK) are app/source artifacts only. They are not boot.img, vendor_boot.img, dtbo.img, vbmeta.img, super.img, recovery.img, payload.bin or a flashable ROM.
