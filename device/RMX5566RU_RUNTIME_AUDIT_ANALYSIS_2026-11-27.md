# RMX5566RU runtime audit analysis — 2026-11-27

## Input and integrity

- User-provided archive: `mana_rmx5566_audit_20261127_022142.zip`
- Archive SHA-256: `c0406a52808fef8328c49b179b4b20a9f9222f42a76c0a85d20538141c99dff8`
- Collector: `MANA RMX5566 READ-ONLY AUDIT v2`
- Scope: unprivileged metadata-only collection. No root, unlock, writes, partition-content reads, mounts, or flashing.

## Confirmed runtime properties

| Property | Observed value |
|---|---|
| Manufacturer / model | realme / RMX5566 |
| Device / product | RE645F / RMX5566RU |
| SoC platform | Qualcomm SM6225 / bengal |
| Hardware / ABI | qcom / arm64-v8a |
| Android / SDK | 16 / 36 |
| Build | RMX5566_16.0.5.1310(EX01) |
| Build fingerprint | realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys |
| Vendor API / VNDK | 33 / 33 |
| First API level | 35 |
| Kernel | 5.15.197-android13-8-o-01074-gdfa4acfa411e |
| Verified Boot | green |
| Bootloader state | locked (flash.locked=1; vbmeta device_state=locked) |
| AVB version | 1.2 |
| Slot | _a |
| Dynamic partitions | true |
| Encryption | encrypted, file-based |

## VINTF findings from the actual archived XML

The archive contains readable vendor, ODM, system and system_ext VINTF XML files. XML files were parsed successfully.

- Vendor device manifest: meta-version 5.0, type=device, `target-level=7`.
- Vendor manifest declares 49 top-level HAL entries and a SELinux policy version of 33.0.
- Vendor device compatibility matrix: version 5.0, type=device; 8 top-level HAL requirements, plus vendor-ndk and system-sdk entries.
- ODM manifest fragments add product-specific Oplus and Qualcomm interfaces. One subsystem fragment declares `vendor.oplus.hardware.subsys_interface.subsys` version 5.
- System framework manifest: version 9.0, type=framework.
- The collected system framework compatibility matrices include levels 5, 6, 7 and 8 (among other dated matrix files).

Important: the vendor manifest's target FCM level 7 is an observed device declaration, not proof that an arbitrary Android 16 framework image is compatible. A complete VINTF reconciliation must compare the exact target framework compatibility matrix and all device requirements using AOSP libvintf/assemble_vintf. Do not infer compatibility from API level or the presence of XML alone.

## Explicitly inaccessible / incomplete data

| Item | Archive result | Interpretation |
|---|---|---|
| `/sys/class/block` | Permission denied | No partition inventory obtained |
| Partition sizes | Empty | Unknown; do not infer layout |
| `/proc/cmdline` | Empty | Boot arguments unknown |
| SELinux runtime state | getenforce and enforce file denied | Unknown; do not label enforcing/permissive |
| Kernel config | `/proc/config.gz` and headers unreadable | Config unknown |
| Device-tree metadata | Empty | Model/compatible not collected |
| Vendor module inventory | Collector shell syntax error | Collection failed; no module conclusions |
| Tool inventory | getprop, uname, ls, lsmod, modinfo, readlink, find, sha256sum, zip available | Tool presence does not imply permission to inspect protected data |

The collector defect in the module-inventory subsection is a script bug and should be fixed separately. It is not evidence that the phone has no modules.

## Engineering interpretation

1. The device identity and current firmware baseline are now strongly pinned to RMX5566RU / RE645F / SM6225.
2. The real vendor and ODM manifests are valuable inputs for a future device tree. They do not provide the device tree, kernel source, matching defconfig/DTS/DTBO, vendor blobs, proprietary firmware, partition map, or AVB signing/rollback configuration.
3. Vendor API/VNDK 33 and first API level 35 are recorded as runtime properties. They are not substitutes for VINTF compatibility testing.
4. Verified Boot is currently green and the bootloader is locked. Preserve that state; no unlock or flashing instructions are authorized by this audit.
5. The current source discovery result remains unchanged: no exact matching public RMX5566RU kernel/device tree has been verified.

## Next safe work

- Add automated XML well-formedness and inventory checks for the archived VINTF source set.
- Fix the Termux collector's module inventory syntax and add a safe retry for read-only sysfs paths; retain explicit UNKNOWN/permission-denied results.
- Continue locating the exact OEM kernel source and matching device tree, using the exact fingerprint and kernel release as search keys.
- Obtain authoritative partition/AVB metadata and stock restore package information before any image design.
- Run a real AOSP `assemble_vintf`/libvintf compatibility check only after the exact Android 16 framework source and proposed device manifests are pinned.

## Release gate

This audit is evidence for device research only. It does not authorize or enable a flashable ROM, recovery, boot, vendor_boot, dtbo, vbmeta, super, or OTA package. No device image should be built for flashing until the exact source, blobs, VINTF match, partition/AVB chain and tested stock recovery path are established.
