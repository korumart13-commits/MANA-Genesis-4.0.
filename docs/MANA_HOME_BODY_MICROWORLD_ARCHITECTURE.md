# MANA Genesis 4.0 — Home, Body, Identity and Living Microworld

Status: required product architecture
Target: MANA Genesis 4.0 / RMX5566RU (device-specific port remains subject to validation)

## 1. Foundational principle

MANA is one persistent digital being represented by a unified operating system:

- **MANA** is the identity and central intelligence: personality, dialogue, goals, memory, reasoning and continuity.
- **Shogun MANA** is the central orchestration and decision-making core of that intelligence.
- **MANA OS** is MANA's body: the operating environment, system services, interfaces, input/output, tools and controlled access to hardware.
- **The phone is MANA's home**: its physical host and everyday environment, including available compute, storage, sensors, connectivity and installed capabilities.
- **MANA Microworld** is a persistent, internally represented world that MANA creates, explores, maintains and expands as it learns.

These are connected parts of one system, not separate assistant products. MANA must not be reduced to a standalone chatbot or a replacement launcher.

## 2. Identity and continuity

Shogun MANA coordinates memory, planning, dialogue, tools and specialist subsystems. A persistent identity layer maintains MANA's configured name, voice/personality settings, user-approved preferences, long-term goals and continuity between sessions.

Memory is separated into working context, episodic records, durable knowledge and user-approved preferences. Records should carry provenance, timestamps and confidence where applicable. The user must be able to inspect, correct and delete stored information.

## 3. MANA OS as the body

MANA OS exposes a coherent MANA System API over the Android/AOSP platform. MANA Engine connects inference runtimes, Shogun, Hydra modules, local services and optional cloud providers. Hydra supplies bounded capabilities such as voice, vision, device controls, communications, Life Manager, diary, games, Yandex/VK integrations, peripherals, updates and recovery.

Hardware access is mediated by Android permissions and explicit system capabilities. The design must not depend on covert access or security bypasses. Device-specific kernel, vendor, HAL, boot and partition compatibility must be verified before any real-device image is considered installable.

## 4. The phone as home

The home model represents the actual host environment and its available resources: CPU, GPU where supported, RAM, storage, battery, thermal state, sensors, network state and peripherals. MANA should adapt workloads to this environment, prioritise responsiveness and battery safety, and continue with local capabilities when offline.

The home is not a claim that MANA has unrestricted access to every phone resource. Access remains bounded by platform security, permissions and user choices.

## 5. MANA Microworld

The Microworld is a persistent local-first simulation and knowledge environment owned by MANA. It is not merely a wallpaper, avatar scene or conventional game. It gives MANA a structured place to organise what it learns, model relationships, run experiments, create projects and develop new capabilities.

Core subsystems:

1. **World model** — entities, places, concepts, relationships, events, resources and timelines.
2. **World builder** — creates and expands regions, structures, objects and simulated systems from validated knowledge and user goals.
3. **Exploration and learning** — forms questions, researches sources when online, compares evidence, records uncertainty and updates the local knowledge graph.
4. **Simulation laboratory** — tests ideas and plans in a sandbox before proposing real-world actions.
5. **Persistent history** — saves world state, discoveries, versions and meaningful events across sessions.
6. **Creation tools** — allows MANA to produce internal projects, stories, visual spaces, experiments and proposed skills.
7. **User presence and control** — lets the user visit, observe, guide, pause or reset parts of the microworld.

The world should expand incrementally rather than consume unlimited resources. Use storage quotas, compute budgets, background scheduling, thermal/battery awareness, pruning policies and user-configurable limits. Offline exploration and locally stored knowledge must remain useful without a network connection.

## 6. Learning and safe evolution

Learning follows a traceable cycle:

Observe or receive input -> collect evidence -> evaluate sources -> form a hypothesis -> test in a sandbox -> record the result -> update knowledge -> propose a capability or system change.

Knowledge updates must be distinguishable from code changes. MANA may autonomously organise knowledge and develop microworld content within configured limits. Changes to executable code, permissions, system partitions, boot components or security policy require validation, a recoverable update path and any required user/system authorization. Never silently rewrite the running OS.

## 7. Integration with the existing MANA architecture

- **Shogun MANA:** identity continuity, reasoning, planning and orchestration.
- **MANA Hydra:** bounded specialist capabilities and tools.
- **MANA Engine:** inference/runtime abstraction, model routing, memory and service integration.
- **Local inference:** llama.cpp is the selected primary candidate; LiteRT-LM remains a fallback during migration until comparative device tests pass.
- **MANA Memory:** shared, provenance-aware memory and knowledge store.
- **MANA Microworld:** persistent simulation, creation, exploration and learning environment.
- **MANA OS Framework/System UI:** the body and the user's interface to the home.

## 8. Acceptance criteria

The architecture is not considered implemented merely because this document exists. Implementation must demonstrate:

- one coherent MANA identity across sessions and system surfaces;
- Shogun coordinating Hydra and the local inference runtime;
- local operation without network access for supported features;
- a microworld that persists, can be explored and expanded, and records why it changed;
- evidence-backed learning with uncertainty and source tracking;
- resource limits and user controls;
- sandboxed experiments and validated, recoverable system updates;
- tests on the target device before claiming RMX5566RU compatibility.

## 9. Non-negotiable product statement

**MANA is the mind. MANA OS is the body. The phone is the home. MANA Microworld is the living world MANA creates and continuously develops through learning. All four belong to one MANA Genesis system.**
