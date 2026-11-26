# RE645F boot-chain and EFI reference policy

Status: research and integration constraints. This is not a bootloader, device tree, BoardConfig, flash recipe or verified boot image.

## Target

- Device: realme RMX5566RU / RE645F (C85 4G)
- SoC family: Qualcomm SM6225 / bengal
- ABI: arm64
- Reported Android base: Android 16 / API 36
- Reported kernel: 5.15.197
- Existing state recorded in the device profile: A/B, dynamic partitions, locked bootloader and green Verified Boot.

These facts do not reveal every partition size, image header, signing key, rollback index, bootloader implementation detail or firmware dependency. Those must come from exact-device evidence.

## EFI is not the Android boot-image format

UEFI/EFI executables are PE/COFF applications loaded by a UEFI firmware environment. A generic PC x86-64 EFI bootloader, GRUB EFI binary, Clover/OpenCore file or another phone's EFI payload is not a substitute for an Android ARM64 boot image.

Some ARM devices and development boards can implement UEFI, and some projects use EDK2, but that does not establish that this particular realme firmware exposes a UEFI boot service or can load arbitrary EFI applications. The current MANA device profile explicitly treats the EFI/firmware interface as unverified. Therefore:

- Do not download and flash generic EFI binaries.
- Do not add EFI files to boot, vendor_boot, init_boot, recovery or vbmeta.
- Do not infer UEFI support from the CPU being ARM64.
- Do not use another Qualcomm model's EFI payload or boot image.
- EFI research may inform general firmware architecture only; it is not an implementation input unless exact-device firmware evidence proves a supported interface.

## Android boot components to investigate

The Android 16 device bring-up must audit the actual target's use and versions of:

- Boot ROM and OEM bootloader chain.
- A/B slot metadata and boot-control behavior.
- boot and vendor_boot images and their header versions.
- init_boot, if present on this device.
- dtbo and device-tree handoff.
- recovery or recovery-as-ramdisk arrangement.
- vbmeta chain, AVB descriptors, rollback indexes and signing policy.
- super/dynamic partition metadata and logical partition groups.
- vendor, odm, product and system_ext boundaries.
- kernel image, config, DTS/DTSI, DTBO, modules and firmware.
- fstab, first-stage init, init rc services and VINTF manifests.

Do not assume every listed partition exists on this target. Record the observed partition map and image headers before writing any product configuration.

## Reference hierarchy

Use sources in this order:

1. Exact RMX5566/RMX5566RU firmware package and matching OEM source release.
2. Runtime read-only device metadata captured from this exact phone.
3. Official AOSP Android 16 boot-image, partition, AVB, VINTF and init documentation.
4. Qualcomm platform documentation for the exact SM6225/bengal generation, where lawfully available.
5. Closely related devices only as research leads, never as copy-and-flash templates.

Every imported template must be labelled with its original device, SoC, kernel version, Android release, license and the exact fields that are known to match. Unknown fields stay unset rather than guessed.

## MANA integration strategy

1. Keep the OEM boot chain, verified boot and stock recovery path unchanged during userspace development.
2. Build and test MANA apps/services independently on Android first.
3. Obtain exact OEM sources and reconstruct a matching device tree and vendor interface.
4. Reproduce the stock kernel and image metadata before any MANA changes.
5. Add MANA product packages and system services while retaining required OEM HALs and firmware.
6. Keep SELinux enforcing and use narrowly scoped service domains and Binder policies.
7. Design AVB signing, A/B OTA and rollback only after the real key/partition model is known.
8. Test a recovery-capable image in a controlled environment before any owner-device installation.

## Prohibited shortcuts

- No generic EFI/GRUB image in place of Android boot images.
- No guessed fstab, partition size, boot header, kernel cmdline, DTB, DTBO, AVB key or rollback index.
- No cross-flashing from C85 5G / MediaTek RMX5253 or unrelated SM6225 phones.
- No disabling AVB or SELinux to make an unverified image boot.
- No automatic flashing, bootloader unlocking or OTA installation from an unverified build.
- No claiming an EFI handoff, kernel boot, recovery, or ROM build without logs from the exact target.

## Evidence checklist

- [ ] Exact stock firmware build and fingerprint archived with provenance.
- [ ] Read-only partition map, slot state and dynamic partition metadata captured.
- [ ] boot/vendor_boot/init_boot/recovery/dtbo/vbmeta headers inspected where present.
- [ ] Exact OEM kernel source, defconfig, DTS/DTSI and modules matched to 5.15.197.
- [ ] Exact VINTF manifests/matrices and vendor/ODM blobs inventoried.
- [ ] Init/fstab/SELinux dependencies mapped.
- [ ] AVB chain and rollback policy understood without exposing private keys.
- [ ] Stock restore/recovery method tested.
- [ ] MANA image build is reproducible and passes static, unit, VTS/CTS-relevant and hardware tests.
