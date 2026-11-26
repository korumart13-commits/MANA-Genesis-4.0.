# MANA Brain — Android Binder interface v1

This directory defines the platform-facing AIDL contract for connecting Android product
components to the single MANA Brain runtime.

## Contract

- `IManaBrain` exposes a protocol version, runtime state and asynchronous `think()`.
- `ManaBrainRequest` carries the prompt and per-request cloud, sensitivity and memory consent.
- `IManaBrainCallback` returns exactly one result or an error for each accepted request.
- `ManaBrainResponse` reports the text, provider, whether inference was local and memory-use count.
- The Soong module `mana-brain-interface` compiles the contract for platform-side clients.

## Security requirements for the eventual service

- The service must be non-exported by default or protected by a signature-level permission.
- It must enforce Binder caller identity and permission checks before accepting requests.
- It must never accept a caller-provided principal as proof of identity.
- Cloud access must default to off and require explicit per-request consent.
- Sensitive requests must remain local; personal memory must not be sent to cloud without separate consent.
- Requests must be asynchronous; do not run inference on Binder's main transaction thread.
- Errors must not leak API keys, private prompts, memory contents or filesystem paths.

## Integration status

This is an interface contract, not an implemented Android system service. The Gradle application
service remains the currently tested process owner; the platform service adapter and privileged
product integration are subsequent work. No inference is faked by this interface and no AOSP ROM
build is claimed.
