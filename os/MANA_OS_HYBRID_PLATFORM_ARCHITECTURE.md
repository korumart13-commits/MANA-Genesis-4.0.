# MANA OS — hybrid platform architecture

Status: product architecture and engineering plan. This document is not a bootable ROM, proof of Google certification, or permission to redistribute proprietary realme/Google components.

## Product decision

The product name is **MANA OS**. The goal is a MANA-owned system experience and platform layer for the realme RMX5566RU / RE645F target, while retaining the realme hardware-support foundation and compatibility with Android applications.

"Replace Android" means replacing and maintaining the user-facing system layer and selected platform services under the MANA product identity. It does **not** mean deleting every Android compatibility component. Android application compatibility depends on Android framework APIs, ART/runtime, package manager, permissions, Binder, media, graphics and other platform contracts. Removing those wholesale would also remove ordinary APK compatibility and Google Play compatibility.

## Platform composition

### 1. Retain from realme / OEM foundation

Retain only exact-device, version-matched components whose provenance and redistribution/use rights are established:

- Boot chain, firmware, partition layout and Verified Boot configuration.
- OEM kernel and device-specific device tree, DTS/DTBO, modules and configuration.
- Vendor and ODM partitions, proprietary HALs, firmware, VINTF declarations and init integration.
- Device-specific camera, audio, radio/telephony, display, touch, sensors, charging, Wi-Fi and Bluetooth support.
- Realme applications and services that are lawfully available for this build and remain compatible with the new platform.
- Realme installer/update components only where signature, package-installer and update interfaces remain compatible and safe.

Do not assume that a Realme APK copied from the stock phone will run on a different framework build. Preserve a compatibility inventory, package signatures, required shared libraries, privileged permissions, overlays, providers and service dependencies. Keep the stock recovery/restore route intact until the port is proven.

### 2. MANA-owned system layer

MANA should own the product identity and the parts we can implement and validate:

- MANA framework-facing system services and system app suite.
- MANA Launcher with a Samsung One UI-inspired interaction model: home grid, dock, folders, widgets, app drawer, search, gestures and adaptive layouts; original MANA navy/blue visual design.
- MANA SystemUI integration for status bar, notifications, quick settings, lock screen and recents. This must be platform integration, not an overlay pretending to replace SystemUI.
- MANA Settings and Control Center, while preserving real Android permission controllers and device settings operations until equivalent implementations are tested.
- MANA Brain, local-first inference, authenticated provider bridge, optional consent-based cloud providers, encrypted local memory and offline mode.
- MANA avatar/voice surface, Diary, Life Manager, Contact Intelligence, Smart Receptionist, Game Intelligence and Future Updates, all permission-gated.
- Signed update manager, diagnostics, compatibility reporting, rollback and recovery UX.

### 3. Android application compatibility layer — deliberately retained

Keep the Android compatibility foundation needed by third-party APKs:

- Android framework/API surface and system-server contracts.
- ART runtime, Zygote, package manager, Binder IPC, permissions/AppOps and package installer.
- Android graphics, media, audio, input, notification, accessibility and networking APIs.
- Android SDK/API compatibility at the selected Android 16 baseline.

MANA may fork, rebrand, extend and progressively replace selected framework services, but must preserve API and behavioral compatibility. Removing the compatibility layer is explicitly out of scope for this product.

### 4. Google services and Play Store

Google Mobile Services and Google Play are proprietary, licensed components; they are not part of AOSP and cannot be recreated by renaming open-source packages.

Integration path:

1. Keep the platform interfaces required by the relevant GMS release.
2. Obtain Google-approved licensing/distribution and device certification where required.
3. Use an authorized GMS package set and signing/privileged-permission configuration; never extract, redistribute or silently preinstall proprietary Google APKs without rights.
4. Validate Play Store installation/update, Play services, account sign-in, push messaging, location and Play Integrity separately. Some apps may still refuse to run on an uncertified or unlocked/custom platform.
5. If licensed GMS is unavailable, provide an explicitly optional alternative app distribution path; do not label it Google Play or claim full GMS compatibility.

## System app policy

- Prefer Android 16/AOSP-compatible Camera, Dialer, Contacts, messaging and utilities when they match the device and are included in the build.
- Realme camera and telephony components may be required for proprietary HAL features. Choose the exact working OEM/AOSP combination after device-tree and dependency audit; do not promise that a generic AOSP camera app will support every sensor feature.
- Keep package installer and package manager security controls. Never grant all permissions automatically.
- Preserve user data and provide rollback/restore procedures before any platform migration.

## Security boundaries

- SELinux enforcing, AVB and verified boot chain retained; no disabling verification to force a boot.
- No root requirement for ordinary MANA features.
- Privileged actions use explicit permissions, allowlists and confirmation for sensitive operations.
- MANA Brain local model files are integrity-checked; cloud requests are opt-in per request.
- No hard-coded API credentials, signing keys or owner identifiers.
- OTA/update packages must be signed and rollback-safe.

