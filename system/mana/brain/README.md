# MANA Brain Android integration

Status: **CORE + SOONG MODULE + PROTECTED BINDER TRANSPORT + LAUNCHER CLIENT + DEVICE TEST SOURCES IMPLEMENTED; DEVICE BUILD NOT YET VERIFIED**.

## Implemented

- The existing Kotlin sources ManaBrain.kt and ManaBrainFacade.kt are compiled directly by the Soong module mana-brain-core. No duplicate routing engine was created.
- The generated Java AIDL interface is linked into ManaAssistant and ManaLauncher.
- ManaBrainBinderService validates request shape, enforces a signature-level permission, checks caller package identity/signature, applies prompt bounds and runs inference on a bounded background executor.
- The service maps AIDL requests into BrainCall and delegates to ManaBrainAndroidRuntime/ManaBrainFacade. Responses preserve request IDs, provider identity and local/cloud provenance.
- ManaLauncher binds to the protected service, submits requests asynchronously and renders real responses or explicit error states.
- If the Soong-side provider registry has no local model provider, Launcher forwards the prompt to the existing com.mana.genesis companion app. That app already contains the LiteRT-LM runtime and MANA Brain bridge; it opens the assistant panel with the original prompt.
- Android instrumentation source tests bind the actual Assistant service, inspect engine metadata and validate callback/request correlation.
- JVM tests cover the Android runtime adapter's fail-closed behavior when no Soong local provider is registered.

## Important runtime distinction

The Soong core module includes the canonical routing engine and facade, but it does not yet package the LiteRT-LM runtime or the existing cloud adapter sources. Those cloud adapters require Gson and kotlinx-coroutines Soong dependencies that have not been verified in this AOSP tree. The Soong provider registry therefore remains not-ready unless a compatible InferenceProvider is registered.

For the currently implemented device experience, the Launcher falls back to the existing MANA Genesis companion app, whose Gradle module already includes LiteRT-LM Android 0.16.1, LocalModelRuntime and ManaBrainBridge. This is a real inference path when that companion APK is installed and a compatible model is loaded; it is not the same as embedding LiteRT-LM into the Soong system image.

## Build and test commands

Inside a complete Android 16 AOSP checkout containing this repository:

- m mana.brain-java mana-brain-core ManaAssistant ManaLauncher
- m ManaBrainInstrumentationTests
- atest ManaBrainInstrumentationTests

The companion app is built separately from the Gradle project:

- gradle -p launcher :app:testDebugUnitTest :app:assembleDebug

## Not yet verified

- No AOSP/Soong module build has run in this environment.
- Android instrumentation tests have been added but have not run on an emulator or device.
- The Soong provider registry has no packaged LiteRT-LM provider.
- The companion app remains a separate APK rather than a Soong-built product package.
- No full RE645F firmware image has been produced.
