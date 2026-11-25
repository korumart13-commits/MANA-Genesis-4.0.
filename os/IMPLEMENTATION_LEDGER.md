# MANA Genesis 4.0 — implementation ledger

Last reviewed: 2026-11-25  
Target profile: realme RMX5566RU / RE645F, Android 16, arm64-v8a.  
This ledger is authoritative for distinguishing implemented code from plans and prototypes. A catalog entry or UI placeholder is never counted as a working feature.

## Status vocabulary

- **Implemented + tested**: executable implementation exists and an automated test passes.
- **Prototype**: executable partial behavior exists, but it is not integrated into the OS product or lacks required end-to-end behavior.
- **Designed only**: architecture/catalog/documentation exists; no working implementation.
- **Blocked**: requires an external artifact, device capability, API approval, license, or hardware source that is not in the repository.

## Current verified changes

| Area | Status | Evidence / limits |
|---|---|---|
| Core module catalog | Implemented + tested | 27 module descriptors; catalog tests verify owner-requested coverage |
| Local-first brain routing | Engine contract tested; Android adapter compile passed in run 45686047287 | Shared MANA Brain router calls LiteRT-LM local provider; launcher build has no cloud provider configured; GPU-first with CPU fallback compiles; device inference/model benchmark pending |
| Cloud gateway dependency | Implemented + tested | Optional; not a dependency of MANA Brain |
| Core boot lifecycle | Implemented + tested | JVM lifecycle and dependency ordering only; not Android init or kernel boot |
| Encrypted MANA Diary core | Implemented + tested | AES-GCM file store, atomic replacement, search/update/export/delete approval; Android KeyStore adapter and UI compile passed in run 45684830885 |
| Encrypted MANA Memory | Implemented + tested; Android UI/Brain retrieval source added, build pending | Separate AES-GCM file and key alias, categories, search, expiry candidates, pinned records and owner-approved forgetting; JVM tests pass; Android UI and local Brain prompt retrieval source compile passed in run 45686047287; retrieval uses simple text matching, not embeddings |
| Life Manager core | Implemented + tested | Tasks persist through a separate AES-GCM encrypted file and Android Keystore alias; owner confirmation is required for consequential tasks; launcher screen source added; Android reminders/calendar adapter remain |
| Smart Receptionist policy | Implemented + tested (policy only) | Playful profile, 10-second delay, unknown-number handling and summaries; every call decision requires owner consent; no actual call answering/screening |
| Game Intelligence core | Implemented + tested (adapter contract) | Active sessions and allowlisted actions require an installed adapter and owner approval; launcher lists games and detects foreground game after user grants Usage Access; compile passed in run 45685024506; no generic screen automation |
| Minecraft Bedrock bridge | Source starter + syntax CI passed | Official Script API 2.11.0 behavior-pack starter emits visible world telemetry and accepts owner-tagged ping/status only; no phone transport or in-game automation yet |
| Future Updates core | Implemented + tested (state machine) | Idea → plan → build → test → owner approval → install; no self-compiling or system updater yet |
| Android UI shell | Prototype, compiled in separate workflow | Java launcher/assistant shells; not a complete System UI |
| Legacy launcher | Prototype; combined Gradle build passed in run 45684830885; latest memory/Life Manager/Brain adapter changes pending | Gradle/Kotlin launcher references MANA core and Brain, local model import/chat, Russian TTS, 3D overlay, encrypted diary, Life Manager and memory UI, game discovery and consented usage monitor; not included in the Soong product |
| Floating avatar overlay | Prototype; renderer and voice compile verified in earlier runs, foreground/snap changes pending | User-enabled draggable overlay hosts a procedural OpenGL ES 2.0 full-body stylized 3D character with idle/blink/speech animation, three outfit palettes, accessory toggle and three expressions; draggable, snap-to-edge and visible stoppable foreground notification compile in run 45686560054; still a primitive-mesh character, not a production rigged/high-detail asset, and not device-tested |
| Local model import | Hardened prototype | Streaming import, 16 GiB cap, free-space reserve, SHA-256 sidecar, temporary file and atomic replacement; no trusted publisher signature/model-license verification |
| Local model runtime | Prototype; Gradle compile passed | LiteRT-LM Android 0.16.1 API is callable from the shared Brain adapter; GPU-first/CPU fallback compiles; no bundled weights, target-device benchmark, model compatibility matrix or full OS service integration |
| Backup exposure | Corrected in launcher manifest | Android app backup disabled for model/user files |
| JVM error handling | Corrected in LiteRT runtime | Ordinary exceptions handled; fatal JVM errors are not swallowed |
| Core security policy | Implemented + tested as JVM policy | Not Android permission enforcement, SELinux, Binder authorization or filesystem sandbox |
| Storage safety planner | Implemented + tested as JVM policy | Metadata model only; does not scan/move actual device files; permanent delete disabled |
| Device-specific ROM | Blocked | Exact OEM kernel source, matching DTS/DTBO, vendor modules/blobs, VINTF, product tree, AVB and recovery/rollback validation are absent |

