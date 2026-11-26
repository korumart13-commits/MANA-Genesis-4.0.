# MANA Brain platform Binder service

This Soong product module provides the Android-side Binder endpoint for the MANA Brain AIDL contract.

## Security and behavior

- Declared as a platform-signed privileged product app.
- Binding is protected by the signature-level permission `com.mana.permission.BIND_BRAIN`.
- Binder entry points repeat the permission check; caller identity is not accepted from request data.
- The API rejects sensitive + cloud requests.
- Personal-memory cloud export requires explicit cloud and personal-memory consent flags.
- Request validation is extracted into a pure Java policy class with a host-side JUnit test target.
- Accepted requests are dispatched away from the Binder transaction thread.
- The queue is bounded to 32 pending requests; overload and shutdown races return an explicit `SERVICE_BUSY` error instead of accumulating unbounded work.
- The service fails closed with `ENGINE_UNAVAILABLE` until a real local inference backend is connected. It does not generate fake answers or forward prompts to a cloud provider.

## Product integration status

`system/mana/mana_product.mk` declares `ManaBrainService` in `PRODUCT_PACKAGES`. This is a product fragment only: it still must be included by a verified Android 16 product makefile in a real AOSP checkout. The fragment itself does not establish a valid RMX5566RU product.

## Current limitation

The Android Service/Binder endpoint exists in source, but it is not yet a functioning inference host. It has not been built by Soong against an AOSP checkout and has no target-specific SELinux service context/domain or backend adapter. Those steps are mandatory before shipping it in a device image. The tested Gradle foreground service remains a separate application-process engine owner; no cross-process backend adapter has been implemented.


## Request bounds

The platform endpoint limits prompts to 32,768 characters. Explicit cloud-provider names are allowlisted (OpenAI, Anthropic, Google/Gemini, xAI/Grok, Yandex and VK); a provider cannot be selected when the request is local-only. These checks happen before the request enters the bounded worker queue.


## Cross-process backend bridge

ManaBrainService binds to the explicit com.mana.launcher/.ManaEngineBridgeService AIDL endpoint. The app-side bridge delegates to the existing process-owned ManaEngineAndroidRuntime; it does not construct a second model or memory store. Both services use the signature permission com.mana.permission.BIND_BRAIN, and the app-side Binder endpoint repeats permission and request-policy checks. If the endpoint is absent, protocol-incompatible, or not signed with the matching platform key, the platform service fails closed with an unavailable/disconnected state.

This is an implemented source-level integration, not yet a verified AOSP product integration. The current Soong ManaLauncher module is a separate minimal home-app stub and does not package the Gradle Kotlin engine bridge. The Gradle debug APK is not platform-signed. A product packaging/signing solution and Soong/device verification are still required.
