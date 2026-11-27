# MANA Total Control, Co-Development Link and Security Research Lab

Date: 2026-11-27

## Preserve and extend the existing project

This is an additive subsystem specification for the existing MANA Genesis 4.0 project. Keep the existing MANA Brain, Shogun, Hydra, Microworld, local LiteRT-LM runtime, encrypted memory, cloud provider adapters, Yandex/VK integrations, Android launcher, Engine service, AIDL/Binder platform service, diagnostics, recovery-copy logic, model integrity checks and self-development engine. Do not replace these with a parallel assistant or a new project.

## 1. MANA Total Control & Self-Development

Goal: MANA can maintain and evolve its own source tree and, where a platform build and device integration permit, develop the Android system around it.

Required capabilities:
- Read-only inventory of device model/build, ABI, Android API, installed packages, granted permissions, available storage, services, hardware interfaces exposed by Android and MANA module health.
- Source-tree understanding, dependency graph, missing-feature analysis and prioritized engineering backlog.
- Generate, edit, review and version-control source patches across MANA-owned repositories and explicitly authorized project workspaces.
- Request changes to AOSP/product/device integration sources, kernel/device-tree adapters and system services when source material is available.
- Run static analysis, unit tests, integration tests, compatibility checks, reproducible builds and security scans in an isolated CI/build environment.
- Produce signed, versioned artifacts with SHA-256, provenance, target fingerprint, dependency manifest and rollback metadata.
- Install ordinary app updates through Android's supported package update flow. Platform images only through a verified device-specific updater/recovery path.
- Manage phone functions through documented Android APIs and explicit permissions; use platform services only when MANA is built into a correctly signed system image with matching SELinux policy.
- Maintain an auditable action log, resource budgets, pause/stop control and owner-visible status.

Authority model:
- MANA may autonomously read its own project, propose patches, run isolated tests and prepare artifacts within a granted workspace.
- The owner can grant scoped capabilities for specific resources and actions. Grants are revocable and expire where practical.
- Android runtime permissions, signature permissions, privileged permissions, root, bootloader unlock, platform signing keys and hardware security boundaries cannot be self-granted by an ordinary app.
- Changes to boot, recovery, kernel, vendor, modem, AVB/vbmeta, partition layout, SELinux policy or system trust roots are protected transactions. Require exact-device compatibility evidence, a verified signature, anti-rollback checks, atomic staging, a tested stock restore path and explicit owner approval.
- Never edit the active installed binary or active boot/system partition in place. Build a replacement, verify it, stage it and use the supported updater. Reboot only after the platform confirms a complete, recoverable transaction.
- No arbitrary command execution on the live phone from generated code. Use sandboxed CI/build workers with bounded CPU, memory, time, network and artifact access.

"Full control" means the broadest documented, explicitly granted control supported by the installed Android build and hardware; it does not mean bypassing Android, SELinux, AVB or physical security.

## 2. MANA Co-Development Link (OpenAI/GPT)

The repository already contains an OpenAI Responses API adapter in `engine/mana-brain/CloudProviderAdapters.kt`. Reuse it rather than inventing a second provider implementation.

The Co-Development Link should:
- Connect to OpenAI through the official API adapter using a user-supplied API key stored in Android Keystore; no embedded/shared keys.
- Offer a dedicated engineering session for architecture review, Kotlin/Java/C++/AOSP code generation, bug diagnosis, test design, build-log analysis, documentation and patch review.
- Give GPT only the files, logs and diagnostics explicitly selected by the owner or approved by the workspace policy. Redact secrets, tokens, private keys, personal data and unrelated local content.
- Return proposed diffs and structured task plans to MANA's self-development engine. GPT does not directly write to the live phone or install a ROM.
- Allow MANA to run tests, report failures back to GPT and iterate on a patch. Every iteration remains versioned and attributable.
- Require explicit per-request cloud consent; preserve local-first behavior. API use incurs the owner's provider charges and depends on network/account availability.
- Show provider, model, cost/usage where available, shared context, proposed changes and approval state.
- Keep an offline mode using local inference. No claim of permanent direct ChatGPT-account linkage: the supported integration is the OpenAI API and requires the owner's credentials and service availability.

The assistant in a ChatGPT conversation is not a permanently resident process inside the phone. The API connection is the supported technical bridge for MANA's independent runtime.

## 3. MANA Security Research Lab (safe malware analysis)

Add a defensive research environment, not a virus factory.

Allowed functions:
- Static analysis of APKs, ELF binaries, scripts and source code in a quarantined workspace.
- Signature, hash, permission, manifest, dependency and suspicious-behavior inspection.
- Detection-rule authoring and testing (YARA-like signatures, IOC matching, policy rules).
- Harmless behavior simulation: fake file markers, mock network endpoints, synthetic process events and synthetic persistence indicators, with no real persistence, propagation, credential access or destructive actions.
- Use the standard EICAR antivirus test file where appropriate to verify detection without deploying real malware.
- Controlled malware sample analysis only where lawful, using inert snapshots, no host/phone shared folders, no unrestricted networking and no execution on the owner's daily-use phone.
- Vulnerability reproduction only in intentionally vulnerable local test targets or authorized labs; produce remediation and regression tests.
- Red-team exercises must be scoped, authorized and non-persistent.

Disallowed functions:
- Creating or modifying self-propagating viruses, worms, ransomware, spyware, credential stealers, botnets or destructive payloads.
- Persistence, stealth, evasion, security-tool disabling, unauthorized remote access, data exfiltration or propagation to other devices.
- Running unknown samples on the production phone or allowing generated code to bypass the lab boundary.

The lab's purpose is to make MANA better at defending itself and the owner's devices. Any suspicious artifact remains quarantined and cannot be installed by the self-development engine.

## 4. RMX5566 implementation boundary

Target remains realme C85 4G, RMX5566RU / RE645F, Snapdragon 685 (SM6225/bengal), Android 16, reported build RMX5566_16.0.5.1310(EX01). Keep the existing OEM vendor/kernel/boot chain unless exact matching sources and a tested recovery route are available. MANA's ability to generate code does not prove that generated hardware drivers, HALs or firmware will work.

## 5. Delivery sequence

1. Reuse existing OpenAI provider adapter and connect it to the self-development planning/review interface.
2. Add scoped workspace capabilities and auditable approval flow.
3. Add patch proposal, CI iteration and build-log feedback loop.
4. Add security research lab policy and harmless test harnesses.
5. Implement Android permission onboarding and supported package update integration.
6. Implement platform updater/A-B rollback only after exact RMX5566 device sources, signing and recovery prerequisites are verified.
7. Perform emulator and physical-device validation before any ROM release.

## Current truthfulness rule

A design document, host test or successful APK build is not proof of full phone control, virus-generation capability, a bootable ROM or a successful device flash. Record each capability as planned, source-implemented, CI-tested, emulator-tested or device-verified.
