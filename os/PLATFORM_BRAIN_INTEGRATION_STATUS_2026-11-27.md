# MANA Brain platform integration status — 2026-11-27

## Implemented source components

- AIDL contract: `system/mana/interfaces`.
- Android Binder service endpoint: `system/mana/services/ManaBrainService`.
- Signature-level bind permission: `com.mana.permission.BIND_BRAIN`.
- Repeated permission enforcement inside Binder entry points.
- Per-request cloud and sensitive-data policy checks.
- Separate opt-in flag for personal-memory export to cloud.
- Product package declaration: `ManaBrainService` in `system/mana/mana_product.mk`.
- Service endpoint fails closed with `ENGINE_UNAVAILABLE` until a real local inference backend is connected.

## Deliberately not claimed

The platform service is not yet a working inference host. It is not Soong-built against an AOSP checkout, not installed into a real product image, and has no target-specific SELinux service context/domain. The Gradle app foreground service is a separate application-process prototype; no cross-process backend adapter has been implemented.

The AIDL/API and service source have only source-level CI checks. Current Gradle CI does not compile Soong modules.

## Required next implementation sequence

1. Pin an Android 16 AOSP manifest/branch and establish a reproducible build environment.
2. Build `mana-brain-interface` and `ManaBrainService` with Soong in that tree.
3. Add a dedicated SELinux service type, service_contexts mapping and least-privilege rules only after checking actual AOSP policy and package identity.
4. Implement a backend adapter that connects the platform Binder service to the single local-first MANA engine. Do not duplicate model/memory ownership between processes.
5. Add Binder instrumentation tests for authorization, callback delivery, cancellation, sensitive/cloud rejection and separate memory-export consent.
6. Include the service in a real product build and verify it on an emulator before any device-specific port.
7. Only then attempt RMX5566RU product integration, once the device-source blockers in `device/RMX5566RU_SOURCE_DISCOVERY_2026-11-27.md` are cleared.

## Current test evidence

- The application-side service-owned engine migration passed `:brain:test`, `:core:test`, `:app:assembleDebug` and `:app:testDebugUnitTest` in workflow run 45906472269.
- The later platform Binder service source changes are under a new CI run; they are not covered by that earlier Android build.
- A successful Gradle build is not an AOSP/Soong build and is not a flashable ROM.
