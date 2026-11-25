# RE645F device-tree foundation (MANA Genesis 4.0)

Status: **FOUNDATION / NON-BUILDABLE PLACEHOLDER**. This directory records the exact target and the structure required for a future device tree. It deliberately does not claim to contain a working BoardConfig, product makefile, fstab, init scripts, DTS, DTBO or proprietary blobs.

## Target identity (runtime evidence wins)

- Product: RMX5566RU
- Device codename: RE645F
- SoC: Qualcomm SM6225, platform `bengal`, hardware `qcom`
- ABI: arm64
- Android target: 16 / API 36
- Runtime kernel: 5.15.197-android13-8-o-01074-gdfa4acfa411e
- Boot scheme: A/B; dynamic partitions; observed active slot `_a`
- AVB: green; bootloader locked

The hardware profile maps to the Snapdragon 685 / C85 4G platform. Do not use C85 5G / RMX5253 / MediaTek resources.

## Expected eventual tree (not yet populated)

```
device/realme/RE645F/
  AndroidProducts.mk
  RE645F.mk
  BoardConfig.mk
  device.mk
  proprietary-files.txt
  manifest.xml
  compatibility_matrix.xml
  init/
  fstab/
  sepolicy/
  overlay/
  rootdir/
  kernel/
```

Only add these build files when their values are sourced from matching OEM firmware/source or verified runtime metadata. A syntactically valid but guessed fstab, partition size, boot header version, kernel cmdline, AVB key, SELinux rule or HAL declaration is not acceptable.

## Evidence gates

1. Obtain OEM source/release metadata explicitly matching RMX5566/RMX5566RU and the target fingerprint.
2. Verify partition names, sizes, filesystem types, logical partition groups and slot metadata from authoritative device metadata.
3. Obtain matching kernel source, defconfig, DTS/DTSI and DTBO sources.
4. Obtain the matching vendor/odm interface declarations and legally usable vendor blobs.
5. Validate VINTF compatibility, init service dependencies, SELinux policy and firmware dependencies.
6. Build and test a non-flashable product target first. Device image generation remains disabled until recovery, AVB and restore paths are independently validated.

## Current implementation state

- [CONFIRMED] Target identity and runtime kernel string from owner-provided audit.
- [CONFIRMED] A/B, dynamic partitions, locked bootloader and green Verified Boot.
- [DOCUMENTED] This directory's intended layout and bring-up gates.
- [MISSING] Actual device tree, product makefiles, BoardConfig, fstab, init scripts, DTS/DTBO and overlays.
- [MISSING] Verified vendor blobs and device-specific VINTF/SELinux integration.

Do not run `lunch` for RE645F and do not create images from this directory yet: no product target is defined.
