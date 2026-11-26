# MANA OS — firmware application modules

This directory contains firmware-facing UI modules only. The autonomous MANA AI remains a separate application.

## Packages

- **ManaLauncher** (`com.mana.launcher`): MANA OS HOME, custom home surface, searchable app drawer and installed-app icons. Existing OEM/system/user apps are not removed. MANA opens the separate `com.mana.genesis` app.
- **ManaSettings** (`com.mana.settings`): MANA OS settings hub and navigation to Android-owned settings pages.
- **ManaControlCenter** (`com.mana.controlcenter`): MANA-styled quick-access hub and optional Quick Settings tile. It does not yet replace the platform notification shade.
- **ManaAssistant** (`com.mana.assistant`): source-only prototype, excluded from the firmware product package list until it is refactored into a thin system bridge with no AI runtime.

## Product integration

`system/mana/mana_product.mk` adds only Launcher, Settings and Control Center. The future verified Android 16 product makefile may include this fragment. It does not remove any Realme/OEM package or installed application.

## Separate MANA application

The standalone app package is `com.mana.genesis`. Its local model, avatar, voice, personality, memory, learning, micro-world, messaging workflows and game/Minecraft capabilities remain outside this firmware directory. The firmware prepares the shell and supported Android integration points without duplicating the agent.

## Current limits

The MANA Launcher/Settings/Control Center modules have source-level MANA themes. Android SystemUI, Keyguard, Recents, stock Settings internals and OEM File Manager still require exact RMX5566RU package/resource mapping before overlays can be written safely. No guessed overlays are added. No complete device build or flashable ROM is claimed.
