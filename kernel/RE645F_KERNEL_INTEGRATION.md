# RE645F kernel integration plan

Status: **BLOCKED — no matching OEM kernel source approved**.

## Runtime target

- Device: RMX5566RU / RE645F
- SoC/platform: Qualcomm SM6225 / bengal / qcom
- Runtime kernel: `5.15.197-android13-8-o-01074-gdfa4acfa411e`
- Architecture: aarch64
- Android: 16 / API 36

## Rejected public candidates

The following repositories must not be used as the target kernel:

- realme C85 5G kernel source: targets RMX5253 / MediaTek and reports Linux 5.15.180.
- Older Realme SM6225/bengal candidate: reports Linux 4.19.157, not the device's 5.15.197 kernel.

A shared SoC family or board label does not establish compatibility. Do not transplant defconfig, DTS, modules, vendor ramdisk or boot artifacts from either candidate.

## Required OEM evidence

1. Source release explicitly mapped to RMX5566/RMX5566RU and preferably the exact firmware fingerprint.
2. Matching kernel source commit and exact upstream/vendor release.
3. Matching defconfig, DTS/DTSI, DTBO source and build scripts.
4. Compiler/toolchain version and reproducible build instructions.
5. Vendor kernel modules, symbol lists, vermagic and module dependency graph.
6. Boot image header/version, vendor_boot/init_boot layout, cmdline, bootconfig and AVB metadata from an authoritative source.
7. Stock recovery/restore procedure and verified rollback path.

## Kernel work policy

- Keep the stock kernel and boot chain untouched.
- Do not use a generic SM6225 GKI/common kernel as a drop-in replacement.
- First reproduce the OEM kernel build byte-for-byte or explain any non-reproducible differences.
- Keep MANA AI inference, provider routing, avatar, memory and most system features in Android userspace. A custom kernel is not required for those features.
- Add kernel code only for a demonstrated kernel-level need, with a threat model, tests and maintainership plan.
- Any kernel artifact produced in CI remains a test artifact; no automatic flashing or OTA inclusion.

## Current state

- [CONFIRMED] Runtime kernel identity and SoC/platform.
- [REJECTED] Two public candidate trees fail version/device matching.
- [MISSING] Matching OEM source, defconfig, DTS/DTBO, modules and build toolchain.
- [BLOCKED] Kernel compilation for RE645F and all boot image generation.
