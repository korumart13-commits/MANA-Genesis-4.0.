# MANA Genesis 4.0 — RMX5566RU device port scaffold

Target identity observed from the owner's Android 16 audit:
- Product/model: RMX5566RU / RMX5566
- Device: RE645F
- SoC: Qualcomm Snapdragon 685 (realme's official product specification); the owner's Android audit reports the Qualcomm SM6225 family and platform codename bengal. Some public catalogs label the variant SM6225pro; the exact silicon/platform identifiers must be read from the live device inventory before selecting any board support.
- Runtime kernel: 5.15.197-android13
- Android API: 36
- Current stock build: RMX5566_16.0.5.1310(EX01)

This directory is a **deferred device-port workspace**, not an enabled AOSP product. It deliberately contains no invented BoardConfig, kernel defconfig, DTS, fstab, init scripts, vendor blobs, VINTF manifest, partition sizes, AVB keys or recovery images.

Important compatibility boundary: other Snapdragon 685 / SM6225 devices (including Xiaomi and Motorola ports) are research references only. Their device trees, vendor blobs, kernel configurations, partition layouts and boot-chain assumptions must not be copied into RMX5566RU without matching hardware/build evidence. A shared SoC name is not proof of board compatibility.

## Files MANA must populate after evidence collection

1. `AndroidProducts.mk` and a device product makefile, only after the exact device tree is verified.
2. `BoardConfig.mk`, using measured partition and boot image metadata.
3. `device.mk`, init/fstab/ueventd configuration and overlays from matching OEM sources.
4. VINTF device manifest and framework compatibility results.
5. Exact kernel source, defconfig, DTS/DTBO and module ABI.
6. Proprietary vendor libraries and firmware with recorded provenance and hashes.
7. Verified AVB chain, rollback indexes, A/B slot and dynamic partition metadata.
8. Owner-usable stock restore package and tested rollback procedure.

## Post-install MANA completion contract

MANA may search public source metadata, inspect the live device through Android's documented read-only APIs, compare source revisions, run generic emulator tests and ask connected GPT providers to review bounded source proposals. Public search hits remain untrusted until their device/build identity, licensing, integrity and hardware compatibility are independently verified.

MANA must never infer compatibility from a repository name, download-and-execute arbitrary scripts, replace the stock boot chain, unlock the bootloader, or write device partitions autonomously. Device-specific images remain blocked until every required evidence item and owner approval is present.
