# MANA Genesis 4.0 — implementation ledger

Last reviewed: 2026-11-26  
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
| MANA boot engine | Prototype; JVM tests pass | `ManaBootEngine` validates live Android product/device/API/ABI/kernel/Verified Boot identity before starting MANA userspace modules. It explicitly reports EFI/firmware interface as unverified; it is not a bootloader, EFI implementation, init replacement or ROM. |
| MANA Brain routing | Implemented + tested; canonical module shared with Android companion | Local provider wins first; cloud requires explicit request permission; sensitive requests fail closed; provider selection is explicit. Android companion now depends directly on `engine/mana-brain`; duplicate routing source removed so JVM tests and Android runtime use the same module. |
| MANA Brain Binder boundary | Soong core + protected service + Launcher client + instrumentation sources implemented; platform build not verified | Existing Kotlin ManaBrain and ManaBrainFacade compile through the new mana-brain-core Soong module. Assistant Binder service validates requests, checks signature permission and caller package signer, and uses a bounded executor. Launcher binds and submits AIDL requests. When no Soong local provider is registered, it forwards the prompt to the existing com.mana.genesis companion app, which contains the actual LiteRT-LM runtime. Device instrumentation sources exist; no Soong build or device test has yet passed. |
| Android local inference | Prototype; Gradle Android source compile verified in earlier project CI | The separate com.mana.genesis companion APK includes LiteRT-LM Android Engine/Conversation, LocalModelRuntime and ManaBrainBridge. The Soong system Assistant currently reaches it by explicit Launcher handoff when its own provider registry is empty; LiteRT-LM is not yet packaged inside the Soong system image. Real inference, performance, thermal behavior and long sessions on RMX5566RU remain unverified. |
| Local model catalog | Prototype with integrity checks | Pinned official Gemma 4 E2B LiteRT-LM artifact (2,588,147,712 bytes), owner acceptance of Apache 2.0, Wi-Fi-only DownloadManager transfer, free-space checks, exact size and SHA-256 verification. Device compatibility and actual target-device inference still require validation. |
| Cloud AI | Prototype; provider tests pass | OpenAI Responses, Anthropic Messages, Gemini generateContent and xAI Responses adapters. Android gateway encrypts credentials with Android Keystore and requires explicit per-request consent; local inference remains preferred. Provider account credentials and live calls are not supplied by this repository. |
| Yandex AI Studio / VK | Prototype; adapter tests and Android settings UI source compile verified | Official Yandex AI Studio provider and read-only VK users/profile + wall connectors have owner-facing settings, encrypted credential storage and opt-in controls. Live owner-authorized account tests and complete workflows remain. |
| Encrypted MANA Memory | Implemented + tested (core), Android integration prototype | AES-GCM local persistence, categories, search, retention candidates and owner-controlled forgetting; Android local prompt retrieval uses text matching, not vector embeddings. |
| Autonomous research loop | Prototype; source and unit tests added, CI pending | `ManaAutonomyEngine` proposes experiments, executes registered research adapters, reflects and records outcomes. `JsonlResearchJournal` persists evidence; `ManaAutonomyScheduler` runs a single bounded background worker; owner rules can be added later; Android grants remain mandatory. Scheduler is not yet started by Android lifecycle. |\n| Adaptive Intelligence | Prototype; opt-in scheduler and encrypted knowledge store source implemented | Owner-entered topics, Yandex Search API v2, daily WorkManager scheduling on unmetered Wi-Fi/battery-not-low, source URL/snippet provenance, encrypted cache, review and owner-approved deletion. It does not crawl arbitrary websites or train model weights. |
| MANA Diary | Implemented + tested (core), Android UI prototype | AES-GCM storage, update/search/export and owner-controlled deletion. Device lifecycle and recovery tests pending. |
| MANA Life Manager | Implemented + tested (core), Android UI prototype | Encrypted task storage and confirmation rules. Full calendar/reminder integration is not complete. |
| Contact Intelligence | Prototype | Explicit Android Contacts permission and local phone-number lookup. No contact data is uploaded. |
| Smart Receptionist | Prototype / partial; optional default-dialer path added | Playful policy, 10-second preference, unknown-number handling, summary preference, immediate Android CallScreeningService, plus an optional ACTION_DIAL + InCallService path. If the owner explicitly grants ROLE_DIALER and enables auto-answer, InCallService schedules answer after 10 seconds. This is not enabled by default and is not yet device-tested; no two-way AI call conversation, audio recording or live-call transcription/summary is implemented. |
| Avatar and voice | Implemented 3D prototype; Android source compile and glTF structure validation pass | Original 49-node full-body glTF character, 12 meshes, 12 PBR materials, six outfit/accessory variants and Idle/Speaking/Happy/Playful clips. SceneView/Filament preview and owner-enabled transparent floating overlay are integrated; outfit/accessory controls and TTS/expression state drive animation selection. A production weighted skin/viseme rig, on-device GPU validation, battery/thermal profiling and real-device animation tests remain. See `launcher/AVATAR_MODEL.md`. |
| Floating overlay | Implemented prototype; source compile verified | Compose/SceneView Filament overlay requires the user's “display over other apps” permission; it runs with a foreground notification, stop action, drag/snap/tap, and reads the selected local glTF outfit. Exact behavior on realme firmware is not yet device-tested. |
| Voice input/output | Prototype; optional system assistant entry point added | Russian speech recognition, Android TTS, an optional VoiceInteractionService/SessionService and owner role-request path exist. Actual assistant role selection, target-device behavior, voice quality, offline availability and animation sync still require device testing. |
| Game Intelligence | Prototype / partial | Installed game discovery/launch and foreground game detection after Usage Access permission; action framework is allowlisted and owner-gated. No universal screen-reading or hidden input injection. |
| Realme Master Games integration | Prototype; source + package-visibility test added, Android CI pending | MANA Game Intelligence now discovers and opens the OEM game hub through its public launcher intent using observed package IDs `com.coloros.gamespaceui` and `com.oplus.games`. It does not invoke private Game Space controls or alter OEM performance policies. Presence and launch behavior still need validation on RMX5566RU. |
| Minecraft Bedrock | Working world-side prototype; Android transport partial | Installable Behavior Pack uses official Script API; companion spawn/follow/stay/explore/hide/status commands, bounded read-only exploration and owner-tagged Script Event command path exist. Pack CI validates/packages `.mcpack`. No authenticated Android-to-world transport, full navigation, block manipulation or complete autonomous bot yet. |
| Future Updates | Core state machine + encrypted Android inbox prototype | Voice/text idea capture and encrypted local idea inbox exist; stage progression is explicit and owner approval gates approval/install transitions. It does not self-compile the OS, sign a ROM, or install updates independently. |
| Android device action bridge | Prototype; Android launcher Gradle build and unit tests pass on commit `63b58b95`; RMX5566RU device validation pending | Owner-facing controls screen exposes allowlisted app launch, torch, brightness, audio volume and DND through public Android APIs and `ManaAutonomyController`. Each capability starts disabled; Android runtime/special grants are requested only after owner opt-in. Exact direct owner phrases and one strictly validated structured MANA Brain proposal can be routed to the confirmation screen. Proposals are never self-confirmed; the Android adapter rechecks the explicit capability and live platform grant. Firmware, shell, root, messaging and call actions are excluded. Not privileged system control. |
| Owner device command parser | Prototype; JVM tests pass | Recognizes a narrow fixed allowlist of direct Russian/English commands (torch, DND, bounded brightness, explicit package launch); unknown text and shell-like strings are rejected. It does not execute model-generated prose. |
| MANA Brain action proposals | Prototype; JVM tests pass | A single structured proposal is parsed against the fixed `ManaDefaultActions` catalogue, exact parameter sets and bounded values; it always returns `ownerConfirmed=false` and is sent to the non-exported owner confirmation screen. No firmware or shell actions. |
| Security policy | Implemented + tested as JVM model | Default-deny capabilities and audit events. Not Android permission enforcement, SELinux, Binder identity validation or filesystem sandboxing. |
| Storage safety | Prototype; Android SAF review, quarantine and owner-approved deletion implemented | Scans only an owner-selected folder, skips protected file types, copies candidates into MANA_Review, verifies SHA-256, supports restore, and can delete the original only after a second explicit confirmation plus matching SHA-256 checks of both original and quarantine copy. No silent deletion or broad filesystem access. Core planner remains a JVM policy model. |
| Settings / Control Center / System UI | Partial; UI source validation passes; platform integration unverified | `ManaSettingsActivity` now has a consistent MANA navy/cyan visual hierarchy, device identity card, grouped System/Brain/Personalization/About sections and safe links into Android-owned protected settings. `ManaControlCenter` remains a quick-entry surface and the user-addable MANA Quick Settings tile is present. The AOSP ManaAssistant entry point launches the separate `com.mana.genesis` companion and forwards an optional draft prompt. These are not a replacement for privileged AOSP SystemUI, lock screen or platform-signed controls. |
| Yandex / VK apps and services | Prototype | Yandex AI Studio Text Generation API and read-only VK users.get/wall.get adapters exist with Android Keystore-encrypted credentials, owner-facing setup, explicit test-request confirmation and provider selection. No VK write operations exist. Owner account credentials, live quota/API tests and broader OAuth workflows remain. |
| Device ecosystem | Prototype / partial | ManaDevicesActivity lists already-paired Bluetooth devices after explicit Nearby devices permission and links to Android Bluetooth/Wi-Fi settings. It does not yet read watch/earbud health or sensors, send car commands, manage Wi-Fi devices, or provide multi-device local-node control. |
| MANA Code Agent / Termux bridge | Prototype; corrected command paths and read-only device audit added; Android Gradle CI pending | Dedicated launcher entry hands off fixed allowlisted Git status, diff check, core/Brain Gradle tests, debug APK build and a read-only RMX5566RU identity collector to Termux. Every operation is previewed and requires separate owner confirmation. Requires Termux RUN_COMMAND permission plus Termux `allow-external-apps` opt-in; APK build additionally needs Java 17, Gradle and Android SDK configured in Termux. Device audit records project-ID candidates, build/kernel identity, Verified Boot, memory and storage without root or block writes. It does not apply AI-generated source patches. No root or system-partition access. See [Termux Code Agent](TERMUX_CODE_AGENT.md) and [device identity collector](../device/collect_rmx5566_identity.sh). |
| Recovery / updater | Designed only | No verified device recovery image, signed OTA, rollback test or stock restore package. |
| AOSP / kernel / ROM | Blocked | No complete AOSP product build for exact RMX5566RU/RE645F. An OEM C85/C85 Pro Android B kernel/vendor-source candidate is now pinned in [the RMX5566RU source audit](../device/RMX5566RU_OEM_SOURCE_AUDIT_2026-11-26.md): Linux 5.15.180, MSM_ARCH=bengal and Moscow Oplus overlays. The target reports Linux 5.15.197 and RMX5566RU; its Oplus project ID is not yet matched to an overlay, and the audited commit names RMX555525665. Exact target kernel mapping, full Android product/device tree, proprietary vendor blobs/HAL/VINTF, AVB signing and recovery path remain unverified. No bootable MANA OS image exists. See also [previous kernel source audit](../device/DEVICE_KERNEL_SOURCE_AUDIT_2026-11-25.md). |

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


