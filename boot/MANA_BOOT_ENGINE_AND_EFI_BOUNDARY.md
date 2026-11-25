# MANA Genesis 4.0 — boot engine and firmware boundary

## What is implemented

`ManaBootEngine` is an executable Kotlin userspace boot coordinator in `os/core`. It:
- waits for Android to launch the MANA process;
- checks product, device, Android API, ABI, kernel release prefix and Verified Boot state;
- refuses to start MANA modules if any identity is missing or mismatched;
- starts the existing `ManaSystemCore` dependency graph only after the checks pass;
- reports module health and can stop the MANA userspace services.

The engine is designed against the recorded runtime target RMX5566RU / RE645F, Android API 36, arm64-v8a, Linux 5.15.197 and green Verified Boot. Runtime values must be read from Android; the defaults are not a substitute for live evidence.

## EFI / firmware decision

EFI is not a Linux kernel component. An EFI executable cannot simply be placed inside an Android kernel and thereby become the phone's firmware or boot engine. The phone currently boots through its OEM Qualcomm firmware/boot chain and Android boot image. The exact internal boot stages, EFI/EDK2 implementation status, firmware interfaces, boot image headers and signing chain have not been verified from matching OEM source.

For that reason this release deliberately reports `FirmwareInterfaceStatus.NOT_VERIFIED`. It does not ship an EFI binary, replace the OEM bootloader, write boot partitions, unlock the bootloader, or bypass AVB. Guessing an EFI payload or flashing one would risk making the phone unbootable.

## What this is not

- not a replacement Linux kernel;
- not Android init or a pre-boot firmware service;
- not a flashable ROM or OTA;
- not proof that MANA owns privileged SystemUI, telephony, camera or power services.

## Required evidence before a real firmware build

1. Exact OEM firmware/source matching the recorded RMX5566RU build fingerprint.
2. Verified boot-chain source and supported Qualcomm boot interface for this board.
3. Matching kernel source, defconfig, DTS/DTBO and vendor modules.
4. Exact boot/vendor_boot/init_boot/vbmeta and dynamic partition metadata.
5. Device-specific AVB signing, rollback, recovery and tested stock restore path.
6. Reproducible Android 16 AOSP product build and hardware validation.

Until all six are available and tested, the safe deliverable is MANA's userspace engine and Android integration, not a purported EFI/ROM image.
