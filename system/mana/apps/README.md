# MANA Genesis 4.0 — Android application sources

This directory contains the first Android-framework-facing source modules for the MANA product image.

## Packages

- **ManaLauncher** (com.mana.launcher): HOME activity, MANA branding, launches the MANA assistant and lists installed launchable apps.
- **ManaAssistant** (com.mana.assistant): MANA-branded assistant and signature-protected Binder host. It delegates requests to the canonical Kotlin Brain core and fails closed when no Soong inference provider is registered.
- **ManaSettings** (com.mana.settings): MANA-branded settings hub that opens Android settings panels for network, Bluetooth, display, sound, battery, app permissions and MANA's own permissions.
- **ManaControlCenter** (com.mana.controlcenter): MANA-branded quick-access hub for Android Wi-Fi, Bluetooth, mobile network, display, sound, battery, location and notification settings.

## Build integration

All four are Soong android_app product modules. Include system/mana/mana_product.mk from the eventual verified Android 16 product makefile to add them to PRODUCT_PACKAGES.

They remain non-privileged product apps: no platform certificate, root access, hidden API, privileged permission or direct radio toggle is assumed. Settings and Control Center open Android's own protected settings panels; they do not pretend to be the privileged Quick Settings/SystemUI implementation.

## Remaining integration gates

- Exact AOSP checkout and verified RMX5566RU/RE645F product and device tree.
- Packaging a LiteRT-LM InferenceProvider and verified Gson/kotlinx-coroutines Soong dependencies so inference can run entirely inside the product image.
- SystemUI/Quick Settings replacement with correct platform signing and SELinux policy; the current Control Center is a safe settings launcher, not a replacement for Android SystemUI.
- Real Android product build and on-device validation.
- OEM kernel source, matching DTS/DTBO, vendor modules, VINTF/HAL and verified recovery/rollback path before any flashable image is attempted.

These are integrated source modules intended for the future MANA product image. No separate APK release or firmware flash is initiated by this workflow.

## MANA Brain Binder boundary (new)

The product declares `mana.brain-java`; ManaAssistant statically links the generated AIDL and `mana-brain-core` libraries. The core Soong module compiles the existing Kotlin ManaBrain and ManaBrainFacade sources.

The Binder endpoint, signature-level permission, caller UID/package/signature validation, bounded inference executor, Launcher client and Android instrumentation test sources have now been added. The Soong runtime still has no packaged LiteRT-LM provider; when it reports inference unavailable, Launcher forwards the original prompt to the existing com.mana.genesis companion app, which contains the Gradle LiteRT-LM runtime. The Binder service is therefore a real transport over the canonical routing core, but Soong-local inference is not yet operational.


### Brain Binder integration boundary

`ManaAssistant` now hosts a signature-protected Binder service and links
`mana.brain-java`. The service is intentionally fail-closed: until the canonical
Kotlin/JVM engine is packaged for Soong, requests receive an explicit
`ENGINE_NOT_PACKAGED` error and readiness is false. No placeholder AI output
is generated. Launcher binding, engine packaging, and Android instrumentation
tests are still outstanding.
