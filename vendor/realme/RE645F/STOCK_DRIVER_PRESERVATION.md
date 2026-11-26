# RE645F stock driver preservation inventory

## Source identity captured from the running device

- Device: RMX5566RU / RE645F
- Android: 16 / API 36
- Stock build fingerprint: realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys
- Runtime kernel: 5.15.197-android13-8-o-01074-gdfa4acfa411e
- Vendor release: V.2f025ac-8895bb-9a8318
- Vendor module build provenance: OPlus Android 13 vendor_dlkm build 1792177965740
- OTA manifest identity: RMX5566_11.F.03_2030_202610170441.37.915645cf

These values identify the currently installed vendor/kernel baseline. They do not prove that a separately compiled kernel or module is compatible with it.

## Existing stock module inventory

The device audit previously surfaced these stock module names (the list is an inventory, not a repository of their binary contents or source code):

- GPU: msm_kgsl.ko
- Camera: camera.ko
- Wi-Fi: qca_cld3_wlan.ko
- Audio DSP: adsp_loader_dlkm.ko, q6_dlkm.ko
- Sensors: oplus_sensor.ko
- Charging/power: oplus_chg.ko, oplus_sched.ko
- Touch: `focaltech_fts.ko`, `synaptics_tcm_ts.ko`

## Source provenance warning

The previous project audit did not identify any MANA-created driver source files or a compiled MANA driver package. The names above are stock modules observed on the device; `modules.load` and `modules.dep` were not readable, and exact binary hashes/source paths were not supplied. Do not call these MANA drivers or assume they can be rebuilt from their names.

## Preservation strategy

1. Keep the currently installed stock boot, vendor_boot, init_boot, vendor, odm, vendor_dlkm and system_dlkm inputs unchanged for the initial MANA userspace integration.
2. Treat the exact stock module binaries, their vermagic, signer, dependency graph, exported symbols and hashes as the compatibility reference.
3. Retain matching OEM HAL/VINTF/init/firmware dependencies. Do not copy a module from a different C85 variant or a different kernel release.
4. Add a read-only extraction and hashing step from an owner-provided stock firmware/device dump before building a device product.
5. Reuse a stock module only in a build whose kernel ABI and vendor interface are demonstrably compatible. Otherwise retain the stock partition intact rather than forcing module loading.
6. Do not patch proprietary driver binaries or claim a new MANA driver implementation from a module inventory.

## Important distinction

The project repository currently contains this preservation inventory and candidate-source audit, not the actual .ko binaries, their matching source tree, a verified RE645F device tree, or a compiled driver release. Earlier device data established the installed stock kernel/module identities; it did not create a new kernel driver build. Exact module files, hashes and build provenance must be present before they can be packaged into a ROM.

## Current integration status

- [CONFIRMED] Running device kernel release and stock fingerprint.
- [RECORDED] Several stock vendor_dlkm module names and OPlus vendor build provenance.
- [MISSING] Binary module payloads, exact module hashes/vermagic/dependencies, matching kernel source/defconfig/DTS/DTBO, exact vendor blobs and a verified product build.
- [BLOCKED] Rebuilding or replacing the kernel/driver set for RE645F.