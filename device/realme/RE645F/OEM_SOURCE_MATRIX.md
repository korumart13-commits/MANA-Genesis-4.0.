# RMX5566 / RE645F OEM source matrix

Updated: 2026-11-26

This file pins public source leads for MANA Genesis 4.0. It is a provenance record, **not** a claim that these sources already form a bootable device port.

## Exact target profile

| Field | Target evidence |
|---|---|
| Product/model | realme C85 4G, RMX5566RU |
| Device | RE645F |
| Platform | Qualcomm SM6225 / Snapdragon 685, `bengal`, `qcom` |
| Android | 16 / API 36 |
| Runtime kernel | 5.15.197-android13-8-o-01074-gdfa4acfa411e |
| Reported Oplus project ID | 25697 (external firmware-catalog match; target-side property not independently confirmed in this record) |
| Boot | A/B, dynamic partitions; bootloader locked; AVB green |

## Best public OEM source lead

### Kernel source candidate

- Repository: https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-kernel-source
- Audited commit: `52ea05ec6849f7c3dd9a65864b5a52038deb9f9d`
- The commit identifies an RMX5555/RMX25665 C85-family Android 16 release, not RMX5566RU.
- `build.config.msm.bengal` sets `MSM_ARCH=bengal`, GKI/consolidate variants, boot header v4, init_boot, vendor_dlkm, system_dlkm and DT overlay support.
- Kernel `Makefile` declares Linux 5.15.180.

### Vendor / kernel-platform source candidate

- Repository: https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-vendor-source
- Audited commit: `2ab37fb9ceda42cd8679dec8585d4ff5209c4e90`
- Contains Oplus Qualcomm device-tree overlays for the Moscow project family.
- Exact overlay candidate: [Moscow-25697-khaje-qrd-nopmi-overlay.dts](https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-vendor-source/blob/2ab37fb9ceda42cd8679dec8585d4ff5209c4e90/kernel_platform/qcom/proprietary/devicetree/oplus/Moscow-25697-khaje-qrd-nopmi-overlay.dts)
- The overlay explicitly sets `oplus,dtsi_no = <25697>` and `oplus,project-id = <25697>`; it includes the Khaje QRD base, common Moscow overlay, NFC, sensor and charging fragments.
- This is strong evidence that the OEM published a 25697 Moscow overlay in the C85/C85 Pro source family. It does not by itself prove that every device component or vendor binary matches RMX5566RU.


### Full stock firmware research lead

- RMX5566 catalog: https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894
- Listed package: `RMX5566export_11_15.0.2.900(EX01)_202601231432250000.zip`, 11.70 GB (catalog entry dated 2026-08-20).
- A second listing identifies an RMX5566 package with Project ID 25697 and 12.00 GB size: https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894
- This is a potential stock-firmware donor for offline inspection of partition metadata, VINTF, init, firmware and proprietary libraries. The package build is not established as identical to the owner's Android 16 `RMX5566_11.F.03_2030_202610170441` build. It must not be flashed or treated as a compatible vendor image without comparing build fingerprints and partition contents.
- The realme support update page documents its official local/recovery update process, but it does not provide a public RMX5566 service image: https://www.realme.com/ae/support/software-update

## Compatibility decision

**Candidate only — not accepted for kernel/image use yet.**

The source family is relevant: Snapdragon 685 / SM6225, Bengal and Moscow overlays align with the target family. However, the available kernel source declares 5.15.180 while the target runtime reports 5.15.197. The audited commit names a different RMX release. A shared SoC, product family or project ID is not enough to safely substitute a kernel or DTBO.

Before accepting these sources, compare against the already collected RMX5566RU audit and obtain authoritative confirmation of:
1. the target's live Oplus project ID and panel/touch/sensor sub-variants;
2. the exact kernel source revision/config matching 5.15.197, including module ABI and symbol versions;
3. the correct DTS/DTBO composition and boot image packaging;
4. exact Android vendor/odm blobs, firmware, HAL/VINTF declarations and SELinux policy;
5. partition metadata, AVB descriptors and a tested stock restore route.

## Explicit exclusions

- Do not use C85 5G / RMX5253 / MediaTek sources.
- Do not build a 5.15.197 target using the 5.15.180 candidate merely because both are 5.15.
- Do not copy an overlay based only on its filename.
- Do not generate a flashable ZIP, boot image, vbmeta or super image from guessed values.
- Do not unlock or flash the owner's phone as part of source discovery.

## Public references

- OEM kernel source: https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-kernel-source
- OEM vendor/kernel-platform source: https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-vendor-source
- RMX5566 firmware catalog entry: https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894
- Android kernel build requirements: https://source.android.com/docs/setup/build/building-kernels

## Release state

`ROM_READY=false`

The MANA Android userspace components can continue to be developed independently, but no complete, verified RMX5566RU ROM can be built from the currently pinned inputs. This file records the best OEM lead found so the next porting step starts from real source instead of guessed device files.
