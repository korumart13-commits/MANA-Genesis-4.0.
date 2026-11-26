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
- Cloud vendor adapters and credential storage.

No model weights are included. This module is the portable orchestration core, not a trained model. The Android prototype supplies the LiteRT-LM adapter and UI-side bridge; the complete MANA OS system-service integration and RMX5566RU device validation remain pending.

## Build
Run with Gradle 8.10+ and JDK 17:
```sh
gradle test
```

The tests use kotlinx.coroutines.runBlocking from the core coroutines dependency; no coroutine-test library is currently required.
