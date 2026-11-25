# RMX5566RU device/kernel source compatibility audit

Audit date: 2026-11-25  
Owner-reported retail name: realme C85 5G; runtime identity: realme C85 (4G), RMX5566RU / RE645F  
Owner-provided runtime facts: Qualcomm SM6225, `ro.board.platform=bengal`, `ro.hardware=qcom`, arm64-v8a, Linux kernel `5.15.197-android13-8-o-01074-gdfa4acfa411e`, Android 16.

## Device-name correction\n\nThe user's diagnostics identify Qualcomm SM6225/bengal/qcom. realme's official specifications distinguish the standard **realme C85** (Snapdragon 685, 4G) from **realme C85 5G** (MediaTek Dimensity 6300). Therefore RMX5566RU / SM6225 should be treated as the standard realme C85 (4G), not the C85 5G. The retail name was easy to conflate, but the SoC and build properties determine the target.\n\n- Official realme C85 (Snapdragon 685): https://www.realme.com/global/realme-c85/specs\n- Official realme C85 5G (Dimensity 6300): https://www.realme.com/global/realme-c85-5g/specs\n- Independent RMX5566 / RE645F mapping: https://www.deviceinfo.app/devices/realme/c85/\n\n## Result

**The exact OEM kernel/device package is still not verified. Do not build or flash a kernel from the repository below.**

A repository with a similar retail model name was found for the **different C85 5G / MediaTek variant**:

- [realme_C85_5G-AndroidV-kernel-source](https://github.com/realme-kernel-opensource/realme_C85_5G-AndroidV-kernel-source)
- Audited upstream commit: `75b7dd046767b696a02a954e0c0a9b53e04d4bad`
- Its top-level `Makefile` declares Linux **5.15.180**, not the owner's running 5.15.197.
- Its `arch/arm64/configs/mgk_64_k515_defconfig` sets `CONFIG_ARCH_MEDIATEK=y` and `CONFIG_BUILD_CONFIG_FILE="build.config.mtk.aarch64"`. This is a MediaTek kernel configuration, not the owner's Qualcomm SM6225 / bengal / qcom platform.
- The accompanying [realme_C85_5G-AndroidV-vendor-source](https://github.com/realme-kernel-opensource/realme_C85_5G-AndroidV-vendor-source) tree contains MediaTek/Oplus vendor kernel material, including 4.14/4.19/5.10 kernel module directories. It does not establish compatibility with this Qualcomm device.

Therefore the repository is a **false match for this owner's hardware**, despite the similar “C85 5G” retail name. The model code and SoC must take precedence over the marketing name.

## Why a generic 5.15 kernel is not enough

The Android Common Kernel documentation describes GKI/KMI compatibility and the separation between the generic kernel and hardware-specific vendor modules:

- https://source.android.com/docs/core/architecture/kernel
- https://source.android.com/docs/core/architecture/kernel/android-common

A generic kernel branch or a same-version kernel is not a drop-in replacement for the phone's vendor kernel. The exact KMI, vendor modules, device tree/overlays, boot image layout, AVB metadata and boot/recovery chain must match.

## Required source package before device work

The following must all be obtained and matched to the owner's fingerprints:

1. Qualcomm SM6225 / bengal OEM kernel source whose release/build metadata matches the running `5.15.197` kernel.
2. Exact defconfig and build configuration used for RMX5566RU / RE645F.
3. Matching DTS/DTBO sources and generated device-tree compatibility data.
4. Matching vendor kernel modules and symbol/KMI information.
5. Toolchain, build manifest and OEM build instructions.
6. Current boot/vendor_boot/dtbo/vbmeta partition metadata and a verified stock restore package.
7. A tested recovery/rollback procedure for both A/B slots.

## Safe conclusion

The C85 5G MediaTek source is not used. The exact Qualcomm RMX5566RU kernel source and device-specific tree remain a release blocker. No kernel image, boot image, DTBO, recovery image or OTA should be generated for installation until every required item above is verified.
