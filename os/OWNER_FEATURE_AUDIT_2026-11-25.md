# MANA Genesis 4.0 — full owner-requirements audit

Audit date: 2026-11-25  
Repository: `korumart13-commits/MANA-Genesis-4.0.`  
Target: realme RMX5566RU / RE645F / Qualcomm SM6225 / Android 16

## Executive verdict

**Not complete. Do not install or flash.** The repository contains a useful source foundation, two different generations of launcher/assistant source, a host-JVM service core, security/storage policy models, and a LiteRT-LM integration prototype in the older Gradle launcher. It does not yet contain a complete AOSP operating system, device-matched kernel build, integrated system services, or a tested device image.

The module catalog has been expanded to represent all owner-requested capabilities, including an explicit multi-provider cloud gateway. Catalog entries and documentation are architecture coverage only; they are not proof of implementation.

## Important repository structure finding

There are currently two different UI implementations:

1. `launcher/` — older Gradle Android application prototype. It contains a floating avatar placeholder, app drawer, voice input through Android's speech-recognition intent, local model import, a LiteRT-LM runtime class and a local chat bridge.
2. `system/mana/apps/` — newer Soong source modules intended for the integrated OS. These contain a simple HOME launcher and assistant shell. The assistant shell does not call LiteRT-LM, does not call the JVM MANA Brain, and currently reports that inference is not connected.

These are not yet unified. The old Gradle prototype is not included by `system/mana/mana_product.mk`; the new Soong apps do not contain the old prototype's local inference/voice/avatar behavior. The old prototype has no current CI build job in this repository. This split is a release-blocking integration gap.

## Owner-requested feature matrix

Status meanings:
- **Source exists (prototype):** code exists, but is not integrated into the OS product or verified on the target.
- **Architecture only:** named in docs/catalog, without a working feature implementation.
- **Partial:** only a limited portion is implemented.
- **Missing:** no implementation was found in the audited repository.

| Requested capability | Repository evidence | Status |
|---|---|---|
| MANA identity, dark navy/blue theme | Both UI generations contain MANA branding and dark blue colors | Partial |
| Integrated OS launcher and app drawer | Soong HOME app plus older Gradle launcher prototype | Partial; two implementations are not unified |
| Open installed apps and games | Launcher queries and starts launchable activities | Partial; app launch only |
| Floating MANA avatar | Older launcher draws a movable circular placeholder | Source exists (prototype); not a 3D character |
| Fixed 3D character, hair/body/voice identity, outfit changes and expressions | No rigged GLB/GLTF, renderer, animation rig or outfit system found | Missing |
| Local-first AI | JVM Brain contract prefers a local provider; old launcher prototype has LiteRT-LM runtime code | Partial; prototype is not OS-integrated |
| LiteRT-LM Android runtime | Dependency and Engine/Conversation calls exist in old Gradle launcher | Source exists (prototype); no Gradle CI/device test and not wired into new Soong assistant |
| Actual local model weights installed and verified | Model import is a user-selected file; no model is included or run-tested on RMX5566RU | Missing on target |
| Model manager: model catalog, checksums, license, storage/RAM/thermal checks, rollback | Only setup notes and file import exist | Partial / mostly missing |
| Multi-provider cloud gateway: OpenAI GPT, Claude, Gemini, Grok | Brain accepts one generic cloud provider; no provider adapters, key vault or selection UI found | Missing |
| Offline operation | Local inference is designed, but model is not installed/tested on device | Partial |
| Persistent local memory and conversation history | Catalog/docs mention it; no encrypted memory/index implementation found | Missing |
| Adaptive learning and source validation | Architecture description only | Missing |
| Future Updates: voice ideas → plan → development → tests → release with owner approval | Architecture only; no idea queue or controlled build pipeline | Missing |
| Voice input | Older launcher uses Android speech recognition intent | Source exists (prototype); depends on available recognition service |
| MANA voice output and stable female voice | No TTS voice/personality implementation found | Missing |
| MANA Diary | Catalog only; no diary UI/storage implementation | Architecture only |
| Life Manager | Catalog only; no task/calendar/reminder implementation | Architecture only |
| Contact Intelligence | Catalog only; no contacts service or permission flow | Architecture only |
| Smart Receptionist: playful, 10-second delay, summaries, unknown callers | Module is catalogued; requested settings and call-handling implementation not found | Architecture only |
| Yandex integration | Catalog only; no official API adapter | Architecture only |
| VK integration | Catalog only; no official API adapter | Architecture only |
| Game Intelligence and entering games | Launcher can start games listed as launchable apps | Partial: game launch only |
| In-game awareness, planning, world interaction and Minecraft bot | No game connector, Minecraft add-on/script, server bridge or gameplay agent found | Missing |
| MANA Settings and Control Center | Catalog only; no integrated settings/control-center modules | Architecture only |
| System UI / boot branding / power-button summon | Catalog only; no Android SystemUI or boot animation integration | Missing |
| Safe storage review/archive/restore | Host-JVM metadata policy supports approved reversible state changes; it never touches actual files | Partial policy prototype |
| Security policy | Host-JVM default-deny capability policy with audit events | Partial policy prototype; not Android enforcement |
| Recovery and signed updater | Catalog/docs only; no device-specific recovery or OTA implementation | Architecture only |
| Local server / multi-device pairing | Architecture document only; no service or pairing code | Architecture only |
| Watch, earbuds and car integrations | Architecture only; no pairing/integration implementation | Architecture only |
| Self-development | Future-updates design only; no safe proposal/build/test pipeline | Missing |
| Exact RMX5566RU kernel/device integration | Runtime facts and source audit only; matching OEM kernel/device tree/vendor blobs not verified | Blocked |
| Full AOSP Android 16 product | AOSP manifest instructions only; no AOSP checkout/product build | Missing |

