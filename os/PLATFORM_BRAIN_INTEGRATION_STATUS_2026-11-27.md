# MANA Brain platform integration status — 2026-11-27

## Implemented source components

- AIDL contract: `system/mana/interfaces`.
- Android Binder service endpoint: `system/mana/services/ManaBrainService`.
- Signature-level bind permission: `com.mana.permission.BIND_BRAIN`.
- Repeated permission enforcement inside Binder entry points.
- Per-request cloud and sensitive-data policy checks.
- Separate opt-in flags for cloud use and personal-memory export.
- Dedicated single-thread executor with a bounded 32-request queue and explicit overload response.
- Pure Java request-policy class, Soong host-side JUnit test target, and dependency-free executable host test covering blank prompts, sensitive/cloud rejection, and independent memory-export consent.
- Product fragment `system/mana/mana_product.mk` declares `ManaBrainService` in `PRODUCT_PACKAGES`.
- Service endpoint fails closed with `ENGINE_UNAVAILABLE` until a real local inference backend is connected.

## Deliberately not claimed

The platform service is not yet a working inference host. It is not Soong-built against an AOSP checkout, not installed into a real product image, and has no target-specific SELinux service context/domain. The product fragment has not yet been included by a verified device product makefile. The Gradle app foreground service is a separate application-process prototype; no cross-process backend adapter has been implemented.

The AIDL/API and service source have source-level CI checks only. Current Gradle CI does not compile Soong modules.

## Required next implementation sequence

1. Establish a reproducible Android 16 AOSP checkout and pin its exact manifest revision.
2. Build `mana-brain-interface` and `ManaBrainService` with Soong in that tree.
3. Add a dedicated SELinux service type, service_contexts mapping and least-privilege rules only after checking actual AOSP policy and package identity.
4. Implement a backend adapter that connects the platform Binder service to the single local-first MANA engine. Do not duplicate model/memory ownership between processes.
5. Add Binder instrumentation tests for authorization, callback delivery, cancellation, sensitive/cloud rejection and separate memory-export consent.
6. Include the service in a real product build and verify it on an emulator before any device-specific port.
7. Only then attempt RMX5566RU product integration, once the device-source blockers in `device/RMX5566RU_SOURCE_DISCOVERY_2026-11-27.md` are cleared.

## Current test evidence

- The application-side service-owned engine migration passed `:brain:test`, `:core:test`, `:app:assembleDebug` and `:app:testDebugUnitTest` in workflow run 45906472269.
- Workflow run 45907689204 completed all substantive source checks, Brain/Core tests, Android compile and app unit-test steps, and both artifact uploads; GitHub was still executing post-job cleanup at the last poll.
- The platform service source is checked by static CI assertions, not compiled by Soong.
- A successful Gradle build is not an AOSP/Soong build and is not a flashable ROM.

## Binder execution hardening

Accepted requests are dispatched onto a dedicated single-thread executor rather than performing backend work on the Binder transaction thread. A disconnected callback is handled without crashing the service, and the executor is shut down in `onDestroy()`. Empty prompts, sensitive+cloud requests, and cloud memory export without both consents are rejected before work is queued.

Service implementation commit: `88d6cc6a707cb92898a746ccf1143c011cbec37f`.
CI assertions for executor dispatch and lifecycle cleanup: `5fdf432eb9dc4dcd6d9fa21ffe0f7415b6f24ace`.


## Request policy test coverage added

The request-policy checks were extracted from the Android Service into the Android-independent
`ManaBrainRequestPolicy` class. A Soong `java_test_host` target named
`mana-brain-request-policy-tests` now covers null/blank prompts, sensitive cloud denial,
missing consent combinations, valid local requests, valid cloud requests without memory export,
and memory export with both required consents. CI checks the test target's source wiring; the
host-side Soong test has not yet been executed because a full AOSP/Soong checkout is not available.


The dependency-free host test is executed directly by the Gradle verification workflow using
`javac` and `java`, so policy behavior is exercised even without an AOSP checkout. The
Soong `java_test_host` target remains available for execution in a full AOSP tree.


## Request policy hardening

The Binder request policy now also enforces a 32,768-character prompt ceiling, rejects a cloud-provider selection on local-only requests, and validates explicit provider names against the supported set (OpenAI, Anthropic, Google/Gemini, xAI/Grok, Yandex and VK). The Binder service passes the requested provider through the policy validator before queueing work. Host tests cover oversized prompts, unknown providers, provider selection on local-only requests, and accepted provider aliases.


## Cross-process engine bridge implemented in source

- Added IManaEngineBackend and its callback to the shared AIDL interface module.
- Added ManaEngineBridgeService to the Gradle Android application. It exposes the existing app-process engine over Binder, checks the signature permission again at each Binder entry point, validates prompt size/provider/cloud policy, and returns asynchronous results.
- The platform ManaBrainService now binds explicitly to com.mana.launcher/.ManaEngineBridgeService, negotiates protocol version 1, forwards validated requests and returns engine results/errors to the original system client.
- Added a reference-counted process owner so the foreground app service and remote bridge share exactly one local model, Brain, encrypted memory and Microworld runtime. Releasing one service no longer shuts down resources still held by the other.
- Added an explicit memory-query field and passed the independent personal-memory/cloud-export consent through to the existing core request policy.
- The Gradle app compiles the shared AIDL source directory; CI now checks the bridge contract and ownership boundaries.

## Bridge integration constraints — not yet a working device integration

The bridge implementation is source-complete but not yet verified by an Android build after these changes or by Soong/device tests. The Gradle application must be packaged into the AOSP product as package com.mana.launcher and signed with the same platform certificate as ManaBrainService; the current standalone debug APK does not satisfy the signature permission. The existing Soong ManaLauncher product module is a separate minimal Java home-app stub and does not yet package the Gradle Kotlin engine or ManaEngineBridgeService. The current product therefore cannot resolve the bridge endpoint until that packaging/signing integration is completed. Until then the platform service reports a disconnected/unavailable backend and does not invent output.


The shared engine owner also now bootstraps a previously saved model asynchronously when either the foreground engine service or the platform Binder bridge acquires the runtime. It only attempts a model already saved/selected on the device; it does not download a model, accept a license, or silently fetch gigabytes. Model hash verification and LiteRT-LM initialization run off the main thread. If no approved/selected model is present, the engine reports that no local model is ready and remains fail-closed for local inference.
