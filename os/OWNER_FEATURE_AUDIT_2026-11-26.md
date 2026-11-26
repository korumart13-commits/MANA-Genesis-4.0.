# MANA Genesis 4.0 — implementation audit
Audit date: 2026-11-26
Branch: `mana-genesis-4-rmx5566-rom-port`
Target: realme RMX5566RU / RE645F / Android 16 (API 36)

## Verdict
The repository is a mixed Android app prototype plus AOSP source foundation. It is **not a complete OS image and not ready for installation as firmware**. CI source checks have passed for the Brain/core, self-development policy, pinned AOSP manifest, device product validation and signed update-channel checks. At audit time, the Android/Kotlin launcher build and artifact publication were still running; no newly built APK had been verified.

## Implemented in source, but not yet proven on the phone
- MANA launcher shell and HOME role request.
- Local encrypted stores/services for memory, diary, life tasks, adaptive knowledge and future-update ideas.
- LiteRT-LM runtime integration prototype; model weights still need separate import and on-device verification.
- Avatar renderer/assets prototype.
- Android action broker for app launch, torch, brightness, volume and Do Not Disturb, subject to Android permissions and explicit owner approval.
- Game session detection using Usage Access and a bridge that opens the installed Realme Game Space app.
- Telecom service source: optional default-dialer integration, call screening and 10-second auto-answer when the owner has explicitly selected MANA as default dialer and enabled the setting.
- Contact lookup is local-only and requires Contacts permission.
- VK adapter is read-only (profile and own wall); Yandex provider/search source exists.
- Self-development policy and isolated-candidate workflow tests exist, but this is not autonomous modification of the installed APK or firmware.
- Device adaptation screen reads build identity and opens Realme's official updater; it does not download or install OEM firmware.

## Missing or incomplete modules
| Capability | Audit result |
|---|---|
| Owner identity / continuous identity verification | Not implemented as a reliable owner-authentication system. Add explicit enrollment and Android BiometricPrompt/device-credential re-authentication; no covert always-on face/voice identification. |
| Sleep / bedtime mode | No dedicated module found. |
| Camera control | No MANA camera module found. Torch control is not camera control. Camera can be opened through Android's camera intent; advanced controls require Camera2 APIs, runtime permission and device testing. |
| Messenger control | No general messenger-control module found. Safe baseline is open app / compose or share via intents; reading or sending inside WhatsApp/Telegram/VK requires official APIs and user consent. |
| Full phone / dialer | Prototype only; role selection and device/carrier behavior need testing. |
| 10-second auto-answer | Source implements a 10-second delay only through optional ROLE_DIALER InCallService; not available while another dialer owns calls. |
| Call summaries / recording | Not implemented as a complete compliant pipeline. Recording is not silently enabled. |
| Game AI / autonomous gameplay | Partial session awareness and launch only. No universal screen control or anti-cheat bypass. Minecraft bridge is a prototype and needs a supported world/server and device tests. |
| Developer Center / AutoDev | Partial/prototype. It can prepare and validate source candidates in an isolated workspace; it cannot change the running app binary or flash the system. |
| Persistent memory | Encrypted local store source exists; end-to-end recall quality, migrations, retention and restore still require integration/device tests. |
| Voice | Recognition/voice-interaction scaffolding exists; a stable MANA female TTS voice and complete always-available assistant experience are not verified. |
| Cloud AI providers | Not all requested providers are wired and tested. No API keys are embedded; provider setup and consent remain required. |
| Full System UI, settings and Quick Settings replacement | Not implemented as a privileged integrated SystemUI product. |
| Realme camera, messaging, OEM service integration | OEM app control is limited to public Android entry points and documented APIs. |
| Full AOSP ROM, kernel, vendor/HAL, recovery and OTA | Blocked: exact RMX5566RU source/device tree/vendor/AVB/signing chain is not verified. |

## Android security and hardware boundaries
A normal third-party APK cannot silently become the default dialer, read protected messenger databases, capture camera output in the background, grant itself permissions, replace SystemUI, write protected partitions or install a Realme-signed OTA. Android roles, runtime permissions and special access require user action. MANA must use official app APIs, Android intents, Telecom/Camera2 APIs and explicit permissions.

## Required implementation order
1. Finish and verify the current Android/Kotlin CI build; inspect APK manifest, package metadata and artifact hash.
2. Add a dedicated sleep/bedtime module with schedules, quiet hours and explicit notification policy controls.
3. Add owner identity enrollment and secure re-authentication using Android BiometricPrompt/device credentials.
4. Add camera launch and Camera2 capture flows with clear foreground UI and runtime permission.
5. Add a messenger hub using launch/compose/share intents first; integrate each messenger only through its official API.
6. Connect the call, contacts, memory, diary, life manager, adaptive learning and Brain services through one tested app runtime.
7. Add integration tests and real-device tests on the exact RMX5566RU build before marking any capability as working.
8. Keep ROM/firmware work blocked until exact OEM sources, signing and recovery/rollback are verified.

## Battery / charging
Battery endurance cannot be measured from source code or CI. It requires installing the verified APK on the target phone and measuring idle drain, foreground AI use, background work and model inference with the model actually imported. A multi-gigabyte local model can materially increase memory, thermal and battery load; boot-time model loading remains disabled by design.

## Release rule
Do not label any module “working” solely because it appears in the menu, manifest, source tree or architecture. Mark it working only after build, automated tests and target-device smoke tests pass.
