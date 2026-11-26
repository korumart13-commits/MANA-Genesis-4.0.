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

## Service-owned Engine construction — follow-up

- `MainActivity` no longer constructs or starts `ManaEngineAndroidRuntime`.
- The Activity prepares the shared Brain, local inference runtime, and encrypted memory dependency, then requests startup of `ManaEngineService`.
- The service promotes itself to foreground immediately in `onCreate()`, then constructs and starts `ManaEngineAndroidRuntime` from `onStartCommand()`. This ordering avoids doing engine initialization before the foreground-service notification is posted.
- `ManaEngineProcessOwner.initializeFromService()` is now the sole Android call site that constructs and starts the Engine runtime.
- The Activity reads the runtime through the process owner. Activity recreation reuses the service-owned Engine.
- Activity cleanup preserves prepared dependencies during the short interval between service request and service initialization.

## Current verification

- The updated Kotlin and manifest files were fetched back from GitHub after the commits and inspected.
- No successful Gradle/CI build or Android device run has been confirmed. In particular, compilation of the service integration, Android 16 foreground-service behavior, model loading, and recovery after process death remain unverified.

## Binder boundary — follow-up

- `ManaEngineService` now returns a process-local `LocalBinder` from `onBind()`.
- The Binder exposes only readiness and a suspend `think(...)` request method; it no longer returns the raw `ManaEngineAndroidRuntime` object.
- `MainActivity` starts and binds to the service, retains the Binder while connected, and unbinds in `onDestroy()`.
- Chat requests now go through `LocalBinder.think()`. The previous direct `ManaBrainBridge.respond()` fallback from the Activity was removed; if the service is not ready, the UI reports that state rather than silently bypassing the Engine.
- The Activity no longer retrieves or stores the Engine instance from `ManaEngineProcessOwner`; it checks whether the runtime is installed only to reuse its supporting Brain/local-inference/memory dependencies during Activity recreation.
- The Binder is local to this app process and is not an AIDL cross-process API. This is an intentional first boundary; a stable cross-process contract would require AIDL or Messenger and separate serialization/versioning.

### Verification status
- Changed service and Activity sources were written to GitHub and fetched back for inspection.
- Static source inspection confirms the Activity chat path uses Binder and contains no direct Brain fallback.
- No Gradle compilation, Android instrumentation test, emulator run, or physical RMX5566RU test has been confirmed.
- The foreground service remains `START_NOT_STICKY`; process-death restoration and continuous 24/7 execution are not claimed.

## Activity recreation race hardening

- Dependency reuse now checks the prepared runtime state, not only whether Engine initialization has completed. This prevents a recreated Activity from allocating replacement Brain/local-inference dependencies during the interval between service startup and Engine initialization.
- Service startup is requested only after the process owner reports that all required dependencies are prepared.
- Follow-up source read-back confirmed there are no remaining Activity calls to `ManaEngineProcessOwner.engineRuntime()`, no direct `engine.think()` calls, and no direct `manaBrain.respond()` fallback in the chat submission path. Requests use the Binder API.
