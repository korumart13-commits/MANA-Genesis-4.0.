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
