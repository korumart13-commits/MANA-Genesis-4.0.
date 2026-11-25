# MANA Genesis 4.0 — Android application sources

This directory contains the first Android-framework-facing source modules for the MANA product image.

## Packages

- **ManaLauncher** (com.mana.launcher): HOME activity, MANA branding, launches the MANA assistant and lists installed launchable apps.
- **ManaAssistant** (com.mana.assistant): MANA-branded assistant shell. It truthfully reports that this Soong shell is not yet connected to MANA Brain; it does not fake inference or transmit prompts.
- **ManaSettings** (com.mana.settings): MANA-branded settings hub that opens Android settings panels for network, Bluetooth, display, sound, battery, app permissions and MANA's own permissions.
- **ManaControlCenter** (com.mana.controlcenter): MANA-branded quick-access hub for Android Wi-Fi, Bluetooth, mobile network, display, sound, battery, location and notification settings.

## Build integration

All four are Soong android_app product modules. Include system/mana/mana_product.mk from the eventual verified Android 16 product makefile to add them to PRODUCT_PACKAGES.

They remain non-privileged product apps: no platform certificate, root access, hidden API, privileged permission or direct radio toggle is assumed. Settings and Control Center open Android's own protected settings panels; they do not pretend to be the privileged Quick Settings/SystemUI implementation.

## Remaining integration gates

- Exact AOSP checkout and verified RMX5566RU/RE645F product and device tree.
- Binder connection from the Soong assistant shell to the already-tested MANA Brain engine.
- SystemUI/Quick Settings replacement with correct platform signing and SELinux policy; the current Control Center is a safe settings launcher, not a replacement for Android SystemUI.
- Real Android product build and on-device validation.
- OEM kernel source, matching DTS/DTBO, vendor modules, VINTF/HAL and verified recovery/rollback path before any flashable image is attempted.

These are integrated source modules intended for the future MANA product image. No separate APK release or firmware flash is initiated by this workflow.