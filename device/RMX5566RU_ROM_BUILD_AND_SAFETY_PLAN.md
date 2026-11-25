# MANA Genesis 4.0 — RMX5566RU ROM readiness and safe build plan

Date: 2026-11-26
Target: realme RMX5566RU / RE645F, Qualcomm SM6225 (bengal), Android 16 / API 36
Decision: **NO FLASHABLE ROM OR INSTALLER ZIP MAY BE GENERATED YET.**

This plan is based on the device profile and kernel audit already committed in this repository. It separates verified runtime facts from missing OEM/build inputs. It is not a claim that a ROM has been built.

## 1. Device identity — use this exact target

The recorded runtime profile identifies:
- Model/product: realme RMX5566RU / RE645F
- Platform: Qualcomm SM6225, `bengal`, `qcom`
- Android: 16 / API 36
- Fingerprint: `realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys`
- Kernel: `5.15.197-android13-8-o-01074-gdfa4acfa411e`, aarch64
- ABI: arm64-v8a
- Active slot observed: `_a`
- A/B updates: enabled
- Dynamic partitions: enabled
- Bootloader: locked (`flash.locked=1`)
- Verified Boot: green

The runtime hardware data corresponds to the Snapdragon 685 / C85 4G platform. Do not use RMX5253 / C85 5G / MediaTek sources or images for this target. Do not infer a compatible kernel from the shared Qualcomm SoC name alone.

## 2. Repository inventory — what is and is not present

Present:
- MANA OS architecture and build-sequence documentation
- Device profile and replacement-readiness gates
- Kernel source audit notes
- MANA JVM core/services and tests
- Android launcher/companion source prototypes and CI definitions

Not present as a validated device build input:
- A complete pinned Android 16 AOSP manifest checkout
- `device/realme/RE645F` (or another proven RMX5566RU product/device tree)
- Exact RMX5566RU kernel source and matching defconfig/DTS/DTBO
- Matching proprietary vendor blobs and firmware
- Matching vendor HAL/VINTF manifests and compatibility matrix
- Captured and verified partition map, sizes and image headers
- AVB key/chain metadata and release signing configuration
- Verified stock restore package and tested recovery/bootloader unlock path
- A reproducible, device-specific full ROM build

The current `device/` and `kernel/` folders contain audit documents, not a complete device tree or kernel source. There is no `vendor/` tree in the project branch.

## 3. Kernel/vendor findings

Public source audit status (updated 2026-11-26):
1. `realme_C85_5G-AndroidV-kernel-source`: targets RMX5253 / MediaTek and is not applicable to this Qualcomm C85 4G.
2. `android_kernel_realme_sm6225` (android13-bengal) and `android_kernel_tree_realme_sm6225`: both declare Linux 4.19.157 and are not compatible with the reported 5.15.197 target.
3. New OEM C85-family candidate: `realme-kernel-opensource/realme_C85_C85pro-AndroidB-kernel-source` at commit `52ea05ec6849f7c3dd9a65864b5a52038deb9f9d` declares Linux 5.15.180 and `MSM_ARCH=bengal`. The companion OEM vendor/kernel-platform source at commit `2ab37fb9ceda42cd8679dec8585d4ff5209c4e90` contains Moscow Oplus overlays with project IDs 25665, 25666 and others, including SM6225-specific fragments. This is a promising OEM source candidate, not yet a verified RMX5566RU build input: the audited source commit identifies `RMX555525665_16.0.2.403(EX01)`, while this phone reports RMX5566RU and kernel 5.15.197. The phone's own Oplus project ID has not yet been captured and matched to the correct Moscow overlay.

Do not build or flash from any candidate yet. The new OEM source audit is recorded in [RMX5566RU_OEM_SOURCE_AUDIT_2026-11-26.md](RMX5566RU_OEM_SOURCE_AUDIT_2026-11-26.md). Required next evidence is a verified target project ID, exact source mapping/version, matching defconfig, DTS/DTSI, DTBO, module ABI and vendor module set. The kernel-platform source tree is not proof that the complete Android proprietary vendor partition, HAL binaries or exact VINTF manifests are available.

## 4. Partition and boot-chain gate

Known only at a high level:
- A/B slots exist; `_a` was active when inspected (observation only)
- Dynamic partitions are enabled
- Verified Boot is green
- Bootloader is locked

Still unknown and required:
- Exact partition names, byte sizes and logical/physical status
- Boot header version and contents of boot, init_boot and vendor_boot
- DTBO and vbmeta descriptors, chained vbmeta layout, rollback indexes
- Super metadata, logical partition groups and extents
- Recovery availability and whether it can sideload/install the intended package
- OTA payload format and exact stock restore route

