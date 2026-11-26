# MANA kernel patch track

This directory stores reviewable MANA-only kernel patchsets. It is deliberately separate from the device's OEM kernel tree.

## Allowed patch shape

- A patchset may only add new source files below `drivers/misc/mana/`.
- Existing kernel files, Kconfig, Makefiles, boot code, memory management, SELinux/LSM, AVB, modem, vendor modules and device-tree files are not modified by this autonomous track.
- Binary patches are forbidden.
- Every patchset must be reviewed and validated against the exact matching OEM source before compilation.
- No CI job in this repository flashes a device or publishes a bootable kernel image.

## Why this is review-only today

The phone reports Linux `5.15.197-android13-8-o-01074-gdfa4acfa411e`. Publicly inspected candidates do not match the exact RMX5566RU source/kernel release. The kernel self-development track therefore prepares patchsets only. It does not claim that a candidate driver compiles or works on the phone.

The MANA Brain, inference engine, memory, avatar, games, micro-universe and user-facing modules belong in Android userspace. They do not need kernel privileges to self-develop. A kernel-side component should be introduced only for a demonstrated low-level requirement after exact source and ABI compatibility are established.

The gate is implemented by `tools/validate_mana_kernel_patch.py` and is exercised by CI.
