# MANA Genesis 4.0 — RMX5566RU device port scaffold

Target identity observed from the owner's Android 16 audit:
- Product/model: RMX5566RU / RMX5566
- Device: RE645F
- SoC family: Qualcomm SM6225, platform codename bengal
- Runtime kernel: 5.15.197-android13
- Android API: 36
- Current stock build: RMX5566_16.0.5.1310(EX01)

This directory is a **deferred device-port workspace**, not an enabled AOSP product. It deliberately contains no invented BoardConfig, kernel defconfig, DTS, fstab, init scripts, vendor blobs, VINTF manifest, partition sizes, AVB keys or recovery images.

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
