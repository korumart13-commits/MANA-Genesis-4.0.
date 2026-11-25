# MANA Genesis 4.0 — AOSP 16 build foundation

Status: **BUILD PLAN / SOURCE CHECKOUT NOT PRESENT**. This document defines the reproducible platform base; it does not claim that AOSP has been downloaded or compiled.

## Platform baseline

- Platform: Android Open Source Project 16.
- Initial reproducibility pin: AOSP manifest tag `android-16.0.0_r4`.
- Manifest: `https://android.googlesource.com/platform/manifest`
- Device bring-up target: realme RMX5566RU / RE645F, Qualcomm SM6225/bengal.
- Host build environment: supported 64-bit Linux workstation/CI runner. An Android phone running Termux is not a supported full AOSP build host.
- Keep source checkout, build outputs, proprietary blobs and signing material in separate storage/permission domains.

The AOSP tag is a platform baseline only. It does not imply that the target's Android 16 vendor stack is compatible with that exact framework tag. Vendor VINTF and firmware compatibility must be checked before device integration.

## Reproducible checkout (host/CI only)

```bash
mkdir -p ~/aosp/mana-android16
cd ~/aosp/mana-android16

# Install Google's repo launcher and required host packages
# using the official AOSP build-environment instructions first.

repo init -u https://android.googlesource.com/platform/manifest \
  -b android-16.0.0_r4
repo sync -c -j"$(nproc)" --no-tags
```

Do not run this on the owner's phone as a requirement: the full AOSP tree and build outputs exceed a practical phone-only workflow. Use a Linux CI runner or workstation with the RAM, storage and toolchain required by the official AOSP build documentation.

## Build sequence

1. Verify the manifest tag and record `repo manifest -r` output.
2. Build a known AOSP reference target supported by that source tag to validate host toolchain and build environment.
3. Add MANA Soong modules behind a product-independent source-validation target.
4. Add a MANA product configuration only after the RE645F device tree, vendor interface and partition metadata are proven.
5. Integrate system services and apps with explicit Android permissions, Binder interfaces, SELinux domains and tests.
6. Build system/system_ext/product artifacts for CI; do not produce flashable device images until the device-specific gates pass.

## Existing MANA integration boundary

The current repository has separate source generations:
- JVM MANA OS core and Brain provider routing.
- Gradle Android launcher prototype.
- Soong source shells for MANA Launcher, Assistant, Settings and Control Center.

These are not yet one integrated AOSP product. The next software integration task is to define stable Binder/AIDL contracts and migrate reusable logic into Android-compatible modules without copying secrets or assuming privileged permissions.

## CI stages

- `aosp-manifest-verify`: sync and archive resolved manifest.
- `mana-jvm-tests`: compile/test JVM core and Brain.
- `mana-soong-source-check`: validate Soong module metadata against the pinned AOSP tree.
- `mana-android-module-tests`: compile and unit-test Android modules.
- `mana-product-build`: disabled until a verified product target exists.
- `re645f-images`: disabled until device tree, vendor, kernel, AVB and recovery gates pass.

## Release safety

No private AVB keys, API keys or user credentials in the repository or CI logs. No automatic flash, sideload or OTA deployment. Preserve the stock boot chain and green AVB state. A successful AOSP reference build is not a successful RE645F ROM build.

## Current state

- [DOCUMENTED] Android 16 manifest baseline and staged CI plan.
- [MISSING] AOSP source checkout and resolved manifest snapshot.
- [MISSING] RE645F product configuration and validated vendor/kernel sources.
- [DISABLED] Device image, OTA and flashing workflows.
