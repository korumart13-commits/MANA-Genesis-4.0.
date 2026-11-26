# RMX5566RU source discovery report — 2026-11-27

## Device identity is now resolved

The owner-provided runtime properties and the two Settings screenshots identify the target as:

- Marketing name: `realme C85` (4G model)
- Model/product: `RMX5566` / `RMX5566RU`
- Device codename: `RE645F`
- Board/platform: `bengal`
- SoC: Qualcomm `SM6225` / Snapdragon 685 family
- Hardware: `qcom`
- GPU: Adreno 610
- ABI: arm64-v8a

The screenshots independently confirm the retail model, exact software build, kernel release, hardware revision, Android version, RAM configuration and storage capacity. The runtime audit remains the source of truth for the device target.

### Owner-observed device profile

| Field | Observed value | Evidence |
|---|---|---|
| Device name | realme C85 | Settings → About device |
| Model | RMX5566 | Settings → About device |
| Software build | RMX5566_16.0.5.1310 (EX01) | About device / Version |
| Android release | Android 16 | Version |
| realme UI | 7.0 | Version |
| Firmware branch labels | Q_V1_P14, Q_V1_P14 | Version |
| Hardware revision | RMX5566_11 | Version |
| Kernel release | 5.15.197-android13-8-o-01074-gdfa4acfa411e | Version |
| SoC | Qualcomm Snapdragon 685, octa-core | About device |
| RAM | 8 GB physical + 16 GB RAM expansion | About device |
| Internal storage | 256 GB total; 140 GB shown in use | About device |
| Battery | 7000 mAh (typ.) | About device |
| Display | 6.8 inch | About device |
| Cameras | 8 MP front, 50 MP rear | About device |
| Android security update | 1 October 2026 | Version |

The 16 GB RAM expansion is virtual/assisted memory and must not be counted as physical RAM. The screenshot reports 140 GB used out of 256 GB; this is a point-in-time usage figure, not a partition map.

This is the **realme C85 4G**, not the realme C85 5G. The distinction is important because the two retail names refer to different hardware platforms:

- Public model index maps `RMX5566` to realme C85 codename `moscow-b`.
- The same index maps `RMX5253` to realme C85 5G codename `mumbai`.
- realme's official C85 specification lists Snapdragon 685 / Adreno 610.
- realme's official C85 5G specification lists MediaTek Dimensity 6300 / Mali-G57.

References:
- Model-code index: https://github.com/KHwang9883/MobileModels/blob/master/brands/realme_global_en.md
- realme C85 official specs: https://www.realme.com/global/realme-c85/specs
- realme C85 5G official specs: https://www.realme.com/th/realme-c85-5g/specs
- Qualcomm Snapdragon 685 product brief: https://docs.qualcomm.com/doc/87-43683-1/87-43683-1_REV_C_Snapdragon_685_4G_Mobile_Platform_Product_Brief.pdf

Do not use C85 5G / RMX5253 / MediaTek device trees, kernels, vendor images or firmware for RMX5566RU.

## Source discovery outcome

Searches were performed for `RMX5566`, `RE645F`, `moscow-b`, `SM6225 5.15.197`, and `bengal defconfig` in public GitHub-indexed content and repository listings.

- No exact RMX5566/RE645F device tree with verifiable `BoardConfig.mk`, `device.mk`, fstab and init configuration was located in the returned results.
- No exact kernel source matching the reported runtime release `5.15.197-android13-8-o-01074-gdfa4acfa411e` and the target's board/device mapping was located.
- Previously reviewed candidates remain rejected: the C85 5G kernel tree targets a different MediaTek/RMX5253 device; an older SM6225/bengal tree reports Linux 4.19.157, which does not match the device's 5.15.197 runtime kernel.
- A generic SoC match or a repository name containing “bengal” is not sufficient evidence of compatibility.

A search returning no exact candidate is not proof that no OEM source exists. It means no candidate is approved from current evidence.

## Next device-audit stage

The two screenshots strengthen the identity and firmware baseline, but they do not reveal the partition layout, boot chain, VINTF declarations, vendor interface level, exact kernel config, device tree or proprietary firmware inventory. The next safe audit is a read-only Termux/ADB property and partition inventory. It must not unlock, root, modify or flash the phone.

Collect, where Android permits:

- `getprop` output (especially fingerprint, incremental build, product/vendor/boot properties, ABI and security patch properties).
- `/proc/cmdline`, `/proc/version`, `/proc/partitions` and block-device symlink names.
- `/vendor/etc/vintf/manifest.xml`, framework compatibility matrix and vendor API level, if readable.
- `/sys/fs/selinux/enforce` and verified-boot state properties.
- Bootloader unlock eligibility and official stock firmware/restore package availability, without changing device state.

Do not upload personal identifiers, serial numbers, IMEI, Android ID, Wi-Fi identifiers, account data or tokens. Redact those fields before sharing diagnostics.

## Required artifacts to unblock device ROM work

1. OEM kernel source release and exact commit mapped to RMX5566/RMX5566RU and the current firmware fingerprint.
2. Matching `bengal` defconfig, DTS/DTSI, DTBO source and kernel build instructions.
3. Device tree with `BoardConfig.mk`, `device.mk`, fstab, init scripts, partition definitions and product inheritance.
4. Matching vendor blobs and proprietary firmware, with legal provenance and hashes.
5. VINTF device manifest and framework compatibility matrix for Android 16.
6. Boot/init_boot/vendor_boot/dtbo/vbmeta/super partition metadata and AVB chain information.
7. Verified bootloader-unlock eligibility and a tested stock restore/recovery procedure.

## Current decision

Continue userspace MANA development, Android platform service design and build automation. The next device-specific work is a read-only hardware/firmware inventory and source matching. Do not build or flash device-specific boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA images until the exact artifacts above are verified.
