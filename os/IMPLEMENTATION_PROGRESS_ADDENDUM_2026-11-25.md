# MANA Genesis 4.0 — implementation addendum (2026-11-25)

This addendum records work completed after the original progress snapshot. All Android source remains a prototype until verified on RMX5566RU and integrated into a bootable AOSP product.

## Additional implementation status

| Area | Current implementation | Remaining validation |
|---|---|---|
| MANA file manager | SAF-only folder selection; bounded recursive scan; conservative temporary/log candidate rules; encrypted quarantine journal; SHA-256-verified copy; restore; optional original deletion only after separate owner confirmation and matching source/backup hashes. The original is never silently deleted. | Provider-specific behavior, large-tree performance, permission revocation, recovery and restore tests on the phone |
| Default phone / Smart Receptionist | Optional CallScreeningService and InCallService; dial pad, incoming/ongoing call UI, call controls, call history with permission, multi-call selection and 10-second auto-answer after the owner explicitly grants ROLE_DIALER and enables the setting | The normal Android app cannot read the PSTN call audio stream; no two-way AI receptionist conversation or audio transcript. Verify realme Telecom behavior, emergency fallback and local law. Current summaries are call metadata only |
| Assistant invocation | VoiceInteractionService and VoiceInteractionSessionService entry point; owner can request assistant role; session opens MANA assistant/avatar. MANA Control Center has a user-addable quick-settings entry point for the floating avatar. | Confirm assistant role availability and power-button/gesture invocation on the realme firmware |
| Avatar | Bundled original glTF full-body model, multiple wardrobe/accessory variants, Idle/Speaking/Happy/Playful animation clips, animated mouth/eyes/brows/head/hair nodes, SceneView preview and foreground floating overlay. Appearance state is shared between launcher and overlay. | No complete glTF skin/skeleton or blendshape rig yet; verify outfit/state/TTS synchronization and rendering on the target GPU |
| Adaptive learning | Scheduled Yandex Search workflow, explicit owner-approved topics and sources, encrypted local knowledge cache, source-review screen and persisted last-run status | Needs live owner API credentials, relevance/freshness checks, prompt-injection tests and on-device scheduling tests. This is retrieval/knowledge-base growth, not model-weight training |
| Device ecosystem | Bluetooth paired-device dashboard and owner-controlled permission flow; local device discovery and car profile UI | Real watch/earbud pairing and vehicle-specific protocols must be tested; no generic safe control API exists for every car |
| MANA local model | Gemma 4 E2B LiteRT-LM download prototype (Wi-Fi-only, upstream SHA-256 verification), local-first Brain routing and CPU fallback path | Measure actual memory use, thermal throttling, token speed, GPU/CPU compatibility and long-session stability on RMX5566RU |

## Verified automated runs

- Android Gradle source build with SAF quarantine, hash verification, owner-confirmed original deletion, Brain mirror check and glTF validation: run 45690856895 passed.
- Android Gradle source build with avatar fallback and existing integrations: run 45691218064 passed.
- MANA Model Manager staged-install JVM tests: run 45687689117 passed.
- MANA Brain multi-provider routing tests: run 45687583299 passed.
- Adaptive knowledge and module catalog JVM tests: runs 45689544497, 45689557101 and 45689623830 passed.
- Minecraft Bedrock companion pack syntax/package workflows: runs 45687934475, 45687940684 and 45688019233 passed.
- Source-only repository bundle workflows are generating downloadable ZIP snapshots; these are not APKs and not bootable firmware.

## Device identity and ROM gate

The phone's runtime identifiers are RMX5566RU / RE645F / Qualcomm SM6225 / bengal / qcom / kernel 5.15.197. These identify realme C85 (4G), not C85 5G. The C85 5G / RMX5253 is a different MediaTek device and its kernel source is not compatible.

The exact OEM Qualcomm kernel source, matching device tree and DTBO, vendor blobs/modules, VINTF manifest, partition metadata, AVB signing chain and tested recovery/rollback package are still missing. There is no bootable MANA OS image yet. Do not flash any experimental image to the owner's daily phone.

## Completion rule

A feature is complete only after it is part of the final MANA OS product, relevant CI passes, and hardware-dependent behavior is verified on RMX5566RU. A source compile, unit test, UI mockup or generated source ZIP alone is not a finished OS.
