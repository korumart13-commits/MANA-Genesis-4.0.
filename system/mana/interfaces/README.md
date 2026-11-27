# MANA Brain — Android Binder interface v1

This directory defines the platform-facing AIDL contract for connecting Android product
components to the single MANA Brain runtime.

## Contract

- `IManaBrain` exposes a protocol version, runtime state and asynchronous `think()`.
- `ManaBrainRequest` carries the prompt and per-request cloud, sensitivity and memory consent.
- `IManaBrainCallback` returns exactly one result or an error for each accepted request.
- `ManaBrainResponse` reports the text, provider, whether inference was local and memory-use count.
- The Soong module `mana-brain-interface` compiles the contract for platform-side clients.

## Security requirements

- The current service endpoint is protected by the signature-level permission
  `com.mana.permission.BIND_BRAIN`.
- Binder entry points repeat the permission check and never trust caller-supplied identity.
- Cloud access defaults to off and requires explicit per-request consent.
- Sensitive requests must remain local.
- Personal-memory cloud export requires separate explicit consent in addition to cloud and
  personal-memory inclusion flags.
- Requests are asynchronous; inference must not run on Binder's main transaction thread.
- Errors must not leak API keys, private prompts, memory contents or filesystem paths.

## Integration status

The AIDL contract is implemented by the permission-protected Android Service endpoint in
`system/mana/services/ManaBrainService`. That endpoint deliberately returns
`ENGINE_UNAVAILABLE` until a real local inference backend adapter is connected. It does not
fabricate model output or forward prompts to a cloud provider.

The Gradle application service remains the currently tested process owner. Cross-process backend
integration, Soong build validation, SELinux service context/domain and device-image testing remain
subsequent work. No AOSP ROM build or device runtime test is claimed.


## App-engine backend contract

IManaEngineBackend is the reverse-side AIDL contract used only between the platform Brain host and the signature-protected com.mana.launcher engine bridge. It carries the same ManaBrainRequest contract and asynchronous result/error callback. The platform host remains responsible for authorization and policy checks; the app bridge repeats checks and delegates to the single process-owned runtime. The interface declaration alone does not package or platform-sign the app bridge.


## Platform signing and privilege boundary

The Gradle release APK is intentionally unsigned. The AOSP staging script imports it with
`android_app_import`, `presigned: false`, and `certificate: "platform"`, so Soong signs
the imported APK with the AOSP product's platform certificate. The private `platform.pk8`
must remain inside the trusted build/signing environment and must never be committed to this
repository or included in user-facing artifacts.

Both the imported MANA app and the platform Brain service are product-specific applications,
not privileged applications. They do not need `privileged: true` to share a signature-level
permission: the launcher and Brain service must instead be signed by the same platform key.
This intentionally avoids granting the launcher a broader privileged-app permission surface.
The platform service still uses platform APIs and the signature-protected Binder permission.

The integration chain is:

1. Gradle release APK package name is `com.mana.launcher`; its manifest must declare
   `ManaEngineBridgeService` and request `com.mana.permission.BIND_BRAIN`.
2. Stage the APK under `vendor/mana/genesis/system/mana/apps/ManaLauncher/prebuilts/`.
3. Soong imports and platform-signs that APK as `ManaLauncher`.
4. The separately built `ManaBrainService` is also signed with `certificate: "platform"`.
5. At runtime, `ManaBrainService` binds to the explicit
   `com.mana.launcher/.ManaEngineBridgeService` component; both sides enforce the
   signature permission and validate the AIDL protocol version.
6. After a real AOSP build, verify the resulting APK signer certificate against the exact
   platform certificate used by that product, then run Binder authorization/callback tests.

This is source-level integration preparation only. No AOSP Soong build, signed APK output,
SELinux policy integration, emulator test or RMX5566 runtime test has yet been completed.
