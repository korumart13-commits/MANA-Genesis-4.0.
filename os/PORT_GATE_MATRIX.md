# MANA Genesis 4.0 — RE645F port gate matrix

This matrix is the authoritative distinction between source work, placeholders and device-ready components.

| Area | State | Evidence / exit condition |
|---|---|---|
| Runtime device identity | CONFIRMED | RMX5566RU / RE645F, SM6225/bengal, Android 16/API 36 |
| Boot security | CONFIRMED | Locked bootloader, green AVB; preserve stock chain |
| Device tree directory | FOUNDATION ONLY | Directory contract documented; no buildable product files |
| Product makefiles / BoardConfig | MISSING | Must be derived from matching OEM source and verified metadata |
| Partition/fstab | BLOCKED | Exact sizes, groups, filesystems and boot metadata not established |
| Vendor HAL family | PARTIAL | Qualcomm/OPlus families known; exact XML/version/instance inventory incomplete |
| Vendor blobs | MISSING | Matching firmware extraction and provenance required |
| Kernel source | BLOCKED | No exact 5.15.197 OEM source/defconfig/DTS approved |
| AOSP 16 baseline | DOCUMENTED | Manifest tag pinned in build plan; source checkout not present |
| MANA JVM core | EXISTING SOURCE | Validate against current implementation ledger and CI |
| MANA Gradle prototype | EXISTING SOURCE | Not yet unified with Soong product modules |
| MANA Soong shells | EXISTING SOURCE | Non-privileged shells; not a complete SystemUI replacement |
| Full AOSP product build | NOT STARTED | Requires synced AOSP and valid product target |
| Bootable RE645F image | NOT BUILT | Requires all device, vendor, kernel, AVB and recovery gates |

## Gate policy

- Never convert PARTIAL or FOUNDATION ONLY into CONFIRMED without source evidence.
- Never use another phone's kernel, device tree, VINTF or proprietary blobs as a substitute.
- Never infer partition sizes or boot image headers from the marketing model.
- Keep all device image and OTA workflows disabled until every critical gate has evidence and a tested restore path.
- The owner’s locked bootloader and green AVB state are preserved; no unlock/flash action is part of this milestone.