| Android Brain Binder endpoint | Protected Assistant service registered; AIDL library linked; validates requests and fails closed with ENGINE_NOT_PACKAGED | Implemented transport boundary only; inference engine not packaged; Launcher client and instrumentation tests pending |

## MANA Brain unified action orchestration — 2026-11-26

| Component | Current implementation | Verified status |
|---|---|---|
| Capability registry | Typed capability descriptors, unique IDs, enabled/available checks and declared Android permission requirements in `engine/mana-brain/.../ManaActionOrchestrator.kt` | Source committed; CI/JVM tests pending |
| Action planner contract | Ordered action plans with unique step IDs and request correlation | Source committed; CI/JVM tests pending |
| Safe executor | Sequential execution; permission preflight; explicit owner approval for configured and HIGH/CRITICAL-risk actions; stop-on-first-failure | Source committed; CI/JVM tests pending |
| Result verification | A capability is marked successful only when it returns both success and verified=true | Source committed; CI/JVM tests pending |
| Audit trail | Per-step request/capability/status/message event interface | Source committed; durable Android audit-store integration pending |
| Existing inference routing | Local-first LiteRT-LM provider discovery and explicit cloud fallback remain separate from action execution | Existing routing retained; provider packaging/device inference remain pending |

This increment creates the shared safe action boundary for future integration of the existing phone-control, receptionist, contacts, avatar, Life Manager, Diary, adaptive learning and game/Minecraft modules. It does not yet wire every module to the registry, grant system privileges, or replace Android SystemUI. No arbitrary shell execution, permission bypass, silent cloud routing or self-granted privilege is introduced.


