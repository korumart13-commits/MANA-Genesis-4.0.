# RMX5566RU kernel source audit

Status: source discovery started; no candidate has been approved for building or flashing.

## Target facts

- Product: realme RMX5566RU / realme C85 5G
- Device codename: RE645F
- SoC: Qualcomm SM6225
- Board/platform: bengal
- Runtime kernel: `5.15.197-android13-8-o-01074-gdfa4acfa411e`
- Firmware fingerprint: `realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys`
- Bootloader: locked; Verified Boot: green

## Candidate rejected pending clarification

Repository: https://github.com/realme-kernel-opensource/realme_C85_5G-AndroidV-kernel-source

The repository name appears to match the C85 5G, but the checked source is not a match for this Qualcomm target. Public device references distinguish RMX5566 (C85, Qualcomm SM6225) from RMX5253 (C85 5G, MediaTek). The candidate source's latest commit explicitly targets RMX5253/MTK. Device naming must follow the model code, not the marketing name supplied in a filename. Additional public device listings support this distinction.\n\nTechnical mismatch details:
- Its top-level kernel Makefile declares Linux 5.15.180, not the device's reported 5.15.197.
- Its latest commit message says it synchronized code for RMX5253 and references an MTK release tag.
- The inspected ARM64 defconfig list did not expose a bengal/SM6225 target configuration.

Therefore this tree is **not approved** as the RMX5566RU kernel source. The repository name alone is not sufficient evidence of device compatibility.

Candidate URL: https://github.com/realme-kernel-opensource/realme_C85_5G-AndroidV-kernel-source/commit/75b7dd046767b696a02a954e0c0a9b53e04d4bad

## Required evidence before kernel work can advance

- OEM source release explicitly mapped to RMX5566/RMX5566RU and the firmware fingerprint above.
- Source kernel release and commit corresponding to the runtime build.
- Matching `bengal` defconfig, board DTS/DTSI and DTBO source.
- Matching vendor module sources/binaries and symbol versions.
- Boot, vendor_boot, init_boot, dtbo and vbmeta header/partition metadata.
- OEM build instructions/toolchain and licensing notices.
- A safe stock restoration/recovery route.

Until these are available, kernel changes remain blocked. The project can continue on AOSP userspace, MANA framework services, AI runtime integration and build infrastructure without fabricating a device kernel.
