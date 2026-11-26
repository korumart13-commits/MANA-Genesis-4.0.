# MANA Brain AOSP bridge packaging plan

Date: 2026-11-27
Status: design decision; not yet integrated into a full AOSP checkout.

## Current source reality

The repository has two separate implementations with the same package identity:

1. Gradle application at `launcher/app`, containing the Kotlin `ManaEngineBridgeService`, Compose UI, engine process owner, local inference and memory dependencies.
2. Soong platform app at `system/mana/apps/ManaLauncher`, currently a small Java launcher stub with HOME intent only.

The Soong stub does not contain the Kotlin bridge service. The Gradle app depends on AndroidX Compose, lifecycle, WorkManager, LiteRT-LM, SceneView, coroutines and Gradle project modules. These dependencies are not currently declared as Soong modules in this repository. Therefore the Gradle debug APK cannot be treated as if it were the Soong source module, and the stub cannot satisfy the platform Binder service's backend binding contract.

The Binder bridge is security-sensitive: the platform service uses the signature permission `com.mana.permission.BIND_BRAIN`. Both sides must be packaged with a compatible signing certificate. A debug-signed APK must not be assumed to satisfy this requirement.

## Chosen integration route

Use a reproducible prebuilt-APK import boundary for the Gradle-built app, then let the AOSP product build sign/package it with the platform certificate:

1. Build `:launcher:app:assembleRelease` (or a dedicated release variant) in the existing Gradle CI, with reproducible dependency locking and no embedded provider secrets.
2. Publish the exact APK as a CI artifact with SHA-256, Gradle task metadata, commit SHA and build provenance.
3. In a full Android 16 AOSP checkout, stage that artifact at a deterministic vendor/MANA prebuilts path.
4. Add an `android_app_import` module for package `com.mana.launcher`, with `presigned: false` and `certificate: "platform"` so Soong signs the imported package as part of the product build. Validate this behavior in the selected AOSP branch before relying on it.
5. Include the imported app in `PRODUCT_PACKAGES`; remove/disable the minimal Soong `ManaLauncher` stub to avoid duplicate package/module ownership.
6. Ensure the imported manifest declares the bridge service, exact action, exported state and signature permission; verify the service permission is defined consistently by the platform side.
7. Keep `ManaBrainService` platform-signed. Add service_contexts, SELinux service type/domain and least-privilege Binder access only after the actual AOSP policy tree is present.
8. Build the product with Soong, inspect package signing certificates and manifest, then run Binder authorization/callback/reconnect/overload tests on an emulator before any device integration.

## Required verification gates

- APK package name is exactly `com.mana.launcher`.
- APK contains `com.mana.launcher.ManaEngineBridgeService`.
- APK is re-signed by the AOSP platform certificate (verify certificate digest against the platform build output).
- Platform service binds using `com.mana.action.BIND_ENGINE_BACKEND` and the explicit component.
- A non-platform-signed caller is rejected; authorized platform caller succeeds.
- Backend-unavailable state is explicit and fail-closed.
- Cloud use and personal-memory export remain separate consent decisions.
- No API keys, personal memory, user data or model weights are embedded in the APK.
- Emulator tests pass before enabling the product fragment in a device target.

## Why this is not yet a completed integration

No full Android 16 AOSP checkout or pinned upstream manifest is currently available in the repository workspace. The exact AOSP branch's `android_app_import` signing behavior, package-manager behavior, service contexts and SELinux policy cannot be validated from the standalone Gradle CI. This document is an implementation plan, not evidence that a platform image has been built.

## Device-port boundary

This userspace packaging work is independent of the RMX5566 hardware port. It does not unblock boot/vendor_boot/dtbo/vbmeta/recovery/super/OTA image creation. The exact realme device tree, kernel source/config/DTS, proprietary vendor components, VINTF reconciliation, partition/AVB metadata and stock restore procedure remain separate release gates.
