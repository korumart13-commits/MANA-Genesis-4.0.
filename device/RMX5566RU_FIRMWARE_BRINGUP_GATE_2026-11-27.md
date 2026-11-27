# RMX5566RU firmware bring-up gate — 2026-11-27

Target: realme C85 4G, RMX5566/RMX5566RU, RE645F, Qualcomm SM6225/bengal, Android 16, runtime kernel 5.15.197.

## Current engineering state

The repository contains:
- A pinned Android 16 AOSP source-bootstrap path.
- MANA's source-level AIDL interface, Brain Binder service, product package fragment, Settings and Control Center modules.
- A guarded Soong build script for the source-backed MANA platform modules.
- Gradle/JVM CI evidence for the application/runtime and system-core layers.

The AOSP tree has not yet been synced and built in this project environment. No MANA Android platform image or device image is claimed.

## Device-specific gates — all remain open

| Gate | Required evidence | Current status |
|---|---|---|
| Device tree | Exact RMX5566RU/RE645F BoardConfig, product/device makefiles, init, fstab and overlays | Not verified |
| Kernel | Exact OEM 5.15.197 source, defconfig, DTS/DTSI, DTBO and module ABI | Not verified |
| Vendor | Matching proprietary blobs/firmware and provenance | Not verified |
| VINTF | Device manifest and framework/device compatibility matrix pass | Not verified |
| Partitions | Exact boot/init_boot/vendor_boot/dtbo/vbmeta/super metadata and A/B layout | Not verified |
| AVB/signing | Verified chain, rollback indices and supported signing path | Not verified |
| Recovery | Complete owner-usable stock restore package and tested recovery route | Not verified |
| Hardware validation | Boot, display, touch, audio, camera, radio, Wi-Fi, Bluetooth, sensors, charging, suspend/resume | Not run |

## Release policy

Do not create a fake product target, borrow a different phone's kernel, or emit flashable partition images to make the build appear complete. Do not unlock or flash the owner's phone as a way to discover missing metadata. Keep the existing stock boot chain and data intact.

The next legitimate milestone is a synced Android 16 AOSP checkout and successful Soong compilation of MANA's source modules. Device ROM integration starts only after the exact source/vendor/recovery evidence is obtained. A generic AOSP build or module compile is not a flashable RMX5566RU firmware.
