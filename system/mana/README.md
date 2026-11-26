# MANA OS system layer — firmware only

**Product scope:** branded Android/Realme-derived firmware and platform integration for the later standalone MANA application.

MANA OS owns the operating-system experience: boot branding, launcher, lock screen, SystemUI, notifications, Quick Settings, Settings, product identity, platform policy and documented system interfaces. The standalone MANA AI application owns the agent, model, avatar, voice/personality, micro-world, memory, learning, game automation and user-facing autonomous behavior.

## Firmware modules

- `ManaLauncher`: HOME activity and MANA OS home-screen shell.
- `ManaSettings`: MANA OS settings entry point and system settings navigation.
- `ManaControlCenter`: current safe control/settings hub; eventual full Quick Settings/SystemUI replacement requires AOSP platform integration.
- `ManaAssistant`: optional thin system integration/entry-point boundary for the separate MANA app. It is not the autonomous agent and must not package the local model, avatar, memory, game agent or learning loop.
- `ManaBrainService` / Binder contracts: only a narrow, authenticated OS-to-app interface when needed; no duplicate agent runtime in the firmware.

## Explicitly out of firmware scope

Do not add an LLM, local model weights, witch/avatar persona, micro-world, voice identity, agent memory, internet-learning engine, messaging agent, autonomous game/Minecraft bot or self-development agent to the firmware. Those belong to the separate MANA app.

## Platform integration requirements

- Prepare the standalone package identity `com.mana.genesis` as a potential default assistant through supported Android roles and VoiceInteractionService contracts.
- If the exact app build and signing identity are verified, consider product preinstallation and narrowly scoped privileged permissions using partition-matched allowlists.
- Expose only documented, purpose-specific Binder/AIDL APIs. Validate UID, package, signing certificate, capability and current grant at the system boundary.
- Keep Android permission enforcement, AppOps, SELinux, Verified Boot, AVB and audit logs enabled.
- Never grant permissions by pretending a runtime or special-access grant has occurred.

## Current implementation state

The Soong source modules for Launcher, Assistant, Settings and Control Center exist. The Launcher and Settings now have MANA dark-navy/cyan resources and theme references. These modules have not yet been integrated into a verified RMX5566RU product build. The device tree and OEM inputs remain incomplete; this directory is not a flashable ROM.
