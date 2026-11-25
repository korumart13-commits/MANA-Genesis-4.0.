# MANA Genesis 4.0 — implementation ledger

Last reviewed: 2026-11-25  
Target profile: realme C85 (4G) RMX5566RU / RE645F, Qualcomm SM6225/bengal, Android 16, arm64-v8a.

This is the authoritative completion ledger. A module catalog entry, UI placeholder, documentation page, or successful compile is not proof of a complete feature. Status is based on the source and CI evidence available in this repository.

## Status vocabulary

- **Implemented + tested** — executable source exists and automated tests pass.
- **Prototype / partial** — executable source exists but is not fully integrated, lacks device validation, or implements only part of the requirement.
- **Designed only** — architecture or catalog entry exists without executable feature behavior.
- **Blocked** — missing OEM/device artifacts, user credentials, platform role, API approval, or hardware test.

## Current source and CI snapshot

| Area | Current status | What is actually present / what remains |
|---|---|---|
| OS module catalog | Implemented + tested | 31 MANA/platform module descriptors and dependency graph. Catalog coverage does not implement the modules by itself. |
| Core boot lifecycle | Implemented + tested (JVM) | Dependency ordering, lifecycle, rollback and health snapshot in host JVM. Not Android init, kernel boot or a ROM. |
| MANA Brain routing | Implemented + tested | Local provider wins first; cloud requires explicit request permission; sensitive requests fail closed; provider selection is explicit. |
| Android local inference | Prototype; Android source compile verified | LiteRT-LM Android Engine/Conversation adapter is connected to the launcher Brain bridge. Real inference, performance, thermal behavior and long sessions on RMX5566RU remain unverified. |
| Local model catalog | Prototype with integrity checks | Pinned official Gemma 4 E2B LiteRT-LM artifact (2,588,147,712 bytes), owner acceptance of Apache 2.0, Wi-Fi-only DownloadManager transfer, free-space checks, exact size and SHA-256 verification. Device compatibility and actual target-device inference still require validation. |
| Cloud AI | Prototype; provider tests pass | OpenAI Responses, Anthropic Messages, Gemini generateContent and xAI Responses adapters. Android gateway encrypts credentials with Android Keystore and requires explicit per-request consent; local inference remains preferred. Provider account credentials and live calls are not supplied by this repository. |
| Yandex AI Studio / VK | Prototype; adapter tests and Android settings UI source compile verified | Official Yandex AI Studio provider and read-only VK users/profile + wall connectors have owner-facing settings, encrypted credential storage and opt-in controls. Live owner-authorized account tests and complete workflows remain. |
| Encrypted MANA Memory | Implemented + tested (core), Android integration prototype | AES-GCM local persistence, categories, search, retention candidates and owner-controlled forgetting; Android local prompt retrieval uses text matching, not vector embeddings. |
| Adaptive Intelligence | Prototype; opt-in scheduler and encrypted knowledge store source implemented | Owner-entered topics, Yandex Search API v2, daily WorkManager scheduling on unmetered Wi-Fi/battery-not-low, source URL/snippet provenance, encrypted cache, review and owner-approved deletion. It does not crawl arbitrary websites or train model weights. |
| MANA Diary | Implemented + tested (core), Android UI prototype | AES-GCM storage, update/search/export and owner-controlled deletion. Device lifecycle and recovery tests pending. |
| MANA Life Manager | Implemented + tested (core), Android UI prototype | Encrypted task storage and confirmation rules. Full calendar/reminder integration is not complete. |
| Contact Intelligence | Prototype | Explicit Android Contacts permission and local phone-number lookup. No contact data is uploaded. |
| Smart Receptionist | Prototype / partial; optional default-dialer path added | Playful policy, 10-second preference, unknown-number handling, summary preference, immediate Android CallScreeningService, plus an optional ACTION_DIAL + InCallService path. If the owner explicitly grants ROLE_DIALER and enables auto-answer, InCallService schedules answer after 10 seconds. This is not enabled by default and is not yet device-tested; no two-way AI call conversation, audio recording or live-call transcription/summary is implemented. |
| Avatar and voice | Implemented 3D prototype; Android source compile and glTF structure validation pass | Original 49-node full-body glTF character, 12 meshes, 12 PBR materials, six wardrobe/accessory base variants plus twelve facial geometry variants (18 model assets total), all preserving Idle/Speaking/Happy/Playful clips. Shared Compose-observable state drives wardrobe, accessory, neutral/playful/happy expression and TTS speaking selection in both SceneView/Filament preview and the owner-enabled floating overlay. JUnit tests cover state and asset selection; glTF CI validates every embedded model. A production weighted skin/viseme rig, on-device GPU validation, battery/thermal profiling and real-device animation tests remain. See `launcher/AVATAR_MODEL.md`. |
| Floating overlay | Implemented prototype; source compile verified | Compose/SceneView Filament overlay requires the user's “display over other apps” permission; it runs with a foreground notification, stop action, drag/snap/tap, and reads the selected local glTF outfit. Exact behavior on realme firmware is not yet device-tested. |
| Voice input/output | Prototype; optional system assistant entry point added | Russian speech recognition, Android TTS, an optional VoiceInteractionService/SessionService and owner role-request path exist. Actual assistant role selection, target-device behavior, voice quality, offline availability and animation sync still require device testing. |
| Game Intelligence | Prototype / partial | Installed game discovery/launch and foreground game detection after Usage Access permission; action framework is allowlisted and owner-gated. No universal screen-reading or hidden input injection. |
| Minecraft Bedrock | Working world-side prototype; Android transport partial | Installable Behavior Pack uses official Script API; companion spawn/follow/stay/explore/hide/status commands, bounded read-only exploration and owner-tagged Script Event command path exist. Pack CI validates/packages `.mcpack`. No authenticated Android-to-world transport, full navigation, block manipulation or complete autonomous bot yet. |
| Future Updates | Core state machine + encrypted Android inbox prototype | Voice/text idea capture and encrypted local idea inbox exist; stage progression is explicit and owner approval gates approval/install transitions. It does not self-compile the OS, sign a ROM, or install updates independently. |
| Security policy | Implemented + tested as JVM model | Default-deny capabilities and audit events. Not Android permission enforcement, SELinux, Binder identity validation or filesystem sandboxing. |
| Storage safety | Prototype; Android SAF review, quarantine and owner-approved deletion implemented | Scans only an owner-selected folder, skips protected file types, copies candidates into MANA_Review, verifies SHA-256, supports restore, and can delete the original only after a second explicit confirmation plus matching SHA-256 checks of both original and quarantine copy. No silent deletion or broad filesystem access. Core planner remains a JVM policy model. |
| Settings / Control Center / System UI | Partial; Soong app CI passes | Product source now includes non-privileged ManaSettings and ManaControlCenter apps with shortcuts into Android protected settings panels; both are included in mana_product.mk and the four-app Android UI CI validation passed. A user-addable MANA Quick Settings tile is also present. These are not a replacement for privileged AOSP SystemUI, lock screen or platform-signed controls. |
| Yandex / VK apps and services | Prototype | Yandex AI Studio Text Generation API and read-only VK users.get/wall.get adapters exist with Android Keystore-encrypted credentials, owner-facing setup, explicit test-request confirmation and provider selection. No VK write operations exist. Owner account credentials, live quota/API tests and broader OAuth workflows remain. |
| Device ecosystem | Prototype / partial | ManaDevicesActivity lists already-paired Bluetooth devices after explicit Nearby devices permission and links to Android Bluetooth/Wi-Fi settings. It does not yet read watch/earbud health or sensors, send car commands, manage Wi-Fi devices, or provide multi-device local-node control. |
| Recovery / updater | Designed only | No verified device recovery image, signed OTA, rollback test or stock restore package. |
| AOSP / kernel / ROM | Blocked | No complete AOSP product build for exact RMX5566RU/RE645F. An official C85 5G (different MediaTek variant) source candidate was audited and rejected: it is MediaTek 5.15.180, while RMX5566RU diagnostics identify the standard C85 4G with Qualcomm SM6225/bengal/qcom and kernel 5.15.197. Exact matching Qualcomm source, DTS/DTBO, vendor modules, VINTF, AVB signing and recovery path remain unverified. No bootable MANA OS image exists. See [device kernel source audit](DEVICE_KERNEL_SOURCE_AUDIT_2026-11-25.md). |

