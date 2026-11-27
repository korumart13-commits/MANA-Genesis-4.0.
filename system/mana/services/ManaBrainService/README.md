# MANA Brain platform Binder service

This Soong product module provides the platform-facing Binder endpoint for the single MANA Brain runtime.

## Security and behavior

- Product-specific Android app signed with the product platform certificate; it is deliberately **not** installed as a privileged app.
- Binding is protected by the signature-level permission `com.mana.permission.BIND_BRAIN`.
- Binder entry points repeat caller permission checks and never trust caller-supplied identity.
- Sensitive requests cannot use cloud providers.
- Personal-memory export to cloud requires separate explicit cloud and memory consent.
- Request validation is covered by the host-side `mana-brain-request-policy-tests` target.
- Accepted requests are dispatched off the Binder transaction thread through a bounded queue of 32 requests.
- Queue overload, backend disconnect and callback disconnect are handled as explicit errors; prompts are not fabricated or silently sent to a cloud provider.

## Cross-process Engine bridge

On service creation, `ManaBrainService` binds to the explicit component
`com.mana.launcher/.ManaEngineBridgeService` using the private `IManaEngineBackend`
AIDL contract. It checks protocol version 1 before accepting the backend as connected.
Requests are forwarded asynchronously to the bridge. The app bridge repeats the signature
permission and request-policy checks and delegates to the existing `ManaEngineProcessOwner`;
it must not create a second model, memory store or Microworld instance.

The Gradle release APK is staged as an `android_app_import` with
`presigned: false` and `certificate: "platform"`. This is required because the APK must
be signed by the same platform certificate as this service. The legacy Java-only
`ManaAssistant` stub is excluded from product packages so there is one central MANA app.

## Soong and Binder verification

- `tools/run_mana_soong_integration.sh` is the canonical real-AOSP runner. It stages the
  unsigned release APK, invokes Soong for the interface, service, launcher and test modules,
  runs host policy tests, and optionally runs the device Binder instrumentation test.
- `system/mana/tests/ManaBrainBinderIntegration` contains the platform-signed
  `ManaBrainBinderIntegrationTests` module. It binds to `ManaBrainService`, verifies
  protocol version 1 and waits for the real app bridge handshake. A disconnected backend
  is a test failure.
- `.github/workflows/mana-soong-contract.yml` validates source contracts and shell syntax
  only. It is not a Soong build and cannot prove that Binder works at runtime.

## Remaining release blockers

No full AOSP Soong build, platform certificate comparison, SELinux service context/domain,
emulator Binder test or RMX5566 runtime test has been completed. The MANA product fragment
must be included by a verified Android 16 product configuration. The source integration
is implemented, but its actual AOSP packaging and runtime behavior remain unverified.

Do not create or flash an RMX5566 image until the exact device tree, matching OEM kernel,
vendor blobs, VINTF, partition/AVB metadata and a tested stock restore route are verified.