## Device-specific release gates

Before a RE645F build is considered usable, establish exact stock firmware fingerprint, partition map, boot image headers, kernel source/config, device tree, vendor/ODM blobs, VINTF compatibility, SELinux policy and AVB chain. Reproduce the stock kernel/device build before changing it.

Test in stages: Soong module build; host/unit tests; emulator or equivalent integration tests; image build; first boot with recovery available; then hardware acceptance for camera, calls/SMS, audio, display/touch, Wi-Fi, Bluetooth, GPS, sensors, charging, suspend/resume, encryption and OTA rollback.

No guessed device target, generic SM6225 kernel substitution, blind vendor mixing, bootloader unlock or flashing is authorized by this plan.

## Existing MANA project modules to carry forward

Use the existing repository implementations and their evidence ledger rather than rewriting them as empty placeholders:

- Canonical MANA Brain core, facade and Binder/AIDL work.
- Companion LiteRT-LM local inference runtime and model integrity/download flow.
- Cloud provider adapters for OpenAI, Anthropic, Gemini and xAI, with explicit consent.
- Yandex AI Studio and VK integrations where configured by the owner.
- Encrypted Memory, Diary, Life Manager and Adaptive Intelligence.
- Avatar/voice prototype and outfit/animation assets.
- Smart Receptionist, Contact Intelligence, Game Intelligence and Minecraft integration.
- Future Updates inbox/state machine and Android device-action allowlist.

Every module keeps its current implementation status from `os/IMPLEMENTATION_LEDGER.md`. A catalog entry or architecture description is not proof of completion.

## Milestones

1. Freeze this hybrid product definition and audit current source/branch.
2. Establish a pinned Android 16 AOSP source manifest and reproducible CI sync.
3. Inventory exact realme firmware, kernel, vendor/ODM and app dependencies for RE645F.
4. Build the MANA product layer while preserving Android API/runtime compatibility.
5. Integrate the One UI-inspired MANA Launcher and true platform SystemUI.
6. Complete the authenticated local inference bridge and integrate MANA Brain services.
7. Audit Realme apps/installers for framework and signature dependencies.
8. Resolve licensed GMS/Google Play availability and run compatibility tests.
9. Build images only after device, security, recovery and signing gates pass.
10. Run exact-device acceptance; only then consider a release/OTA.

## Current reality

The repository is not yet a complete Android 16 source checkout or a validated RE645F device port. No bootable MANA OS image, GMS-certified image or flashable OTA is produced by this architecture change. The separate companion APK remains an application prototype, not the operating system.


## MANA-first intelligence architecture — primary product principle

MANA is the product's primary intelligence and orchestration layer. The operating system is the trusted execution environment, device abstraction and human interface through which MANA helps its owner. The relationship is not "Android assistant app"; it is "MANA intelligence operating a MANA system built on a compatible platform".

### MANA Brain as the central orchestrator

- One identity and policy engine: MANA Brain routes tasks to the best available capability and presents one coherent assistant, memory and avatar.
- Multi-provider reasoning: local LiteRT-LM inference first when available; optional OpenAI, Anthropic, Google Gemini, xAI, Yandex and other explicitly configured providers. Provider choice, data sharing and cloud use are visible and consent-based.
- Capability registry: every callable system, app, device, network, productivity, accessibility and automation capability declares its permission, scope, risk, availability and audit behavior.
- Planner/executor: break user goals into steps, check prerequisites, execute permitted actions, verify results and report failures honestly.
- Persistent owner-controlled memory: encrypted local storage, inspectable records, retention controls, export and deletion; no hidden collection.
- Learning and improvement: opt-in retrieval/knowledge updates, evaluated prompt and workflow improvements, local personalization, test-driven code proposals and versioned model/provider updates. This is not unlimited self-training of model weights or guaranteed autonomous capability growth.
- Model independence: no single model vendor is the system identity. Providers can be changed without changing MANA's identity or core policy.

### MANA authority and system control

MANA should be able to control every function that the platform and hardware legitimately expose through documented APIs, system roles and privileged services, including:

- Launching and coordinating apps; managing permitted files and documents.
- Calling, messaging and contacts only with the required roles, permissions and user intent.
- Camera, microphone, media, screen brightness, volume, connectivity, location and other device controls where Android/realme APIs allow them.
- Notifications, alarms, reminders, routines, accessibility workflows and supported automation.
- App installation/update orchestration through the platform Package Installer and Play Store user-authorized flows.
- Device diagnostics, battery/thermal-aware scheduling, model management and update planning.

MANA does not bypass Android's sandbox, silently grant runtime permissions, impersonate the owner, defeat account security, hide activity, or obtain arbitrary root/kernel access by assertion. Sensitive operations (payments, account changes, app installs from unknown sources, calls/messages, deletion, firmware/OTA, security settings) require explicit owner authorization or the OS's normal confirmation UI. Emergency stop, audit history and revocation remain available.

