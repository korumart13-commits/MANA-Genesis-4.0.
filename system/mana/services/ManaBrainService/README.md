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
