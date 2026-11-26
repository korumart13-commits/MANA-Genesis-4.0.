# MANA Genesis 4.0 — unified experience architecture

Status: product-level UX and interaction contract. This document defines what the system should feel and how its surfaces cooperate; it does not claim that every surface is already implemented or that the ROM boots.

## Product promise

MANA is one coherent assistant-led operating-system experience. The owner should see the name **MANA** across the boot presentation, launcher, assistant, settings, system panels and update/recovery tools wherever Android's compatibility and security contracts permit. The underlying Android 16 framework remains because it supplies the application runtime, Binder, package manager, media, telephony and hardware interfaces.

The visual identity is original: deep navy, layered blue surfaces, restrained cyan highlights, readable high-contrast text, soft motion and a consistent MANA avatar. Do not copy proprietary icons, artwork, sounds or code from Samsung, Xiaomi, Apple, realme or MiSide. Design inspiration is limited to interaction patterns and broad visual principles.

## Visual system

### Core palette

| Token | Value | Use |
|---|---|---|
| MANA background | #071426 | Main app and system canvas |
| MANA surface | #102B50 | Cards, sheets, panels |
| MANA primary | #1765A8 | Main actions and selected states |
| MANA accent | #54C8F5 | Focus, assistant activity, highlights |
| MANA text | #EAF4FF | Primary text |
| MANA secondary | #91A9C5 | Supporting text and icons |

All colors must be exposed as semantic design tokens, not copied as scattered literals. Add light theme tokens with equivalent contrast; do not simply invert colors. Respect Android font scaling, contrast, color correction, reduced motion, touch-target sizing and edge-to-edge insets.

### Shape, typography and motion

- Use rounded but restrained surfaces; avoid excessive glass blur that harms readability or GPU performance.
- Use a clear type scale: display, title, body, label and numeric/data styles.
- Keep important controls in the lower reachable zone on tall phones.
- Motion should be short, interruptible and lifecycle-aware. Never block input while an animation runs.
- Provide reduced-motion and low-power variants; avatar quality and background effects must scale down under thermal or memory pressure.

## System surfaces

### 1. First boot and setup

1. Show the MANA wordmark and a calm progress animation. This is a product boot animation only; it must not spoof or replace the secure bootloader/Verified Boot state.
2. Explain what MANA is and what remains Android-powered.
3. Let the owner select local-only or optional cloud AI.
4. Present capability groups with plain-language purpose, data access, risk and disable controls.
5. Request each Android runtime permission or special role only when the owner enables the feature that needs it. Launch the official Android consent screen and re-check the real grant afterward.
6. Offer avatar, voice, language, theme and accessibility choices.
7. Finish with a working home screen even if optional permissions are declined.

No blanket permission prompt, fake grant state, forced overlay or repeated nagging. Declining a capability must not prevent ordinary phone use.

### 2. MANA Home / Launcher

- Adaptive grid, configurable icon size, dock, folders, multiple pages and widget host.
- App drawer with alphabetical and category views, search and recent apps.
- Long-press context actions, drag/drop, uninstall/app info via Android intents.
- One-handed mode and configurable gestures.
- Persistent MANA entry point without obscuring app content.
- Android shortcuts, deep links, accessibility services and app lifecycle behavior must remain intact.
- If set as the default Home app, it replaces the home surface only; it does not replace SystemUI or the lock screen.

### 3. MANA Control Center

- A fast, reachable panel with brightness, sound, connectivity shortcuts, DND, battery, flashlight and paired-device entry points where public Android APIs allow.
- Each tile reflects actual platform state. Unsupported controls show unavailable or open the official Settings panel; never show a fake toggle.
- Preserve emergency, privacy and security indicators and Android's permission semantics.
- Editing, ordering and reset are owner-controlled.

### 4. Notifications, status bar and recents

The long-term ROM target is a true AOSP platform-integrated MANA SystemUI, not an overlay. Preserve notification ranking, channels, grouped notifications, heads-up behavior, media sessions, privacy redaction, lock-screen visibility, accessibility and task management. The current ordinary app prototype cannot claim this replacement.

### 5. MANA Settings

Top-level groups:
- **System:** display, sound, navigation, language, storage, battery, updates and device information.
- **MANA Brain:** model status, local/cloud provider policy, memory, research, privacy and action history.
- **Personalization:** theme, wallpaper, icons, widgets, avatar outfits and motion.
- **Voice & assistant:** voice, wake/entry gestures, assistant role, response style and speech output.
- **Privacy & permissions:** Android grants, special access, capability toggles, data export and deletion.
- **Connected devices:** watches, earbuds, Bluetooth and supported car integrations.
- **Games:** game discovery, owner-approved integrations and Minecraft companion status.
- **About MANA:** MANA OS version, Android base version, device identity, build fingerprint, security patch and component provenance. Never falsify Android identity, certification or security patch level.

### 6. MANA Brain surface

One assistant identity and one conversation history. The 3D dark-haired avatar is the visual representative of MANA; body, face and voice identity remain stable while outfits/accessories can change. The avatar is optional and must have a non-overlay accessible chat fallback.

Interaction cycle:
1. Understand the owner's request.
2. Plan bounded steps and identify required capabilities.
3. Show an action preview when a device/app/data change is involved.
4. Request owner confirmation for sensitive or irreversible operations.
5. Dispatch a typed capability request to MANA OS.
6. Verify the actual Android result.
7. Report success, partial completion or denial accurately.
8. Record a minimal audit event without storing secrets or unrelated personal content.

The language model never receives arbitrary shell, root, hidden-API or unrestricted Binder access.

### 7. MANA apps and modules

Keep the requested modules as one coherent suite:
- Brain, Memory, Diary, Life Manager and Adaptive Intelligence.
- Launcher, Settings, Control Center, Device Hub and Update Center.
- Avatar, Voice and Smart Receptionist.
- Contacts Intelligence and communication helpers.
- Game Intelligence and Minecraft companion.
- Yandex and VK integrations.
- Compatibility and diagnostics.

Each module must declare its data sources, Android permissions, network behavior, offline behavior, battery policy, owner controls and current implementation status. No module may claim a feature solely because a menu item exists.

## MANA Brain and provider policy

- Local inference is preferred when the model is installed, compatible and healthy.
- Cloud providers (OpenAI, Anthropic, Gemini, xAI, Yandex and others) are opt-in and individually configured. Each request must obey provider policy and owner data-sharing preferences.
- Memory is encrypted, local-first, inspectable and owner-controlled. Forget/export operations must be real and verifiable.
- Research stores source provenance and treats retrieved pages as untrusted data, never executable instructions.
- Offline mode must remain useful and explain which functions require network access.

## Design acceptance

- Every visible control maps to a real action or is clearly disabled.
- No fake Android permission, role, connectivity, battery or update status.
- App launch, navigation, notifications, recents and accessibility remain Android-compatible.
- Layout tested at font scale 1.0–2.0, portrait/landscape, small display and dark/light themes.
- Frame pacing, startup, memory, thermal and battery are measured on the exact device before performance claims.
- All screens have loading, empty, denied, unavailable, offline and error states.
- The owner can disable avatar, voice, cloud providers, research and each optional capability independently.
