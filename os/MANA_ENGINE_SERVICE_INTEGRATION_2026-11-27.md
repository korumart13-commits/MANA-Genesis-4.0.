# MANA Engine foreground-service integration — 2026-11-27

## Implemented

- Added `ManaEngineService`, an Android foreground service using the declared `specialUse` foreground-service type and a persistent, low-importance notification.
- Added the matching manifest service declaration and special-use subtype description. The app already declares `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE`.
- Added `ManaEngineProcessOwner` as the single process-level owner for the existing `ManaEngineAndroidRuntime`, `ManaBrainBridge`, local LiteRT-LM runtime, and shared encrypted memory service.
- `MainActivity` installs the runtime into the owner and starts the foreground service after successful engine initialization.
- Activity recreation reuses the already-owned engine, Brain, local inference runtime, and memory service instead of constructing a second copy.
- Activity destruction no longer shuts down those shared components. The foreground service releases them in `onDestroy()`.
- The service uses `START_NOT_STICKY`: after process death it does not pretend that the model/runtime was restored. The user must reopen MANA to initialize it again.

## Boundaries and caveats

- This is an application-level foreground service, **not** a privileged Android system daemon, SystemUI integration, ROM service, or guarantee of uninterrupted 24/7 execution.
- Android, realme power management, user force-stop, battery policy, and foreground-service policy can still stop the process.
- The service uses Android's `specialUse` category. Distribution through Google Play requires review of the declared use case and applicable foreground-service policy; the declaration alone does not grant unrestricted background execution.
- The service is started from the visible launcher Activity after runtime initialization. It does not silently start at boot.
- No target SDK migration was made: the app remains `compileSdk 36 / targetSdk 35` pending a separate Android 16 behavior and permission audit.
- This change does not establish RMX5566RU ROM bootability or add missing device-specific kernel, vendor, VINTF, AVB, recovery, or partition artifacts.

## Verification status

- Source files and manifest were read back from GitHub after commits.
- No Gradle build, Android lint, emulator test, or physical-device test has yet been confirmed for this service integration.
- The repository workflow is configured to run `:brain:test`, `:core:test`, `:app:assembleDebug`, and `:app:testDebugUnitTest` on relevant pushes. A successful run must be checked before calling this integration build-verified.

## Follow-up hardening

- The process owner exposes the same Engine, Brain, local inference, and encrypted memory instances to a recreated Activity; this avoids creating a duplicate runtime when Android recreates the launcher Activity while the started service remains alive.
- A previously loaded local model is not loaded a second time on Activity recreation.
- Local inference shutdown is now idempotent, and Activity cleanup releases Brain/inference only when ownership was not transferred to the service.
- These follow-up changes are source-level changes only; no build or runtime test has been confirmed.
