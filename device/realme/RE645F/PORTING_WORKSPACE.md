# RE645F porting workspace — next executable steps

This workspace uses pinned public OEM sources rather than floating branches. It is intentionally a **source-acquisition and validation stage**, not a bootable ROM declaration.

## Run source acquisition

From the repository root on a Linux build host:

```bash
bash device/realme/RE645F/fetch_oem_candidates.sh --all
bash device/realme/RE645F/report_port_gates.sh
```

The script fetches the exact audited commits into:
- `kernel/realme/c85-family-kernel`
- `vendor/realme/c85-family-kernel-platform`

It refuses to overwrite existing non-matching directories and verifies the fetched commit hashes. It does not flash, unlock, or write to a phone.

## What these sources solve

- Provides a real Qualcomm Bengal OEM kernel source candidate.
- Provides the Oplus Moscow/Khaje overlay source family, including the project-ID 25697 candidate.
- Makes every source revision reproducible and reviewable.

## What remains a hard blocker

1. The candidate kernel tree reports Linux 5.15.180, while RMX5566RU reports 5.15.197. Exact source/config/module ABI match is not established.
2. The kernel source commit metadata points to a different C85-family RMX release; no proof yet that its board configuration is identical to RE645F.
3. The 25697 overlay does not prove the exact panel, touch, camera, NFC, sensor and charging sub-variants.
4. The repository does not contain verified RMX5566RU vendor/ODM proprietary blobs and firmware set.
5. The exact device fstab, partition map, dynamic partition groups, boot image header/cmdline, AVB descriptors and signing/recovery/restore process remain unverified.

## Build policy

Until those gates are closed, no boot, vendor_boot, dtbo, vbmeta, super or OTA image is emitted. This is not excessive caution: those images contain device-specific boot and storage assumptions, and a wrong image can make recovery substantially harder.

## Next porting work

- Compare the full target audit against OEM source project IDs and overlay includes.
- Identify the precise matching kernel release and defconfig; check module ABI and exported symbols.
- Extract the exact stock firmware payload and derive the device's partition/VINTF/firmware requirements from it.
- Build a non-flashable AOSP product target only after product makefiles and BoardConfig values are evidence-backed.
- Integrate MANA System UI/launcher/Brain into the Android 16 product once the product target exists.
- Test recovery and restore route before any device image is labelled installable.

The Android 16 target identity is independently cross-checked by public device catalog evidence, but the runtime kernel value comes from the owner's existing audit. See [OEM_SOURCE_MATRIX.md](OEM_SOURCE_MATRIX.md).
