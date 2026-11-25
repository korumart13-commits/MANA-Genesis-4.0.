# MANA Brain Engine — Stage 1

This is the first independently testable Kotlin/JVM core for MANA Genesis 4.0.

## Implemented
- Local inference provider is always attempted first.
- Cloud fallback requires explicit per-request permission.
- Sensitive requests never fall back to a cloud provider.
- If neither permitted provider is available, the engine fails closed with a clear exception.
- Provider identity and local/cloud origin are returned with every response.

## Not yet implemented
- LiteRT-LM runtime adapter and model loading.
- Token streaming, cancellation, context window management and prompt templates.
- Encrypted persistent memory and user-approved memory lifecycle.
- Android service/UI integration.
- Cloud vendor adapters and credential storage.

No model weights are included. This module is an orchestration core, not a trained model and not yet an executable on-device assistant.

## Build
Run with Gradle 8.10+ and JDK 17:
```sh
gradle test
```

The tests use kotlinx.coroutines.runBlocking from the core coroutines dependency; no coroutine-test library is currently required.
