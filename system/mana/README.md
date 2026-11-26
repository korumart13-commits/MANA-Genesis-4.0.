# MANA OS system layer — Android-preserving firmware shell

**Base decision:** keep the existing compatible Realme Android 16 platform. MANA OS is its branded visual/system-integration layer; do not replace Android, kernel, radio or OEM hardware stack unless exact device evidence makes that unavoidable.

MANA OS owns the user-facing shell: MANA launcher, app drawer, theme resources, Settings presentation, Control Center design, and later OEM/SystemUI/Keyguard/Recents/Files overlays. It preserves the stock Android/OEM packages and all user-installed apps.

The separate `com.mana.genesis` application owns the AI agent, local model, avatar, voice/personality, memory, micro-world, learning, messaging and game automation. The firmware must not package a duplicate AI runtime.

## Firmware modules

- `ManaLauncher`: HOME launcher and MANA OS home/app drawer. It enumerates installed launchable apps and uses their actual app icons. It is designed to be selected through Android's HOME role; the OEM launcher remains installed.
- `ManaSettings`: MANA-branded settings hub and navigation into Android's protected settings pages.
- `ManaControlCenter`: MANA-styled quick-access hub and optional user-added Quick Settings tile. It is not yet a replacement for the real SystemUI notification shade.
- `ManaAssistant`: source-only prototype for a future narrow platform bridge; it is deliberately excluded from `PRODUCT_PACKAGES` until it is reduced to an authenticated OS-to-app interface with no AI runtime.
- `ManaBrain` source modules: not included in the firmware product image; reserved for the standalone MANA application/runtime work.

## Visual implementation plan

See `os/MANA_OS_VISUAL_DESIGN_AND_ANDROID_OVERLAY_RU.md` and `system/mana/overlays/README.md`. The design combines broad usability cues from classic/modern Samsung interfaces with restrained iOS 15/iPhone 13-style spacing and translucency, using original MANA assets and layouts rather than copying proprietary artwork.

## Platform integration requirements

Prepare the standalone `com.mana.genesis` app to use supported Android assistant roles and public APIs. A future narrow Binder/AIDL bridge may expose purpose-specific platform functions after identity, signature, permission and SELinux validation. Keep Android permission enforcement, AppOps, SELinux, Verified Boot, AVB and audit logs enabled.

## Current implementation state

Launcher and Settings have MANA dark-navy/cyan resources and theme references; Control Center has a matching theme. The Launcher now displays actual installed application icons and opens the standalone `com.mana.genesis` app. The firmware product fragment includes Launcher, Settings and Control Center only; it does not include ManaAssistant or MANA Brain runtime. These are source changes, not a complete RMX5566RU firmware build.