## Realme Master Games bridge — 2026-11-26

MANA now exposes an owner-invoked **Open Master Games** action in the Game Intelligence section. The bridge checks for the two observed Realme/ColorOS package IDs and launches the installed app through `PackageManager.getLaunchIntentForPackage`. Package visibility is declared for Android 11+. No hidden OEM APIs, root, input injection, or silent performance changes are used. This is an integration entry point, not yet a remote-control API for Game Space. A direct OEM control API has not been identified; performance controls remain owned by Realme Game Space and Android's supported game-mode mechanisms.


## 2026-11-26 incremental implementation note

- Added `ManaCapabilityRegistry` to the canonical `mana-brain-core` source set. It provides a declarative Brain-to-OS capability boundary and fail-closed decisions for unknown, disabled, unsupported, missing-permission, missing-special-access and unconfirmed-sensitive actions.
- Added initial capability descriptors for app launch, opening supported settings, notification listener access, contacts read, answering calls through the dialer role, and signed MANA update installation.
- Added JVM tests for unknown capabilities, Android runtime permissions, Android special access, owner confirmation, disabled/unsupported capabilities and duplicate IDs.
- Added `os/MANA_BRAIN_OS_CONTROL_CONTRACT.md` describing the Brain → OS → device execution model and the required Android platform adapter.
- These new sources are committed but their tests have **not yet been run in CI or Soong**. No device-side capability adapter, complete SystemUI replacement, AOSP image, or RMX5566RU ROM is claimed by this change.