"Full control" is implemented as the broadest legitimate, testable set of owner-authorized capabilities—not an unsafe unrestricted shell or an impossible promise that every API/hardware function is accessible.

### Continuous self-development without uncontrolled self-modification

- MANA can observe its own health and capability registry, identify missing features, research documented APIs and propose implementation plans.
- It can generate code changes in an isolated development workspace, run unit/static/security tests and build reproducible artifacts in CI.
- New capabilities enter a staged lifecycle: proposal → implementation → tests → security/license review → owner approval → signed update → rollback-capable deployment.
- Security-critical components, boot chain, kernel, SELinux policy, signing keys and firmware cannot be silently rewritten by the running assistant.
- No self-granting permissions, no silent policy weakening, no unreviewed code execution on the host OS and no self-issued signing authority.
- The owner can pause learning, disable providers/capabilities, inspect changes and roll back updates.

### Human-facing experience

- A persistent MANA assistant entry point with the original 3D avatar and expressive animation, plus accessible non-overlay controls.
- One coherent UI combining Android's flexibility and app ecosystem with iOS-inspired clarity, consistency, animation quality, privacy controls and continuity concepts.
- One MANA design system: deep navy/blue palette, adaptive home screen, widgets, app drawer, dock, quick controls, notification center, lock screen, settings and search.
- Samsung One UI-inspired home-screen organization can be retained as a design reference; iOS is a second design reference, not a source of proprietary binaries or code.
- Keep real Android app behavior and accessibility; do not make the avatar the only way to operate the phone.

### Android, Google and Apple ecosystem boundaries

- Android APK support and Google Play require the Android framework/API, ART, package manager, Binder and compatible GMS interfaces. Keep these as compatibility foundations while MANA owns the primary assistant and UX.
- Google Play remains a user-facing app/service. MANA can help search, open, organize and initiate owner-approved Play Store actions through supported interfaces. Google account authentication remains Google's secure sign-in flow; MANA does not collect Google passwords or bypass account security.
- Google Mobile Services are proprietary and require appropriate licensing, distribution rights and device certification. Play Integrity and individual app compatibility must be tested; never claim certification without evidence.
- Apple ID/App Store access is not equivalent to Android app compatibility. Apple account sign-in, App Store purchase/download flows and iOS app execution are not promised on MANA OS. Apple services may be used only through official apps, web interfaces, APIs or interoperability programs that Apple actually makes available for Android and the target region/device.
- Cross-device continuity with Apple devices is a future integration project limited to official, documented protocols and available apps/APIs. No copying of iOS frameworks, private APIs, App Store client, Apple binaries or proprietary system components.

### Existing modules — retain and integrate, do not replace with empty mockups

Carry forward the current implementations and their evidence/status in `os/IMPLEMENTATION_LEDGER.md`: MANA Brain core and Binder/AIDL work; local LiteRT-LM companion runtime and integrity-checked model flow; OpenAI/Anthropic/Gemini/xAI adapters; Yandex AI Studio and VK integrations; encrypted Memory, Diary, Life Manager and Adaptive Intelligence; avatar/voice prototype; Smart Receptionist; Contact Intelligence; Game Intelligence and Minecraft integration; Future Updates workflow; allowlisted Android device actions.

The existing APK is a companion prototype and the existing JVM/Android modules are components, not a completed MANA OS image. Integration and device acceptance remain separate milestones.


## Autonomous research is the primary experiment

MANA Genesis is intended to study how a persistent AI develops when it is given initiative, memory, tools and an evolving set of owner-defined goals. Device control is one capability domain, not the project's central purpose.

The initial autonomy profile is **maximum initiative inside MANA's own research environment**:

- MANA may independently choose research questions, break goals into experiments, compare hypotheses, inspect public read-only sources, organize its memory, reflect on outcomes and propose new skills.
- A scheduler may continue bounded research cycles without waiting for a new user prompt. Every cycle records the goal, method, observation, evidence and reflection.
- A skill or capability improvement is versioned and tested before it is treated as a successful improvement. An unverified outcome remains a hypothesis, not a learned fact.
- The owner may add explicit rules at any time (allow, forbid, or require approval for a named capability). A prohibition takes precedence over the default autonomy profile.
- With no custom owner rules, internal sandbox work and public read-only research are autonomous by default. Device operations remain limited to capabilities actually granted by Android. External side effects remain subject to the platform's authorization and owner confirmation.
- MANA must not silently export private memory, credentials or personal data to a cloud model. Cloud inference and external account access use their explicit provider consent paths.

This is not a claim of unrestricted self-modification or guaranteed model-weight training. The first implementation is an auditable research-and-reflection loop with pluggable planners, runners and journals. Persistent scheduling, long-term journal adapters, skill evaluation and later model adaptation are separate implementation milestones.
