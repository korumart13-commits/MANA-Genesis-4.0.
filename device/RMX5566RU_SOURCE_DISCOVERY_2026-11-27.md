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

### Android 16 kernel compatibility interpretation

The reported `5.15.197-android13-8-o-01074-gdfa4acfa411e` release is in the Android 13 / 5.15 kernel lineage. AOSP's Android 16 kernel compatibility table lists `android13-5.15` among supported kernel branches. This is a useful platform-level compatibility signal, but it does **not** identify the exact OEM kernel source, vendor module ABI, defconfig, DTS/DTBO, or prove this phone can boot an arbitrary Android 16 image. The exact OEM kernel and matching vendor modules are still required. See the [AOSP Android common kernel compatibility table](https://source.android.com/docs/core/architecture/kernel/android-common).

## Next device-audit stage

The two screenshots strengthen the identity and firmware baseline, but they do not reveal the partition layout, boot chain, VINTF declarations, vendor interface level, exact kernel config, device tree or proprietary firmware inventory. The next safe audit is a read-only Termux/ADB property and partition inventory. It must not unlock, root, modify or flash the phone.

Collect, where Android permits:

- `getprop` output (especially fingerprint, incremental build, product/vendor/boot properties, ABI and security patch properties).
- `/proc/cmdline`, `/proc/version`, `/proc/partitions` and block-device symlink names.
- `/vendor/etc/vintf/manifest.xml`, framework compatibility matrix and vendor API level, if readable.
- `/sys/fs/selinux/enforce` and verified-boot state properties.
- Bootloader unlock eligibility and official stock firmware/restore package availability, without changing device state.

Do not upload personal identifiers, serial numbers, IMEI, Android ID, Wi-Fi identifiers, account data or tokens. Redact those fields before sharing diagnostics.


### Phone-side collection without a computer

The enhanced read-only Termux collector is `device/collect_rmx5566_readonly_audit.sh`. Version 2 collects a whitelist of non-personal build properties, kernel and command-line metadata, partition names/sizes where readable, SELinux state, readable VINTF XML, device-tree metadata and kernel-module inventory. It creates a ZIP archive in Termux home.

On the phone:

```sh
pkg update
pkg install curl zip
curl -fL "https://raw.githubusercontent.com/korumart13-commits/MANA-Genesis-4.0./main/device/collect_rmx5566_readonly_audit.sh" -o rmx5566_audit.sh
bash rmx5566_audit.sh
```

The script prints the full archive path, similar to `~/mana_rmx5566_audit_YYYYMMDD_HHMMSS.zip`. Attach that ZIP here after reviewing it. The script does not request root, unlock the bootloader, read partition contents, mount filesystems, change properties or flash anything. Some paths may be inaccessible to ordinary apps; this is expected and should remain marked unknown rather than bypassed.

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


# RMX5566 audit v3 results — owner archive 2026-11-27 04:37 +03

The owner supplied and the project reviewed `mana_rmx5566_audit_20261127_043744.zip` generated by the read-only Termux collector v3. This section records only fields present in that archive.

### Confirmed runtime properties

- `ro.product.manufacturer=realme`
- `ro.product.model=RMX5566`, `ro.product.device=RE645F`, `ro.product.name=RMX5566RU`
- `ro.product.board=bengal`, `ro.board.platform=bengal`, `ro.soc.model=SM6225`, `ro.hardware=qcom`
- `ro.product.cpu.abi=arm64-v8a`
- Android 16 / API 36; build `RMX5566_16.0.5.1310(EX01)`
- Build fingerprint: `realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys`
- Android/vendor security patch: `2026-10-01`
- Vendor API 33, VNDK 33, first API 35
- Kernel: `5.15.197-android13-8-o-01074-gdfa4acfa411e`
- Boot properties report verified boot `green`, flash locked, vbmeta device state `locked`, AVB 1.2, active slot `_a`, dynamic partitions enabled, file-based encryption enabled.

### New interface evidence

The archive contains readable vendor, ODM, system and system_ext VINTF manifests/matrices. The vendor manifest includes Qualcomm radio, GNSS, display, KeyMint, sensors, thermal, USB, Wi-Fi, media codec and power interfaces; ODM manifests expose numerous Oplus-specific AIDL/HIDL services including sensors, charger, display, fingerprint, NFC, game optimization and touch. The full XML files are preserved in the owner audit archive; this summary does not claim that a candidate AOSP framework has passed VINTF compatibility.

The archive lists vendor_dlkm kernel modules, including Qualcomm audio, camera, KGSL/GPU, WLAN, USB, memory and performance modules plus Oplus BSP, charging, connectivity, display/touch and sensor modules. These names are useful inventory evidence only; module binaries, symbol versions and ABI compatibility were not extracted or validated.

### Access limitations (not collector failure)

- `/sys/class/block` was denied to the unprivileged Termux process; no partition link inventory or sizes were returned.
- `/proc/cmdline` and partition-size output were empty.
- SELinux status and policy version were not readable through the attempted interfaces.
- `/proc/config.gz` and kernel headers archive were not readable.
- Device-tree metadata output was empty.
- The collector intentionally did not read partition contents, request root, unlock, mount, write or flash.

Therefore the audit improves the known VINTF and module inventory, but does not close the partition map, boot image layout, kernel config, DTS/DTBO, AVB chain or restore-package blockers. Do not infer these from mount paths or module filenames.

### Engineering consequence

Keep the existing exact target mapping and continue source discovery for the matching OEM kernel/device tree/vendor release. Treat the runtime fingerprint and VINTF XML as inputs for later compatibility analysis. Do not synthesize BoardConfig/fstab/partition sizes or flash images from this metadata alone.

