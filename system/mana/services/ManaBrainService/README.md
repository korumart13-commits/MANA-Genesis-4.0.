# MANA Brain platform Binder service

This Soong product module provides the first Android-side Binder service endpoint for the
MANA Brain AIDL contract.

## Security and behavior

- Installed as a platform-signed privileged product app.
- Binding is protected by the signature-level permission `com.mana.permission.BIND_BRAIN`.
- Binder calls repeat the permission check; caller identity is not accepted from request data.
- The API rejects sensitive + cloud requests.
- Personal-memory cloud export requires two independent request flags.
- The service fails closed with `ENGINE_UNAVAILABLE` until the real local inference backend
  is connected. It does not generate fake answers or forward prompts to a cloud provider.

## Current limitation

This is a real Android Service/Binder endpoint, but it is not yet a functioning inference host.
It is not registered in an AOSP product makefile, has not been built by Soong against an AOSP
checkout, and has no SELinux service context/domain or backend adapter. Those steps are mandatory
before shipping it in a device image. The existing Gradle foreground service remains the currently
tested application-process engine owner; no claim is made that the platform service can access it.