## Game capability — exact boundary

The current launcher can open installed games using Android launch intents. That does **not** mean MANA can see the game screen, understand the world, control the player or operate as a bot.

Game Intelligence must be built as a permissioned adapter framework:
- Generic Android game launcher and game-session detection using supported APIs.
- Per-game integrations only through official SDKs, documented plugin/mod interfaces, user-approved local files or a server the owner controls.
- Minecraft Bedrock integration should use supported Creator/Script APIs and an explicitly installed world/add-on or owner-controlled server bridge. The official Microsoft Creator documentation describes these supported scripting surfaces.
- Do not use Accessibility APIs as a universal hidden gameplay-control mechanism. Google Play policy prohibits using Accessibility API to bypass Android privacy/security controls or power autonomous decision-making agents.
- No anti-cheat bypass, credential theft, hidden screen capture, or unauthorized game automation.

## Security findings requiring correction before release

1. The old Gradle prototype manifest currently has `android:allowBackup="true"`. For a local-memory/model app, backup behavior must be explicitly designed; default should be disabled until encrypted export/restore is implemented and tested.
2. The old `LocalModelRuntime` catches `Throwable`, including fatal VM errors. Change normal error handling to catch `Exception`; fatal VM errors must not be disguised as ordinary model failures.
3. Model import copies a selected file without an explicit size/free-space/hash/license validation pipeline. Add streaming size limits based on verified available storage, SHA-256 verification where a trusted digest exists, atomic temp-file promotion, and cleanup on failure.
4. The current app's local model path and runtime are prototype code, not a system-level isolated service. It must not be granted privileged/system identity just to gain access.
5. The newer Soong Assistant is intentionally disconnected from inference. Do not claim the assistant is AI-operational until the Binder/service bridge and LiteRT-LM provider pass tests.
6. JVM security decisions do not enforce Android permissions, Binder identity, SELinux, encrypted storage or kernel isolation.
7. CI currently compiles the new Java Soong UI source against Android 16 API and tests JVM modules. It does not build the old Gradle/Kotlin LiteRT-LM prototype or a full AOSP image.
8. The module catalog is metadata; module descriptors do not start services by themselves.

## Required integration order

1. Unify the old Gradle prototype's launcher/avatar/local inference features with the new Soong OS product modules. Choose one source of truth; remove duplicate competing UI implementations only after feature parity is verified.
2. Add CI that compiles/tests the Android/Kotlin LiteRT-LM adapter against a pinned supported artifact and verifies the model import path. Add a fake-provider test suite for MANA Brain and a real-device smoke test when hardware is available.
3. Implement the local model manager, persistent encrypted memory, multi-provider cloud gateway (OpenAI, Anthropic, Google, xAI) with opt-in per provider and no embedded keys.
4. Implement the remaining owner modules: voice output, avatar renderer, Diary, Life Manager, Contact Intelligence, Smart Receptionist with its configured behavior, Yandex, VK, Adaptive Intelligence, Future Updates, Developer Center, Local Node, Device Ecosystem, Settings and Control Center.
5. Implement Game Intelligence in two layers: general launch/session integration and explicit per-game adapters, starting with Minecraft Bedrock Creator/Script API or an owner-controlled server bridge.
6. Integrate services through Android Binder with caller identity checks, SELinux enforcing domains, scoped permissions, encrypted storage and audit.
7. Obtain exact RMX5566RU AOSP/device/kernel/vendor inputs; build and test images in CI without flashing.
8. Validate hardware, AVB, partition layout, signing, recovery and rollback before any installation plan.

## External technical references checked

- LiteRT-LM Android integration and Kotlin Engine/Conversation APIs: https://developers.google.com/edge/litert-lm/android
- LiteRT-LM overview and model/tool-use support: https://developers.google.cn/edge/litert-lm/overview
- Android Games API reference: https://developer.android.com/games/reference
- Google Play Accessibility API restrictions: https://support.google.com/googleplay/android-developer/answer/16558241
- Minecraft Bedrock Creator documentation: https://learn.microsoft.com/en-us/minecraft/creator
- Minecraft Bedrock Script API reference: https://learn.microsoft.com/en-us/minecraft/creator/scriptapi/?view=minecraft-bedrock-stable

## Final audit decision

The repository is **not sufficient for the full MANA Genesis 4.0 vision yet**. The architecture has been expanded to cover the complete requested module set, but the implementation gaps above remain. The only verified recent Android compile is the small Java UI shell in `system/mana/apps`; it is not the full launcher, avatar or local AI runtime. No full OS or installable firmware exists yet.