## Required completion gates

### 1. Exact device and operating system
- [ ] Obtain and verify exact OEM kernel source, defconfig, DTS/DTBO, vendor modules and toolchain for RMX5566RU/RE645F.
- [ ] Complete matching VINTF/HAL/vendor compatibility and partition metadata.
- [ ] Build the full Android 16 AOSP product reproducibly.
- [ ] Integrate MANA Launcher, Brain service, SystemUI, Settings, Control Center, permissions and boot branding into one product.
- [ ] Implement SELinux domains, Binder caller checks, AVB signing, A/B OTA and tested rollback/recovery.
- [ ] Test install/restore on a spare or safely recoverable device before touching the owner's daily phone.

### 2. Intelligence and model lifecycle
- [ ] Verify LiteRT-LM model compatibility, actual inference, memory use, latency, thermal load and battery on the target phone.
- [ ] Add license acceptance, model provenance/signature verification, storage/RAM compatibility checks and safe model rollback.
- [ ] Move Brain from launcher-only adapter to a stable OS service/Binder contract.
- [ ] Complete provider selection/settings UI for all cloud adapters; use Android Keystore, no embedded keys, no cloud without per-request permission.
- [ ] Add encrypted vector retrieval, evaluation and user-approved learning sources.

### 3. Avatar and voice
- [ ] Finish and validate a proper skinned character rig with facial blendshapes/visemes, clothing variants and stable identity.
- [ ] Test idle, speaking, happy and playful animations in the floating overlay and full-screen preview.
- [ ] Verify Russian TTS voice, offline speech recognition, interruption and animation synchronization on-device.
- [ ] Profile GPU frame time, memory, battery and thermal behavior on RMX5566RU.

### 4. Personal and game modules
- [ ] Device-test the optional ROLE_DIALER/InCallService path and 10-second auto-answer on RMX5566RU; it must remain disabled unless the owner explicitly changes the default phone app and enables the setting.
- [ ] Complete calendar/reminder adapters, contact workflows and owner-visible data controls.
- [ ] Complete Yandex and VK OAuth/settings flows and official API integration.
- [ ] Pair Android Game Intelligence to Minecraft through an authenticated owner-controlled local bridge and test world.
- [ ] Complete watch, earbuds, car and multi-device pairing with least privilege and disconnect handling.

### 5. Security and release
- [ ] Run dependency/SBOM scanning, static analysis, fuzzing and penetration testing.
- [ ] Publish privacy policy, data-safety declarations, permission explanations and security documentation.
- [ ] Run full AOSP build, device instrumentation, OTA/rollback, recovery and performance tests.
- [ ] Never flash firmware until all device and recovery gates are verified.

## Completion rule

A requirement is marked complete only after its implementation is integrated into the MANA OS product, automated tests pass, and device-dependent behavior is verified on the target hardware. A source file, model preview, adapter test, Gradle compile, Minecraft pack artifact or module descriptor alone does not satisfy that rule.
