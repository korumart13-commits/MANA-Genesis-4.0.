# MANA Genesis 4.0 — Shogun MANA, Hydra and MANA Engine architecture

Status: architecture baseline / implementation plan. This document adds the requested engine and brain architecture without deleting or replacing existing project files.

## Product intent

MANA Genesis 4.0 remains an AOSP-derived Android 16 operating system for the verified target profile RMX5566RU / RE645F. It is not a standalone launcher or a conventional assistant APK. All previously requested capabilities remain in scope; implementation is staged so that each subsystem can be built and tested without pretending that an unbuilt firmware image already exists.

## Names and responsibilities

### Shogun MANA — central brain and system orchestrator

Shogun MANA is the single authoritative AI decision and orchestration layer for MANA OS. It owns intent understanding, task planning, policy evaluation, delegation, result synthesis, and coordination between system services and AI providers. It is the brain of the whole MANA experience, not a second competing assistant persona.

Shogun MANA is implemented on top of the existing `engine/mana-brain` foundation. The existing `ManaBrain` class remains the initial inference-routing component; it is not renamed or discarded merely to satisfy branding. The Shogun layer is added above it and evolves through tested interfaces.

### MANA Hydra — modular capability orchestration

Hydra is the multi-domain execution architecture coordinated by Shogun MANA. Its "heads" are bounded capability modules, not independent agents with unrestricted device access. Initial heads:

- System: approved Android settings and system actions through explicit MANA System API capabilities.
- Memory: user-controlled local memory, retrieval, retention and deletion workflows.
- Knowledge: source-backed research, local knowledge indexing and provenance.
- Voice and Vision: speech input/output and permitted visual understanding.
- Device: status, sensors and supported device controls through Android APIs.
- Communications: phone, contacts, receptionist, Yandex and VK integrations under runtime permissions.
- Life: diary, reminders and Life Manager.
- Game: Minecraft and game-intelligence integrations within the game's supported APIs.
- Maintenance: diagnostics, integrity checks, backup/restore planning, update validation and rollback coordination.

Each head has a declared capability manifest, input/output contract, permission requirements, resource limits, audit events and tests. A head cannot silently grant itself permissions, execute arbitrary shell commands, bypass Android security boundaries, or access another head's private data.

### MANA Engine — runtime and integration layer

MANA Engine is the project-owned runtime integration layer that provides a stable contract between Shogun MANA/Hydra, local inference, cloud providers, system services, memory, tools and user interface. It includes:

1. Request and response contracts, cancellation and streaming.
2. Capability registry and permission broker.
3. Task scheduler, bounded execution and resource budgets.
4. Provider routing and health checks.
5. Event/audit trail and error isolation.
6. Versioned plugin/adapter interfaces.
7. Test harness and compatibility checks.

The existing Kotlin/JVM module `engine/mana-brain` is preserved as the first inference-routing foundation. MANA Engine grows around it in separate packages/modules; existing tests and behavior must remain valid unless a deliberate migration is covered by tests.

## Gecko integration — provisional identification

The spoken name "Geeko" most likely refers to Mozilla Gecko, the browser engine. The repository tree currently contains no Gecko/GeckoView source, dependency, or adapter, so the exact existing engine/module the user remembers has not yet been identified in the current repository snapshot.

If Mozilla Gecko is the intended engine, MANA should integrate the supported GeckoView/runtime as a web-rendering and browsing subsystem behind a project-owned `MANA Engine` adapter. It must not be misrepresented as the engine for Android itself: Android's application runtime, framework, graphics and system services remain AOSP components. The project may customize the integration, UI, policies, extensions and MANA-specific interfaces, while preserving upstream notices, licenses, trademarks and required source disclosures. Do not simply rename upstream Gecko source as wholly original MANA code.

If "Geeko" refers to a different engine already developed in another branch/archive, integrate that exact source after locating and auditing it; do not replace it with Gecko by guesswork.

## Selected local inference runtime

**Selected runtime: Google LiteRT-LM for Android (Kotlin API).** This is the model-execution engine that runs the local LLM on the phone. It is distinct from Shogun MANA (the orchestrator) and from the project-owned MANA Engine (the integration/control layer).

