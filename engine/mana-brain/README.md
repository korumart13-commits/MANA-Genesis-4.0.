# MANA Brain Engine — Stage 1

This is the first independently testable Kotlin/JVM core for MANA Genesis 4.0.

## Implemented
- Local inference provider is always attempted first.
- Cloud fallback requires explicit per-request permission.
- Sensitive requests never fall back to a cloud provider.
- If neither permitted provider is available, the engine fails closed with a clear exception.
- Provider identity and local/cloud origin are returned with every response.

## Not yet implemented
- A LiteRT-LM Android runtime adapter exists in `launcher/app/src/main/java/com/mana/launcher/LocalModelRuntime.kt`; model loading is implemented in the Android prototype, but target-device validation is pending.
- Token streaming, cancellation, context window management and prompt templates.
- Encrypted persistent memory and user-approved memory lifecycle.
- Android service/UI integration.
- Official HTTPS adapters for OpenAI Responses, Anthropic Messages, Google Gemini and xAI APIs; the Android app supplies encrypted credential storage.

No model weights are included. This module is the portable orchestration core, not a trained model. The Android prototype supplies the LiteRT-LM adapter and UI-side bridge; the complete MANA OS system-service integration and RMX5566RU device validation remain pending.

## Build
Run with Gradle 8.10+ and JDK 17:
```sh
gradle test
```

The tests use kotlinx.coroutines.runBlocking from the core coroutines dependency; no coroutine-test library is currently required.


## GPT-assisted engineering

`ManaEngineeringCopilot` reuses the configured `InferenceProvider` (including the existing OpenAI Responses adapter) to prepare owner-approved code-change proposals.

- Owner approval and per-task cloud permission are mandatory.
- Only explicitly selected, bounded source files are included; common secret patterns are redacted.
- The model must return a structured unified diff, summary, test plan and risk notes.
- Patch paths are constrained to approved MANA source workspaces; path traversal, device/kernel trees, signing keys, build output and workflow files are rejected.
- The result is always `REVIEW_REQUIRED`. The copilot does not apply patches, execute code, commit changes, install APKs or control the phone.

The Android app exposes this through **MANA Engineering Lab**. The owner selects source files, reviews the exact data-sharing prompt, authorizes one request, and receives a copyable diff proposal. It is not yet connected to an automatic isolated CI patch-application service; that integration remains on the self-development backlog.