## 2026-11-26 Binder capability adapter increment

- Extended the protected `IBrainService` AIDL contract with `executeCapability(ManaCapabilityRequest)` and a structured `ManaCapabilityResponse`.
- Added the Android platform adapter `ManaAndroidCapabilityExecutor`. It reads current package permission grants with `Context.checkSelfPermission`, checks notification-listener special access and dialer role state, then evaluates the shared `ManaCapabilityRegistry`.
- Implemented only two execution adapters: launching an installed package through `PackageManager.getLaunchIntentForPackage`, and opening a small allowlist of Android Settings screens through public Settings actions.
- Added package-visibility declarations for launcher activities.
- Sensitive capabilities remain disabled. The adapter deliberately ignores the caller-supplied `ownerConfirmed` boolean; a Binder field is not proof of owner consent. Contacts, call control, notification reading and OTA installation have no execution adapter in this increment.
- Added Android instrumentation test sources for unknown capabilities, fail-closed sensitive actions, missing launch targets and non-allowlisted settings pages.
- Updated the Android Binder module source filters and instrumentation-test dependencies so the adapter and tests are included in that module.

### Verification status for this increment

- **Passed locally:** the exact capability-registry Kotlin source compiles with the installed Kotlin compiler; six smoke assertions passed (unknown ID, disabled sensitive capability, missing notification special access, allowed low-risk capability, disabled capability, and owner-confirmation requirement).
- **Not run:** Android Binder/AIDL Gradle compilation, Android instrumentation tests, Soong build, and RMX5566 device tests. This environment did not provide an Android SDK/device test run. Do not mark the adapter as Android-build-verified until those checks execute.

