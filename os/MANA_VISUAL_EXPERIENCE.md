# MANA Genesis 4.0 — visual experience and performance principles

## Product identity

MANA owns the visual identity. The interface is a coherent Android 16 experience, not a reskin that removes Android compatibility or copies proprietary vendor code.

## Design references

- **Samsung One UI is the primary UX reference:** large, reachable interaction areas; clear hierarchy; one-handed layouts; adaptable home screen; app drawer; widgets; folders; edge-to-edge content; cohesive system panels and settings.
- **Xiaomi/HyperOS is a secondary system-shell reference:** compact control-center patterns, device hub, visual continuity between home screen, lock screen and settings, and a unified theme system. Use original MANA components and public Android APIs; do not redistribute proprietary HyperOS assets or binaries.
- **iOS is a motion and performance reference:** restrained transitions, consistent spring-like motion, responsive touch feedback, reduced jank, lifecycle-aware background work and disciplined resource use. These are design/performance principles, not imported iOS frameworks or a claim that iOS code runs on Android.

## MANA visual language

- Deep navy and blue foundation, high-contrast text, restrained cyan accents.
- MANA identity and avatar are consistent across launcher, assistant, lock screen entry points and supported system surfaces.
- System-wide design tokens for color, typography, spacing, shape, elevation, motion and accessibility.
- Light/dark themes, dynamic contrast, font scaling, reduced-motion setting and accessible touch targets.
- User can select avatar outfits/accessories; the avatar's identity and voice remain stable.

## Required surfaces

1. Launcher: home pages, dock, folders, widgets, app drawer, search, gestures and adaptive grid.
2. Lock screen: clock, notifications, shortcuts and secure authentication delegated to Android.
3. Quick Settings / Control Center: editable tiles backed by real Android settings APIs; unsupported actions are shown as unavailable, never simulated.
4. Notifications and recents: preserve Android notification semantics, task management and accessibility.
5. Settings: MANA settings grouped by system, intelligence, privacy, devices, avatar, games and updates; permission changes route through Android's real permission UI.
6. Assistant surface: MANA avatar, voice/text interaction, action previews, confirmation prompts, progress and verified outcomes.
7. Device hub: supported watches, earbuds, car and nearby-device connections with explicit pairing and disconnect controls.

## Performance engineering

- Profile frame pacing, input latency, startup, memory pressure, thermal behavior and battery on actual hardware.
- Prefer platform-native rendering and Android lifecycle-aware APIs; avoid persistent background polling and unnecessary wake locks.
- Use bounded executors, structured cancellation, caching with size limits, lazy module loading and adaptive quality for avatar rendering.
- Reduce animation complexity under thermal or battery pressure; honor system animator and accessibility settings.
- Do not promise a speed increase from visual styling alone. Performance claims require repeatable before/after benchmarks on the target hardware.

## Platform boundaries

A regular APK can provide the launcher and assistant experience but cannot replace privileged SystemUI, lock screen, permission controller or framework services. Those require integration into a valid Android 16 product build, correct signing/privileges, SELinux policy and device validation. Google Mobile Services and Xiaomi/realme proprietary components remain subject to their licenses and compatibility requirements.