Never hard-code slot `_a`. Never overwrite boot, init_boot, vendor_boot, dtbo, vbmeta, super, modem, persist, userdata or recovery during discovery. Never disable AVB or bypass rollback protection.

## 5. Safe read-only data collection

The next device audit should collect these properties in Termux without root and without writing to partitions:

```sh
mkdir -p "$HOME/mana_rmx5566_audit"
{
  echo "=== identity ==="
  getprop ro.product.model
  getprop ro.product.device
  getprop ro.product.vendor.device
  getprop ro.build.fingerprint
  getprop ro.build.version.release
  getprop ro.build.version.sdk
  getprop ro.build.version.incremental
  echo "=== hardware ==="
  getprop ro.board.platform
  getprop ro.boot.hardware
  getprop ro.soc.model
  getprop ro.product.cpu.abi
  getprop ro.product.cpu.abilist
  uname -a
  echo "=== boot state ==="
  getprop ro.boot.slot_suffix
  getprop ro.boot.slot
  getprop ro.boot.flash.locked
  getprop ro.boot.vbmeta.device_state
  getprop ro.boot.verifiedbootstate
  getprop ro.boot.avb_version
  getprop ro.boot.dynamic_partitions
  getprop ro.boot.super_partition
  echo "=== memory ==="
  grep -E 'MemTotal|MemAvailable' /proc/meminfo
  echo "=== mount / filesystem ==="
  df -h / /data
  echo "=== block metadata access (may be denied without root) ==="
  ls -l /dev/block/by-name 2>&1
} | tee "$HOME/mana_rmx5566_audit/device_readonly.txt"
```

This command only reads Android properties, procfs, filesystem usage and directory metadata. If access to `/dev/block/by-name` is denied, do not try to bypass permissions or obtain root; mark the partition map as unknown and obtain it from an authenticated stock firmware package or official service documentation. Do not run `dd`, `flash`, `fastboot erase`, `fastboot flash`, `parted`, `mkfs`, or any command that writes block devices.

The exact stock OTA/full firmware package and its cryptographic hashes should be preserved separately before any future unlock or test.

## 6. Build path — staged gates

### Gate A — source provenance
- Pin an Android 16 AOSP manifest revision.
- Obtain the exact OEM device tree, kernel source, vendor blobs and firmware for RMX5566RU.
- Record licenses, source commit IDs, firmware versions and SHA-256 hashes.
- Stop if the device code, kernel release or fingerprint do not match.

### Gate B — product integration
- Add a dedicated RMX5566RU product configuration only after its real board files are available.
- Integrate MANA services, launcher, settings and SystemUI as product modules.
- Implement required SELinux policy, permission allowlists, init rc services, overlays and VINTF declarations from verified interfaces.
- Build userspace first; keep the stock kernel and boot chain unchanged.

### Gate C — reproducible image build
- Build in CI from the pinned manifest and recorded proprietary inputs.
- Validate image headers, partition size limits, dynamic partition metadata, AVB descriptors and signing.
- Run static checks, unit tests and VINTF compatibility checks.
- CI must never flash a connected phone.

### Gate D — installation safety
- Establish OEM-supported bootloader unlock eligibility and document that unlocking normally wipes user data.
- Preserve a complete, exact stock restore package and verify its hashes.
- Prove a recovery/restore route on a spare or serviceable test device first.
- Determine the correct installer format only after confirming the device's supported recovery/OTA mechanism.
- Require explicit owner approval before any destructive unlock or installation.
- Keep the release blocked if any recovery, AVB, signing or rollback prerequisite is uncertain.

## 7. What the final one-file package may be

Only after Gates A–D pass may the project produce a signed, device-specific installer (for example, an OTA payload or recovery package in the exact format supported by this model). It must contain the verified images, update metadata, signature, compatibility assertions, checksums, installation instructions and a tested rollback procedure.

A ZIP containing source code or an APK is not a ROM. A generic recovery ZIP is not safe merely because it is a single file. There is no safe universal installer for this phone without the exact partition and boot-chain data.

## 8. Current release decision

**ROM status: BLOCKED / NOT FLASHABLE.**
- No complete AOSP source checkout: BLOCKED
- Exact RMX5566RU device tree: MISSING
- Exact matching kernel source: MISSING
- Matching vendor blobs/HAL/VINTF: MISSING
- Partition map and image metadata: INCOMPLETE
- Bootloader unlock and restore path: NOT VERIFIED
- Device-specific image build: NOT STARTED
- Signed installer ZIP: NOT CREATED

Continue development on source-level MANA services and Android userspace modules. Do not represent a debug APK, source archive or CI success as a bootable operating system.
