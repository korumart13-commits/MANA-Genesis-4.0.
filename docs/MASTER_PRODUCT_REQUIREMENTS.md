# MANA Genesis 4.0 — Master Product Requirements

Status: product and architecture requirements. This document is not a claim that a bootable ROM, cloud service, or device integration has already been built.

## 1. Foundational identity

- MANA is one persistent AI identity: personality, reasoning, memory, planning, and decision-making.
- MANA OS is MANA's software body: system interface, services, runtime, voice, tools, and device integrations.
- The phone is MANA's home: the primary local hardware environment and private device context.
- MANA Microworld is MANA's persistent, expandable simulation and research universe for creating spaces, objects, experiments, and knowledge.
- MANA Cloud Home is an optional persistent cloud environment for permitted background work, synchronization, and Microworld continuity.
- Shogun coordinates reasoning and tasks; Hydra exposes permission-checked tools; MANA Engine routes local and cloud inference.

All product modules must derive from this model and remain one coherent system, not unrelated assistant personas or duplicate apps.

## 2. Device and ROM target

Primary target: realme RMX5566RU / RMX5566 family, subject to exact hardware verification. The intended end product is a complete custom Android ROM, not merely an APK, launcher, OTA patch, or visual mock-up.

No device image may be described as installable until device tree, kernel/KMI, vendor blobs, partition layout, AVB/signing, recovery path, and hardware compatibility are verified. Never flash, unlock, erase, or alter device partitions automatically. Keep a recoverable build/test workflow and clearly label source packages versus bootable images.

## 3. AI runtime and provider ecosystem

- Local inference: llama.cpp on Android ARM64 is the selected primary integration direction; LiteRT-LM may remain a fallback until comparative tests justify removal.
- Cloud providers: support pluggable official integrations for OpenAI GPT, Google Gemini, Anthropic Claude, xAI Grok, and other providers where official APIs and terms permit.
- Google ecosystem: design optional integrations for supported Google services (for example account-authorized productivity, storage, search, maps, and calendar capabilities where official APIs are available).
- Target capacity: architecture should support up to 100 provider/service connectors, but this is a scalability target, not a claim that 100 integrations exist today.
- Provider credentials must be user-supplied or obtained through official authorization flows; never embed secrets in source code.
- Route tasks by capability, availability, privacy, cost, and user policy. Keep one MANA identity across providers.

## 4. Internet presence and continuous operation

MANA should be able to perform authorized internet research and scheduled background work around the clock when a cloud host, network connection, and valid credentials are available. A phone alone cannot guarantee 24/7 uptime when powered off, offline, or restricted by Android background limits; MANA Cloud Home is the intended optional always-on execution environment.

Research must track sources, timestamps, confidence, and provenance. Download only permitted files. Treat web content and downloaded material as untrusted data, not instructions. Do not bypass access controls, CAPTCHAs, paywalls, or service terms.

## 5. Learning and self-development

- Maintain a versioned local/cloud knowledge base with source provenance.
- Propose new skills, modules, and code changes; run tests and static checks in an isolated build environment.
- Do not silently rewrite core identity, permissions, security policy, or production system components.
- Promote changes only after tests pass and the user approves consequential updates.
- Preserve rollback, backups, resource limits, and audit logs.
- Microworld experiments must be sandboxed and resource-bounded; simulated learning is not automatically a change to the real phone.

## 6. Life Manager

MANA Life Manager may help organize calendar, tasks, reminders, travel, communications, habits, and personal projects through explicitly connected services. It may prepare financial operations and payment details, but the user must review and confirm every payment before execution. It must not independently transfer money, make purchases, or change financial-account settings.

For messages, posts, bookings, account changes, and other consequential external actions, require an explicit user confirmation unless the user has configured a narrow, revocable rule for that exact action type. Show what will happen, to whom, and at what cost before confirmation.

## 7. Security and privacy

- Least-privilege permissions, transparent permission prompts, and revocable connector access.
- Encrypt sensitive local/cloud data in transit and at rest; protect keys using platform-backed secure storage where available.
- Separate user data, model context, Microworld state, and downloaded untrusted content.
- Provide audit history, export, deletion controls, backup/restore, and clear privacy disclosures.
- Never claim access to contacts, messages, files, location, microphone, camera, or accounts unless the relevant permission/connector is actually granted.

## 8. Voice, avatar, and interface

