# RE645F Android integration and firmware build status

## Current verified state

- The canonical Kotlin MANA Brain routing core and transport-neutral facade are compiled as a Soong java_library source module.
- The Assistant app includes that library and the generated Java AIDL library.
- A signature-protected Binder service delegates to the canonical engine facade.
- The Launcher binds to that service, checks readiness and submits asynchronous prompts.
- Device-side Android instrumentation tests cover Binder binding, engine metadata and request/result correlation.
- The JVM test suite includes Android-runtime adapter tests.
- The Soong-side LiteRT-LM InferenceProvider is not yet present/registered. Until one is installed, isReady() is false and the Binder returns an explicit inference-unavailable error. The Soong Launcher then forwards the original prompt to the existing com.mana.genesis companion APK, which contains LiteRT-LM Android 0.16.1, LocalModelRuntime and ManaBrainBridge. That companion remains a separate APK, not part of the Soong system image.
- Cloud provider adapters remain excluded from the Soong library because their Gson and kotlinx-coroutines dependencies are not yet supplied as verified Soong modules.

## Why a complete firmware image cannot be built yet

This repository still does not contain a buildable RE645F product definition. The device foundation directory is intentionally documentation-only. The following are missing or not validated against the exact RMX5566RU / RE645F firmware:

1. AOSP 16 source checkout and complete manifest/dependency sync.
2. Exact AndroidProducts.mk, product makefile and BoardConfig.mk.
3. Verified partition layout, fstab, dynamic partition group sizing and boot image header configuration.
4. Exact OEM kernel source, matching defconfig, DTS/DTSI and DTBO.
5. Matching vendor/odm proprietary blobs, HAL/VINTF manifests and firmware dependencies.
6. Device-specific init configuration, SELinux policy and compatibility validation.
7. AVB signing configuration and a verified recovery/restore path.

AOSP/Soong modules can be integrated and tested independently, but these missing inputs prevent a valid lunch target and full system/vendor/boot image build. Do not create a guessed product target, substitute a generic kernel, unlock the bootloader, disable AVB or flash the phone.

## Required build sequence once exact inputs exist

1. Sync the exact Android 16 AOSP manifest and record its revision.
2. Add the verified RE645F product/device/vendor/kernel inputs.
3. Run Soong module builds for mana.brain-java, mana-brain-core, ManaAssistant, ManaLauncher and ManaBrainInstrumentationTests.
4. Run JVM tests and device instrumentation tests on an Android 16 target.
5. Run VINTF/SELinux/CTS/VTS-relevant checks for the product.
6. Build the complete product images only after all device gates pass.
7. Keep generated images non-flashable until AVB, recovery and restore validation are complete.

No firmware image is produced by this change.
