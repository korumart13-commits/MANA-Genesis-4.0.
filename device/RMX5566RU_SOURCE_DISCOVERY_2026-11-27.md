# RMX5566RU source discovery report — 2026-11-27

## Device identity is now resolved

The owner-provided runtime properties identify the target as:

- Model/product: `RMX5566RU`
- Device codename: `RE645F`
- Board/platform: `bengal`
- SoC: Qualcomm `SM6225` / Snapdragon 685 family
- Hardware: `qcom`
- GPU: Adreno 610
- ABI: arm64-v8a

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

The runtime audit from the actual phone takes precedence over a retail label. Do not use C85 5G / RMX5253 / MediaTek device trees, kernels, vendor images or firmware for RMX5566RU.

## Source discovery outcome

Searches were performed for `RMX5566`, `RE645F`, `moscow-b`, `SM6225 5.15.197`, and `bengal defconfig` in public GitHub-indexed content and repository listings.

- No exact RMX5566/RE645F device tree with verifiable `BoardConfig.mk`, `device.mk`, fstab and init configuration was located in the returned results.
- No exact kernel source matching the reported runtime release `5.15.197-android13-8-o-01074-gdfa4acfa411e` and the target's board/device mapping was located.
- Previously reviewed candidates remain rejected: the C85 5G kernel tree targets a different MediaTek/RMX5253 device; an older SM6225/bengal tree reports Linux 4.19.157, which does not match the device's 5.15.197 runtime kernel.
- A generic SoC match or a repository name containing “bengal” is not sufficient evidence of compatibility.

A search returning no exact candidate is not proof that no OEM source exists. It means no candidate is approved from current evidence.

## Required artifacts to unblock device ROM work

1. OEM kernel source release and exact commit mapped to RMX5566/RMX5566RU and the current firmware fingerprint.
2. Matching `bengal` defconfig, DTS/DTSI, DTBO source and kernel build instructions.
3. Device tree with `BoardConfig.mk`, `device.mk`, fstab, init scripts, partition definitions and product inheritance.
4. Matching vendor blobs and proprietary firmware, with legal provenance and hashes.
5. VINTF device manifest and framework compatibility matrix for Android 16.
6. Boot/init_boot/vendor_boot/dtbo/vbmeta/super partition metadata and AVB chain information.
7. Verified bootloader-unlock eligibility and a tested stock restore/recovery procedure.

## Current decision

Continue userspace MANA development, Android platform service design and build automation. Do not build or flash device-specific boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA images until the exact artifacts above are verified.