- Registered both new capability parcelables in `system/mana/brain/aidl/Android.bp`, so the Soong AIDL interface source list includes the request and response types used by the updated Binder contract.


## 2026-11-26 architecture and build repair increment

- Added `os/MANA_EXPERIENCE_ARCHITECTURE.md` with the unified MANA visual system, first-run flow, launcher, Control Center, notifications, Settings, Brain interaction loop, module behavior contract and acceptance criteria.
- Added `os/BOOT_CHAIN_AND_EFI_REFERENCE_POLICY.md` with exact-device boot evidence requirements and an explicit prohibition on treating generic PC EFI binaries or unrelated phone images as RMX5566 boot inputs.
- Renamed the action-executor-only `CapabilityRisk` and `ManaCapability` symbols to `ActionCapabilityRisk` and `ExecutableManaCapability` to remove collisions with the separate declarative Android capability registry. The action executor's behavior and owner-confirmation gate are unchanged.
- Removed an SDK-inaccessible notification-listener query from the Android Binder adapter. Notification-listener capability remains unavailable/disabled until a supported, verified platform access check is implemented; it is not guessed or implicitly granted.
- Corrected the unified CI raw-`javac` source check to exclude the Binder adapter, which depends on generated AIDL and Kotlin classes and is intended for Soong integration rather than standalone compilation.
- The Settings hub was refreshed with MANA design tokens and explicit links to Android-owned settings pages. This remains a normal app activity, not privileged Settings replacement.
- Latest observed CI for commit `ac7cca0d`: the source-validation job passed; Brain, core and Android prototype jobs were still running when this ledger update was authored. Check the linked Actions run before treating them as passing.
- No full AOSP Soong product build, complete RMX5566RU device tree/vendor/kernel integration, AVB image, boot test, recovery test or flashable ROM was produced by this increment.

See also:
- `os/MANA_EXPERIENCE_ARCHITECTURE.md`
- `os/BOOT_CHAIN_AND_EFI_REFERENCE_POLICY.md`


## MANA OS firmware-only implementation increment — 2026-11-26

- Added `os/MANA_OS_FIRMWARE_ONLY_IMPLEMENTATION_RU.md` as the authoritative firmware-only scope for the RMX5566RU target. It explicitly separates the firmware from the later standalone MANA AI app.
- Added the MANA dark-navy/cyan color resources and app themes to the Soong `ManaLauncher` and `ManaSettings` source modules.
- Updated the launcher application label to **MANA OS** and settings label to **MANA OS Settings**.
- Explicitly wired each app's `res` directory in its Soong `Android.bp`.
- Added `.github/workflows/mana-os-branding-validation.yml` to parse the XML and verify the MANA OS labels/theme references.
- These are real source changes in the repository. They are **not yet AOSP/Soong-built** and do not yet replace Realme SystemUI, lock screen, framework settings, boot animation or product identity. The validation workflow file is committed; a run for this workflow has not yet been observed.
- No RMX5566RU image, OTA or flashable package has been generated. The exact OEM/device bring-up blockers listed above remain in force.


### Verification follow-up — MANA OS branding source checks

- GitHub Actions run [45870151076](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45870151076) completed successfully for commit `b60a634d9197fcce958b71d1e000d4637f2a50ca`. Its Android UI job compiled the MANA UI Java sources against the Android 16 API and passed package-integration and manifest/permission-surface checks.
- GitHub Actions run [45870172403](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45870172403) completed successfully for commit `d99d5c48034988149c7cc4519b2b2c67dca93cf6`. XML parsing and MANA OS branding/theme-reference checks passed.
- These checks validate source syntax and the existing Android UI Java compile workflow. They do not constitute a full Soong build, AOSP product build, SystemUI replacement, device boot test or flashable RMX5566RU firmware.