The current repository already contains a concrete Android integration:
- Dependency: `com.google.ai.edge.litertlm:litertlm-android:0.16.1` in `launcher/app/build.gradle.kts`.
- Runtime adapter: `launcher/app/src/main/java/com/mana/launcher/LocalModelRuntime.kt`.
- The adapter creates LiteRT-LM `Engine` and `Conversation`, streams generated messages, attempts GPU first and falls back to CPU, and owns lifecycle on a dedicated background thread.
- `ManaBrainBridge.kt` routes local inference through this adapter before considering any permitted cloud provider.

This means LiteRT-LM is not merely a proposal in a setup note: the Android prototype has a real adapter in source. However, source presence does not prove a successful build, successful model load, stable GPU operation, or acceptable speed on RMX5566RU. Those must be tested on the target device.

### RMX5566RU backend policy

The verified target is Qualcomm SM6225 / Adreno 610. Use CPU as the guaranteed compatibility baseline. Keep GPU as an optional acceleration attempt and automatically fall back to CPU if initialization or inference fails. Do not assume Qualcomm NPU support: no compatible NPU delegate or target-specific model compilation has been verified for this phone.

A second runtime such as llama.cpp may be evaluated as an optional adapter for GGUF model compatibility, but it is not the selected primary runtime. Its published OpenCL Android verification currently names newer Snapdragon/Adreno devices; its documentation warns that phone-class Adreno 6xx devices may have outdated drivers/compilers. Therefore do not promise Adreno 610 GPU acceleration from llama.cpp without a device test.

### Model and release pinning

Keep model weights separate from the OS source and install them only through an explicit owner-controlled model manager. Pin runtime and model versions in the build, verify publisher checksums, record model license/terms, and run memory, latency, thermal and battery benchmarks before selecting a default model. Do not use a floating `latest.release` dependency for a reproducible firmware build.

## Inference and provider policy

- Local inference is preferred and is the offline path.
- LiteRT-LM/model loading, model licensing, device resource checks and lifecycle management are implementation work still required.
- Cloud models (OpenAI, Anthropic, Google, xAI and other explicitly configured providers) are optional adapters.
- Cloud use requires user configuration/credentials and an explicit request policy. Never embed API secrets in source, firmware, APKs or public CI logs.
- Sensitive requests remain local and must fail closed if a safe local provider is unavailable.
- Every result records provider identity and whether processing was local or remote.

## System integration and security

MANA Engine does not gain unrestricted root access. System operations are exposed through narrowly scoped, permission-checked Android system services and MANA System API capabilities. Privileged components are integrated as signed system/product/system_ext modules only as part of a controlled OS build. SELinux remains enforcing; Verified Boot and signing requirements are respected. No stealth persistence, exploit-based privilege escalation, bootloader bypass or hidden data collection is part of this design.

## Build sequence

1. Preserve and test the current `engine/mana-brain` module.
2. Add Shogun orchestration contracts and deterministic unit tests.
3. Add Hydra capability manifests, registry, permission broker and isolated execution contracts.
4. Add MANA Engine adapters for local inference, cloud providers, Android system APIs and (after identification) Gecko/GeckoView.
5. Add encrypted local memory, model lifecycle, voice/vision and user-facing control/consent.
6. Integrate services into the AOSP product build and test on emulator/CI.
7. Only after exact device sources, vendor interfaces, kernel, partitions, AVB/signing and recovery/restore path are verified, produce and validate a device image for RMX5566RU.

## Current verified implementation boundary

The repository currently has a Kotlin/JVM MANA Brain foundation and provider adapter files, but the Brain README explicitly lists the LiteRT-LM runtime, persistent encrypted memory, Android service/UI integration and credential storage as not yet implemented. The repository does not currently contain a complete AOSP source checkout or a validated bootable RMX5566RU firmware image. This architecture document records the requested direction; it does not claim those components have already been built.

## Acceptance criteria

- Existing MANA Brain tests continue to pass.
- Shogun produces a traceable plan and delegates only to registered Hydra capabilities.
- Every capability declares permissions and cannot exceed them.
- Local-first routing and sensitive-request fail-closed behavior are tested.
- Gecko integration, if confirmed, is isolated to its intended browser/web runtime role.
- Provider secrets never appear in repository history or build artifacts.
- AOSP integration passes build, security and device-specific validation before any flashable image is labelled device-ready.