## Owner requirements — completion gates

### OS foundation and device integration
- [ ] AOSP product tree and reproducible full product build for exact RMX5566RU/RE645F.
- [ ] Exact OEM 5.15.197 kernel source, matching defconfig/DTS/DTBO, toolchain and vendor modules.
- [ ] VINTF/HAL/vendor compatibility validation, partition map, A/B update and rollback tests.
- [ ] Verified Boot/AVB signing design, SELinux domains and recovery image tested without risking the owner's daily phone.
- [ ] MANA branding, boot animation, System UI, launcher, Settings and Control Center integrated into one product.

### MANA intelligence and memory
- [ ] Real LiteRT-LM Android runtime adapter wired to MANA Brain through a stable service/Binder contract.
- [ ] Model manager with provenance, license, size/free-space, hash/signature, compatibility, atomic install and rollback checks.
- [ ] Local model packaged or imported with measured RAM/storage/thermal performance on target hardware.
- [ ] Durable encrypted local memory, retention controls, user review/export/erase and offline tests.
- [ ] Cloud adapters for OpenAI, Anthropic, Google and xAI; opt-in per request, encrypted credential storage, no embedded API keys, sensitive-data guardrails and provider tests.
- [ ] Adaptive learning limited to user-approved sources, provenance tracking, retrieval evaluation and safe resource scheduling.

### Avatar, voice and interaction
- [ ] Production-quality original, fully rigged 3D character asset: current procedural primitive-mesh character is an initial 3D prototype, not a skinned mesh/skeleton/blendshape asset; outfit/accessory system and asset provenance remain.
- [ ] Real-time procedural OpenGL ES 2.0 3D renderer and user-granted Android overlay host have been added to the launcher prototype; verify CI and target-device rendering. Integrate the renderer into the Soong MANA system product.
- [ ] Drag, snap-to-edge, resize, hide/show, touch/gesture, lifecycle and accessibility tests for floating avatar.
- [ ] Stable character identity/voice; outfit/accessory customization without changing body/face identity.
- [ ] Speech-to-text is wired through Android recognizer intent in the launcher. Russian Android TextToSpeech output and avatar mouth animation have been added in source; verify build, installed Russian voice availability, true female voice selection, interruption and on-device behavior. Wake/gesture invocation remains incomplete.
- [ ] Power-button/gesture invocation only through APIs supported by the target OS; no privileged interception assumption.

### Personal modules
- [ ] Diary with local persistence, search, edit, export and user-controlled deletion.
- [ ] Life Manager with reminders/tasks/calendar adapters and explicit confirmation for consequential actions.
- [ ] Contact Intelligence with scoped Contacts permission, local processing and user-visible explanations.
- [ ] Smart Receptionist with playful character, 10-second delay, summaries, unknown-number flow, opt-in call screening and legal/telephony capability checks.
- [ ] Yandex and VK adapters using official APIs, consent and secure token storage.
- [ ] Future Updates pipeline: voice idea capture → review → plan → isolated build/test → signed update → owner approval → rollback.
- [ ] Watch, earbuds, Bluetooth/Wi-Fi car and local-node integrations with pairing, least privilege and disconnect handling.

### Game Intelligence
- [ ] Launch and detect game sessions without claiming arbitrary screen control.
- [ ] Supported Android game integrations and explicit owner-authorized actions.
- [ ] Minecraft Bedrock add-on/Script API or owner-controlled server bridge, with version compatibility and test world.
- [ ] Game memory/learning scoped to each game, user opt-in, no anti-cheat bypass or hidden input injection.

### Security and release
- [ ] Real Android permission model, Binder caller verification, SELinux policy and non-privileged default.
- [ ] Encrypted storage and keys held by Android Keystore; no plaintext secrets or API keys.
- [ ] Threat model, dependency/SBOM scan, static analysis, fuzzing, penetration test and privacy/data-safety documentation.
- [ ] Full AOSP build, automated device tests, battery/thermal/performance tests, OTA/rollback test and recovery drill.
- [ ] No firmware flash until every device/boot/recovery gate is independently verified.

## Non-negotiable completion rule

A requirement is marked complete only after its implementation is in the OS product, the relevant automated tests pass, and device-dependent behavior is tested on the target hardware where applicable. A source file, catalog entry, CI compile, screenshot, or mockup alone does not satisfy that rule.
