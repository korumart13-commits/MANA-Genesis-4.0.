# MANA Brain Binder integration

Status: **AIDL CONTRACT + JVM FACADE + PROTECTED ANDROID BINDER TRANSPORT IMPLEMENTED; INFERENCE ENGINE NOT YET PACKAGED**.

## Implemented in this change

- Versioned-by-module AIDL contract: `IBrainService`, asynchronous `IBrainCallback`, `BrainRequest` and `BrainResponse`.
- Transport-neutral Kotlin `ManaBrainFacade` delegates to the existing canonical `engine/mana-brain` `ManaBrain` implementation.
- Request IDs and provider provenance are preserved.
- Cloud remains opt-in per request; sensitive requests are rejected by the existing engine when local inference is unavailable.
- Unit tests cover local provenance, default cloud denial, sensitive-request denial and request validation.

## Security contract for the Android adapter

The eventual Android service must:
1. Be bound only by explicit component/action, not an exported implicit endpoint.
2. Require a signature-level permission declared by the MANA product.
3. Check Binder caller UID/package/signature before accepting a request.
4. Enforce prompt size/rate limits and bounded concurrency.
5. Dispatch inference off the Binder thread and deliver completion through `IBrainCallback`.
6. Never persist prompts by default; memory writes require a separate owner-approved API.
7. Return structured errors and never silently switch to cloud.
8. Catch caller death/cancellation and release inference resources.

## Not yet implemented

- Packaging the canonical Kotlin/JVM engine and its runtime dependencies for Soong/Android.
- Mapping generated AIDL parcelables to `BrainCall` and `BrainResult` inside the service.
- Lifecycle owner for the LiteRT-LM engine and cancellation/back-pressure.
- Launcher app binding/client and user-facing readiness states.
- Product signing/SELinux service domain and platform permission tests.
- End-to-end Android instrumentation tests.

The AIDL contract now has a registered Android Binder transport protected by a signature-level permission. The transport is not an inference engine: it returns `ENGINE_NOT_PACKAGED` and `isReady() == false` until the canonical engine is integrated. Do not advertise MANA Brain inference as operational until engine packaging, client integration and Android 16 product tests pass.


## Android Binder transport status

The Assistant package now declares a signature-protected `ManaBrainBinderService`
and links the generated `mana.brain-java` AIDL library. The service validates
request shape and returns explicit error `1001` (`ENGINE_NOT_PACKAGED`) rather
than inventing an inference result. `isReady()` remains false and the engine
version reports `not-packaged`.

This is a real protected Binder endpoint, but it is **not yet a functioning
inference service**. The canonical Kotlin/JVM engine depends on coroutines and
Gson and is not currently built as an Android/Soong runtime artifact. The next
integration gate is to package that exact engine (without a duplicate
implementation), inject it into the service, and add Android instrumentation
coverage. Launcher-side binding and user-facing readiness reporting remain
pending.
