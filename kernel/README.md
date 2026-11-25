# MANA kernel integration

MANA OS will use a device-matched Linux kernel as its hardware kernel. The first target is the stock device's Linux 5.15.197 Android kernel on Qualcomm SM6225 / bengal / RE645F.

A kernel is not safely replaceable based only on the SoC name. The exact board DTS, defconfig, vendor modules, boot image header, vendor_boot layout, DTBO, module ABI, signing and vendor interfaces must match the phone's firmware.

## Current state

- Device runtime reports: `5.15.197-android13-8-o-01074-gdfa4acfa411e`.
- Exact OEM source commit for this firmware fingerprint: not yet verified.
- Exact defconfig and DTS/DTBO: not yet verified.
- Vendor modules and module ABI: not yet verified.
- Boot/vendor_boot header and partition metadata: not yet verified.
- No custom kernel image has been built.

## Kernel integration policy

1. Keep the stock kernel and boot chain as the recovery baseline.
2. Obtain the exact OEM GPL source release matching the device build/fingerprint.
3. Compare kernel release, source commit, config, DTS and module versions with the running device.
4. Build the unmodified source first and compare outputs/metadata.
5. Apply MANA-specific changes only as small, reviewable patches.
6. Run static checks and build in CI; do not flash during CI.
7. Require a verified recovery path, unlocked bootloader, correct AVB/signing plan and explicit owner approval before any device installation.

Do not use a kernel from another SM6225 phone as a drop-in replacement. Shared SoC support does not imply compatible board wiring, panel, touch, camera, power, modem or partition layout.
