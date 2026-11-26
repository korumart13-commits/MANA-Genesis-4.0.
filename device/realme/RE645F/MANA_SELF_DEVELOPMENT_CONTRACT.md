# MANA Genesis 4.0 — self-development and module preservation contract

## Product invariants

The single MANA Brain remains the orchestrator; the MANA engine is the separately testable execution/runtime layer; MANA OS is the platform and user experience. Android 16 remains the compatibility base until a verified MANA-controlled ROM exists.

No existing feature family is removed as a shortcut. The release manifest must retain module IDs for:
- core brain, local inference/runtime, provider routing, encrypted memory and learning;
- launcher, settings, system UI/control-center integration and update center;
- avatar, voice, animation and personality presentation;
- diary, life manager, smart receptionist and contact intelligence;
- game intelligence, Minecraft bridge/world exploration and game tools;
- micro-universe/world simulation, creator tools and auxiliary services;
- device connectivity, watch/earbuds/car integrations where supported.

A module may be marked unavailable on a particular build if its dependencies or device capability are absent. It must not be silently deleted or represented as working when it is not packaged and tested.

## Autonomous code evolution

MANA's development agent may inspect its own repository, propose patches, edit MANA-owned source files in a disposable workspace, run tests, compare regressions and prepare signed candidate releases. Each change records:
- task and rationale;
- exact source revision and patch;
- dependencies and license review;
- tests executed and their results;
- artifact hashes and MANA release version.

The agent may not edit the active running binary in place. It must build a candidate, verify it, retain the previous known-good version and activate via a supported Android package/update mechanism. Failed health checks trigger rollback.

## Privilege boundary

The current companion/system source is not yet a privileged system service on the target phone. An ordinary APK cannot grant itself platform signature permissions or kernel access. A later MANA ROM may integrate a privileged, signature-protected system service with narrowly typed APIs, SELinux policy and explicit owner-granted capabilities. Kernel, boot-chain, modem, vendor HAL, AVB and recovery modifications remain separate high-risk release tracks.

## Release gate

A release may be called an installable MANA system update only after:
1. all packaged modules have a machine-readable inventory;
2. source and tests pass;
3. APK/module signatures and hashes verify;
4. target model and exact base fingerprint match;
5. installation path is supported by the currently running platform;
6. activation and rollback are tested on the exact device.

The MANA userspace update channel uses its own monotonically increasing version code (starting with `MANA +1`). It never overwrites the Realme/Android build fingerprint or pretends to be an OEM OTA.
