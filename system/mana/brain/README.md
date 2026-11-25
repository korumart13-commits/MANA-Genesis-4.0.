# MANA Brain Binder integration

Status: **AIDL CONTRACT + JVM FACADE IMPLEMENTED; ANDROID BINDER SERVICE NOT YET IMPLEMENTED**.

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

- Android `Service` class and manifest/permission wiring.
- Mapping generated AIDL parcelables to `BrainCall` and `BrainResult`.
- Lifecycle owner for the LiteRT-LM engine and cancellation/back-pressure.
- Assistant app binding/client and UI states.
- Product signing/SELinux service domain and platform permission tests.
- End-to-end Android instrumentation tests.

The AIDL contract is not itself a running Binder service. Do not advertise the Assistant as connected until the adapter and client are built, permission-checked and tested in an Android 16 product image.
