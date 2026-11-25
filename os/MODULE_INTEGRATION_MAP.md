# MANA Genesis 4.0 — integrated system module map

All listed modules belong to one OS product and one MANA identity. They are not separate assistant personas. This map records the integration boundaries; a listed module is not considered implemented until its source, permissions, tests and product integration exist.

## Boot and platform foundation

| Module | Responsibility | Depends on |
|---|---|---|
| Android platform | AOSP framework, services, ART, Package Manager, permissions | Device tree, vendor, kernel |
| MANA System Core | MANA service lifecycle, dependency ordering, health state, rollback | Android platform |
| MANA Security | SELinux domains, permission gates, encrypted storage, audit | Android security model |
| MANA Update | Signed update verification, staged rollout, rollback checks | Security, storage, recovery |
| MANA Recovery integration | Recovery/update hand-off and restore documentation | Device recovery, AVB |

## User interface and identity

| Module | Responsibility | Depends on |
|---|---|---|
| MANA System UI | Status/navigation surfaces and system overlays | Android framework, security |
| MANA Launcher | Home screen, app drawer, widgets, gestures | System UI, package manager |
| MANA Settings | MANA identity, device, privacy, AI and module settings | System Core, security |
| MANA Control Center | Connectivity, brightness, sound and quick controls | System UI, platform APIs |
| MANA Avatar | Persistent character, animation, expressions and outfit assets | System UI, renderer, Brain |
| MANA Voice | Speech input/output and owner-controlled wake interaction | Audio, permissions, Brain |

## Local-first intelligence

| Module | Responsibility | Depends on |
|---|---|---|
| MANA Brain | Single conversation/router identity and context management | Local runtime, memory |
| Local LLM runtime | LiteRT-LM inference, streaming, cancellation and lifecycle | Supported model, device runtime |
| Model Manager | Import/download, license notice, integrity, storage/RAM checks | Storage, network permission |
| MANA Memory | User-controlled local memory, indexing, export and deletion safeguards | Encrypted storage, Brain |
| Adaptive Intelligence | Source evaluation, approved learning tasks and update proposals | Brain, network opt-in, audit |
| Future Updates | Ideas backlog, plan, build, test and owner approval pipeline | Update, security, audit |

## Personal and communication modules

| Module | Responsibility | Depends on |
|---|---|---|
| Smart Receptionist | Call screening/response where Android and carrier APIs permit; 10-second delay setting | Telephony roles, contacts, voice |
| Contact Intelligence | Contact context and unknown-number handling | Contacts permission, Receptionist |
| Life Manager | Tasks, reminders and schedules | Notifications, calendar permission |
| MANA Diary | Private notes and timeline | Encrypted storage |
| Yandex integration | Official API-based services where licensed and available | Network opt-in, provider credentials |
| VK integration | Official API-based services where licensed and available | Network opt-in, provider credentials |
| Game Intelligence | Game-aware tools and Minecraft integration where permitted | Brain, user-granted game/storage APIs |

## Device ecosystem

- MANA Primary Node: this phone is the primary local node; LAN server remains off by default.
- Smart watch, earbuds and car integrations use explicit Bluetooth/Wi-Fi pairing and scoped permissions.
- No hidden background recording, silent call answering, remote control or public internet listener.
- Every paired device is owner-approved, revocable and assigned minimum permissions.

## Local-first and safety rules

- Local inference is attempted first. Cloud services are disabled unless the owner explicitly enables a provider for a request.
- Sensitive prompts and private memory are never silently sent to a cloud provider.
- Files are never automatically deleted based only on a low-value score. Any cleanup proposal requires review, recoverability and owner approval.
- Phone, contacts, microphone, camera, location, SMS and call permissions remain visible and revocable.
- Avatar appearance/voice identity remains stable; only outfits/accessories are user-changeable.
- AI suggestions cannot silently change system partitions, permissions, boot settings or update signing keys.

## Current implementation status

- Shared MANA Brain routing source is now compiled into the Android launcher prototype and routes requests to LiteRT-LM locally; no cloud adapter is enabled in this build. Target-device inference and model benchmarking remain pending.
- MANA System Core has JVM lifecycle tests and is linked into the Gradle launcher prototype; it is not integrated into AOSP system_server or the Soong product.
- Diary, memory and Life Manager have AES-GCM encrypted file stores, Android Keystore key access in the launcher, owner-controlled deletion and JVM tests. Memory retrieval is currently simple text matching.
- The launcher prototype has a procedural OpenGL ES full-body avatar, Russian Android TTS, a draggable overlay with a visible stoppable foreground notification, game discovery and user-authorized foreground-game detection. This is not yet production System UI and is not device-tested.
- Minecraft Bedrock has an official Script API behavior-pack starter and syntax CI; phone-to-world transport and owner-approved in-game actions remain unimplemented.
- Smart Receptionist and game-action policies are tested, but telephony call screening/answering and actual in-game automation are not implemented.
- Kernel/device support remains blocked on exact RMX5566RU 5.15.197 source/config/device tree/vendor data.
- Production rigged avatar asset, Yandex/VK and multi-cloud adapters, full Android Binder/SELinux enforcement, device ecosystem integrations and complete AOSP product build remain outstanding.
