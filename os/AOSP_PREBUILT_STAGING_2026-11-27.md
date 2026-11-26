# Stage the MANA Gradle app into an Android 16 AOSP tree

This is the next concrete integration boundary between the tested Gradle app and the platform-side Binder service. It does not claim that a ROM has been built.

## Inputs

- A full Android 16 AOSP checkout containing `build/make/core/envsetup.mk`.
- This repository checked out at `vendor/mana/genesis`.
- The unsigned release APK produced by `gradle --no-daemon :app:assembleRelease` from the `launcher` Gradle root.
- Android SDK `apkanalyzer` available on PATH (or pass its path through `APKANALYZER`).

## Run

```bash
tools/stage_mana_aosp_prebuilt.sh /path/to/aosp /path/to/app-release-unsigned.apk
```

The script verifies the package ID, the declared `ManaEngineBridgeService`, and the MANA signature permission before changing the AOSP checkout. It copies the APK to the MANA Soong module and replaces the source-stub `Android.bp` in that AOSP checkout with an `android_app_import` module configured with `presigned: false` and `certificate: "platform"`. The previous module file is retained as `Android.bp.mana-source-backup`.

The existing `system/mana/mana_product.mk` already includes the module name `ManaLauncher`; the imported app therefore keeps the same product package name and application ID. The Gradle app is the single package owner, avoiding a second APK with the same application ID.

## Required follow-up gates

1. Confirm the selected Android 16 branch accepts the import properties and builds the module with Soong.
2. Confirm the output package certificate matches the platform certificate from that exact build.
3. Inspect the final merged manifest and privileged-permission allowlist requirements.
4. Integrate service contexts and least-privilege SELinux policy against the actual platform policy tree.
5. Run Binder bind/authorization/callback/reconnect/overload tests on an emulator.
6. Only after those pass, integrate the product fragment into a verified RMX5566 device product.

The script does not fetch AOSP, modify bootloader state, access a connected phone, or create/flash boot, vendor_boot, dtbo, vbmeta, recovery, super or OTA images. The Gradle debug APK is not suitable for this import path; use the unsigned release APK so Soong can sign it.
