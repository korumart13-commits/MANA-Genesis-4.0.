# MANA OS — firmware application modules

This directory contains the firmware-facing UI and narrow system integration modules. It does **not** contain the autonomous MANA AI product.

## Packages

- **ManaLauncher** (`com.mana.launcher`): MANA OS HOME activity. Its Android label is **MANA OS** and it now has the first dark-navy/cyan resource theme.
- **ManaAssistant** (`com.mana.assistant`): optional system entry-point/Binder boundary for the separate MANA app. It must remain a thin bridge, not a second AI agent or local model runtime.
- **ManaSettings** (`com.mana.settings`): MANA OS settings hub. Its Android label is **MANA OS Settings** and it shares the initial MANA palette.
- **ManaControlCenter** (`com.mana.controlcenter`): current safe quick-access hub. It is not yet a replacement for privileged Android SystemUI or Quick Settings.

## Product integration

Include `system/mana/mana_product.mk` from the eventual verified Android 16 device product makefile to add the modules to `PRODUCT_PACKAGES`. No valid RMX5566RU product makefile exists yet, so the fragment is not currently consumed by a device build.

## Separate MANA application

The standalone application package is expected to be `com.mana.genesis`. Its agent runtime, local model, avatar, voice, personality, memory, learning, micro-world, messaging workflows and game/Minecraft capabilities remain outside this firmware directory. The firmware may expose supported Android roles and a small authenticated platform bridge; it must not duplicate those app features.

## Current limits

All four Soong modules are currently declared as non-privileged product apps. Do not mark them privileged or add permission allowlists until the exact package, signing certificate, requested permissions, target partition and security policy are verified. The current Settings and Control Center modules open Android-owned settings pages; they do not yet replace platform Settings/SystemUI.

No AOSP product build or RMX5566RU device test is claimed by the presence of these source files.
