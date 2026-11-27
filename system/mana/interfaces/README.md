# MANA Brain — Android Binder interface v1

This directory defines the platform-facing AIDL contract between Android system components and the single MANA Brain runtime.

## Contract

- `IManaBrain` exposes protocol version, runtime state and asynchronous `think()`.
- `ManaBrainRequest` carries the prompt and per-request cloud, sensitivity and memory-consent flags.
- `IManaBrainCallback` returns a result or error.
- `IManaEngineBackend` is the private reverse-side contract from platform Brain to the app-owned Engine bridge.
- `ManaBrainResponse` reports text, provider, local/cloud execution and memory-use count.
- Soong module `mana-brain-interface` compiles the shared AIDL types.

## Security requirements

- The services are protected by signature permission `com.mana.permission.BIND_BRAIN`.
- Both Binder endpoints repeat caller permission checks.
- Cloud access defaults to off and requires explicit per-request consent.
- Sensitive requests must remain local.
- Personal-memory cloud export requires separate cloud and memory consent.
- Inference is dispatched asynchronously, not on the Binder transaction thread.
- Errors must not leak API keys, private prompts, memory contents or filesystem paths.

## Implemented source-level route

1. Platform-signed `ManaBrainService` exports `IManaBrain` under the signature permission.
2. On creation it explicitly binds to `com.mana.launcher/.ManaEngineBridgeService`.
3. It negotiates backend protocol version 1 through `IManaEngineBackend`.
4. The app-side bridge validates the caller and request policy, then delegates to the single `ManaEngineProcessOwner`.
5. Results and errors travel asynchronously through the AIDL callbacks.

The bridge does not create a second inference engine or memory store. A missing or mismatched backend fails closed with a disconnected/unavailable state. A local model that is not ready is reported as a truthful engine state/error; it is not represented as a successful inference.

## Signing and product boundary

The Gradle release APK is unsigned. The AOSP staging script imports it with
`android_app_import`, `presigned: false` and `certificate: "platform"`, so Soong signs
it using the product's platform certificate. `ManaBrainService` and the instrumentation
test use the same platform certificate. Both production apps remain product-specific,
non-privileged applications; they do not require `privileged: true` to share a signature
permission. Private signing keys must never be committed to this repository.

## Verification status

- GitHub Actions source-contract checks validate the AIDL declarations, component names,
  permission boundaries, test-module definition and runner syntax.
- A device-side test module exists at
  `system/mana/tests/ManaBrainBinderIntegration`. It checks protocol negotiation and
  waits for the platform service to complete the app-bridge handshake.
- A real Soong build, signed APK certificate comparison, SELinux policy integration,
  emulator/device Binder execution and RMX5566 runtime test are still pending.

A green source-contract workflow is not evidence of a compiled AOSP image or a live Binder
connection. Do not mark the bridge operational until the actual Soong build and instrumentation
test logs are green.
