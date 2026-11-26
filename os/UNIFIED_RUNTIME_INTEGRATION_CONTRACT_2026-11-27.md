# MANA Genesis 4.0 — Unified runtime integration contract

Status: engineering contract / integration gate (not an implementation claim)
Target: Android 16; realme RMX5566RU / RE645F; Qualcomm SM6225 / bengal; arm64-v8a
Reviewed: 2026-11-27

## Purpose

Define the single runtime boundary that joins MANA Brain, Shogun, Hydra, MANA Engine and Microworld without creating competing agents or bypassing Android security. MANA Brain remains the single identity and reasoning authority. Shogun schedules tasks. Hydra exposes narrowly scoped capabilities. MANA Engine owns provider selection, context assembly and lifecycle. Microworld is MANA's persistent, bounded, evidence-backed environment.

This document does not claim that these modules are already merged into a bootable Android product.

## Runtime ownership and call direction

- MANA Engine is the composition root. It creates and owns lifecycle-scoped instances of Brain, Shogun, Hydra adapters, encrypted Memory and Microworld.
- Brain accepts a user/task request and returns a typed decision or response. It does not directly invoke Android APIs.
- Shogun accepts typed task plans, applies ordering, cancellation, deadlines and per-step authorization, then delegates to Hydra capabilities.
- Hydra is a registry of explicit capability adapters. Each adapter declares a stable capability ID, input/output schema, required Android permission/role, risk class and audit event. Unknown capabilities are denied.
- Microworld stores spaces, objects, observations with provenance, and experiment records. It does not execute arbitrary scripts, shell commands or network instructions.
- Android-facing adapters live behind interfaces. JVM core must remain testable without Android framework classes.

## Mandatory request pipeline

1. Receive an owner request through the UI/assistant entry point.
2. Build a bounded context from local memory and Microworld; personal memory is included only when the owner opted in.
3. Select local inference first. Cloud providers are considered only when the owner explicitly permits that request; sensitive requests fail closed.
4. Brain produces a typed intent/plan, not executable code.
5. Shogun validates plan structure, limits, cancellation and authorization for every step.
6. Hydra resolves only registered capabilities and checks permission/role again immediately before invocation.
7. Consequential actions (payments, purchases, messages to others, deletion, calls, device changes, external publication) require a separate, clear owner confirmation immediately before execution. No confirmation may be inferred from broad autonomy settings.
8. Record a minimal audit event and return the result. Secrets and unnecessary personal data must not enter logs.

## Capability and safety rules

- Default deny; least privilege; no arbitrary shell, root, hidden input injection, anti-cheat bypass or silent permission escalation.
- API keys are never committed. Cloud credentials remain in Android Keystore-backed storage.
- Network learning is source-attributed, bounded, scheduled under owner-configured constraints, and treated as untrusted data rather than instructions. It may update a local knowledge store; it does not silently train model weights or rewrite executable code.
- MANA may draft and test proposed updates in an isolated build pipeline, but installation, signing, flashing and irreversible changes require explicit owner approval and verified rollback.
- Continuous availability is best-effort and subject to Android background limits, battery/network policy and user controls; do not promise an unrestricted 24/7 process.
- Microworld growth is bounded by storage, size, retention and resource budgets, with pause/resume and owner-visible export/forget controls.

## Required interfaces before product integration

- BrainPort: request + bounded context -> typed response/plan; provider metadata and local/cloud provenance.
- TaskCoordinator: typed plan -> cancellable task result; step-level authorization and audit hooks.
- CapabilityRegistry: capability descriptors and invocation; permission re-check at call time.
- MemoryPort: explicit opt-in read/write, retention and forget operations.
- MicroworldPort: snapshot, pause/resume, create space/object, record sourced observation, create/finish experiment.
- ModelRuntimePort: load/unload, generate, cancellation, token/context limits, health and resource telemetry.
- AndroidServiceBoundary: Binder caller identity validation, signature/permission checks, SELinux domain and lifecycle handling. It must not be simulated by JVM policy tests.

## Integration and acceptance gates

### Gate A — JVM composition
- Instantiate one Engine and prove it owns exactly one Brain, Shogun, Hydra registry, Memory and Microworld.
- Test local-first routing; cloud denied by default; sensitive requests denied from cloud.
- Test authorization on every task step, unknown capability rejection, cancellation and stop-on-denial.
- Test that payment/message/delete/device-change capabilities cannot run without fresh per-action confirmation.
- Test Microworld persistence, tamper rejection, bounds, pause/resume and recovery from interrupted writes.

### Gate B — Android application process
- Compose the same interfaces from Android Keystore, LiteRT-LM and provider adapters.
- Verify model artifact signature/hash, license acceptance, storage/RAM preflight and graceful unload.
- Run instrumentation tests for process death, restart, permission revocation, offline mode, network loss and low storage.
- Validate Russian speech input/output and avatar lifecycle without keeping an unrestricted background service alive.

### Gate C — privileged OS product
- Only after exact OEM sources are verified: move stable service contracts to Binder, validate caller UID/signature/permissions, define SELinux domains and integrate product SystemUI/Settings.
- Build reproducible Android 16 product images with matching vendor/VINTF/HAL, AVB and partition metadata.
- Verify stock restore, recovery and rollback before any installation attempt.

### Gate D — RMX5566RU device
- Verify actual model/board/SoC/kernel fingerprint against the owner-provided runtime audit.
- Test local inference, thermal/battery behavior, microphone/speaker, overlay, telephony roles, Bluetooth and suspend/resume.
- Use a spare or recoverable device first. Do not flash the owner's daily phone until all gates pass.

## Current verified boundary from implementation ledger

The repository ledger reports Shogun/Hydra orchestration and the Microworld/core JVM paths as implemented and tested; the unified Engine has JVM tests and an Android app-process composition prototype. Android local inference, cloud connectors, avatar/voice and personal modules remain prototypes or partial integrations. The Gradle launcher and Soong product shells are separate source generations. There is no complete Android 16 AOSP product build or bootable ROM for the exact RMX5566RU target.

The implementation ledger remains authoritative for module status. Update it only when source, CI evidence and device validation support a status change.
