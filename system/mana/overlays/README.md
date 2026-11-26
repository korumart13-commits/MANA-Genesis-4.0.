# MANA OS resource overlays

This directory is reserved for Android Runtime Resource Overlay (RRO) packages and product-specific OEM resource overlays. It is intentionally not populated with guessed targets.

## Required target mapping

Before adding an overlay, inspect the exact RMX5566RU firmware package and record:
- package name and APK path for SystemUI;
- package name and APK path for Settings;
- Keyguard implementation and package;
- Recents/Launcher implementation and package;
- OEM File Manager package;
- framework-res package and existing overlay priority;
- vendor/Oplus theme resources and overlayable declarations.

## Planned MANA overlay groups

- `MANAFrameworkOverlay`: framework theme colors, dimensions, status/navigation bar appearance and system dialog resources where overlayable.
- `MANASystemUIOverlay`: status bar, Quick Settings, notifications, volume panel and media controls.
- `MANASettingsOverlay`: Settings category layout, row styles, icons, colors and typography while preserving all OEM pages.
- `MANAKeyguardOverlay`: lock screen clock, notification spacing, shortcuts and wallpaper presentation without replacing credential verification.
- `MANARecentsOverlay`: task card appearance and overview spacing while preserving Android task management.
- `MANAFilesOverlay`: only if the installed OEM file manager exposes supported overlayable resources; otherwise build a separate MANA Files front end and leave the OEM app installed.

## Safety and compatibility

Do not guess target package names or resource IDs from a different Realme model/build. Do not add `overlayable` bypasses or framework patches that weaken platform security. Build and validate each overlay against the exact Android 16/Oplus target. If the OEM component is not overlayable, use a maintained source-level integration or leave that component unchanged until exact sources are available.