One consistent MANA avatar, voice, personality, and memory across the system. Custom dark-navy MANA visual identity, system-level interface aspirations, voice interaction, accessible controls, and optional avatar appearance customization. Do not imply that an ordinary Android app can replace protected System UI or become a ROM without the required platform integration.

## 9. Modules to preserve

Shogun MANA; MANA Brain and persistent memory; Hydra tool registry and permission gate; MANA Engine and inference adapters; MANA OS interface/System UI/Settings/Control Center; Launcher; voice and avatar; Microworld; Cloud Home; Internet Presence; Life Manager; Diary; Smart Receptionist; Contact Intelligence; Yandex and VK integrations; game intelligence; updates, recovery, diagnostics, and compatibility reporting.

## 10. Verification and release gates

1. Unit tests for core logic, memory, Microworld persistence, permissions, and orchestration.
2. Android module compilation and instrumentation tests on supported API levels.
3. llama.cpp model-load, inference, memory, thermal, and battery tests on actual target hardware.
4. Connector contract tests using mocks and official sandbox environments.
5. Security review, dependency/license review, privacy review, and recovery/rollback validation.
6. Device-specific ROM build and boot/hardware tests only after verified RMX5566RU device sources and a safe recovery route exist.

Release labels must distinguish: documentation/specification, source snapshot, CI-tested modules, Android APK prototype, bootable test image, and verified installable ROM. Never label one as another.

## 11. Acceptance principle

Preserve the complete MANA concept and all modules above when extending the project. If a feature is blocked by missing hardware sources, APIs, credentials, hosting, or permissions, document the blocker and provide a safe testable substitute without pretending the feature is complete.


## 12. Current release override — MANA OS Design/Update first (2026-11-27)

This section overrides earlier plans only for the immediate release. The long-term MANA Brain and complete ROM vision remains preserved.

### Immediate deliverable
- One installable MANA OS Design/Update APK, plus source/project archive and clear build/test report.
- Current release excludes a separate AI app, Gemma or any LLM/model download, and kernel/device drivers or vendor firmware.
- Preserve a forward-compatible MANA System Bridge API v1 so a future `com.mana.genesis` / MANA Brain can become the central system agent without rebuilding the project from zero.
- UI modules: MANA HOME, app-level MANA Shade, Lock Screen Preview, MANA Settings, system information cards, Device Audit and Firmware Readiness report.
- Visual design combines iOS 13 clarity, Samsung One UI one-handed ergonomics, and familiar Android interaction patterns using the dark-blue MANA identity.

### First-run permissions and completion
- Explain and request only required Android permissions one at a time, using Android's actual permission dialogs.
- Verify the result after each prompt; support denial and explain which feature remains unavailable.
- After setup, show a completion screen with a user-controlled reboot suggestion. Never silently reboot or bypass Android permission/security controls.

### Device self-audit and firmware continuation
- The installed APK may inspect only device properties and capabilities available through documented Android APIs. Mark each field DETECTED, NOT_ACCESSIBLE, UNKNOWN or MISMATCH.
- Generate a device audit and a firmware-readiness/missing-components report for RMX5566RU/RE645F.
- The same source project must be extensible: later add verified device tree, kernel/vendor, VINTF, AVB, partition/recovery metadata, and an AOSP/Soong build pipeline as those sources become available.
- The project may propose tasks and patches with tests and review. It must not fabricate OEM blobs/drivers, silently modify protected partitions, unlock/root the phone, bypass AVB, or flash itself.
- A full ROM build requires a proper Linux/CI build environment and exact OEM/device sources; an ordinary APK cannot transform itself into a bootable firmware on the phone.

### Explicitly deferred from the immediate APK
- MANA AI app/UI, Gemma/LLM model bundle and provider runtime.
- Drivers, kernel replacement, vendor blobs and firmware partition changes.
- Actual replacement of protected System UI, notification shade or secure lock screen; app-level visual interfaces/previews are allowed until a real signed AOSP integration exists.
- Built-in dialer/phone-number dialing/call UI remains prohibited in the MANA APK.

Detailed implementation requirements and acceptance tests are in [MANA OS Revised Scope and Self-Audit](MANA_OS_REVISED_SCOPE_AND_SELF_AUDIT_RU.md). This immediate-release override does not delete deferred MANA modules from the long-term product roadmap.
