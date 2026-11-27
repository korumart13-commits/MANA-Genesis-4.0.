# Android 16 platform source

MANA Genesis 4.0 is an AOSP-derived OS. This workstream starts from the official Android 16 platform manifest and then adds only verified device/vendor projects.

## Reproducible source bootstrap

On a Linux build host with the Android `repo` tool, Git, curl, Python 3 and at least 250 GiB of free workspace storage:

```sh
bash platform/bootstrap_aosp16.sh /path/to/aosp-mana
```

The script initializes the official `android16-release` manifest, downloads the MANA local manifest **before** the first sync, syncs the pinned MANA source snapshot, and records the resolved AOSP manifest commit and MANA commit in `mana-build-lock.json`. Set `MANA_SYNC_JOBS` or `MANA_MIN_FREE_GIB` to tune host resource checks.

This creates a source checkout, not a ROM. AOSP source and build output require substantial host storage, memory, bandwidth and build time; a phone-only Termux environment is not a supported AOSP build host.

## MANA platform module build

After syncing, pass a lunch target that is actually listed by that exact AOSP checkout:

```sh
bash vendor/mana/genesis/platform/build_mana_platform_modules.sh <verified-aosp-target>
```

The guarded script selects the lunch target and builds the source-backed AIDL interface, Brain service and host policy tests, Settings and Control Center through Soong. A successful module build proves compilation for the selected AOSP target only; it does not establish RMX5566RU hardware compatibility.

## Common MANA product fragment

A verified AOSP product makefile can include:

```make
include vendor/mana/genesis/platform/mana_aosp16_common_product.mk
```

This common fragment sets MANA product identity, declares the source-backed Soong namespaces and includes the MANA system package list. It deliberately does not define a device product, kernel, fstab, vendor files or partition configuration.

## Device integration gate

Do not add an RMX5566RU product/lunch target until the exact device tree, matching vendor blobs, kernel source/config and VINTF compatibility data are available. Shared SM6225 support from another brand is useful research, not a drop-in device port.

The target phone reports realme RMX5566RU / RE645F, Qualcomm SM6225 (bengal), Android 16, kernel 5.15.197 and a locked bootloader. No exact matching device tree, kernel source/config/DTS/DTBO, vendor set or verified stock restore package has been established. The C85 5G / MediaTek target is not compatible.

## Build outputs

The MANA build pipeline must build Android platform targets and MANA system packages, not mislabel a separate launcher APK as an OS. Image packaging and signing remain disabled until device partition metadata, AVB chain, kernel and vendor compatibility are verified. No boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA image may be produced for flashing from an unverified target.
