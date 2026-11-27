# MANA Brain Binder integration test

## What the device test covers

The Soong module `mana-brain-binder-integration-tests` is a platform-signed
instrumentation test. It binds to the explicit `com.mana.system.brain/.ManaBrainService`
component and checks:

1. The test process has the signature permission `com.mana.permission.BIND_BRAIN`.
2. The public Brain AIDL endpoint returns protocol version 1.
3. The Brain service reaches a connected backend state rather than remaining in
   `UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED`.
4. A local-only request receives a callback from the backend. Model-not-ready or inference
   errors are acceptable diagnostics; a missing/disconnected backend is not.

This test exercises the installed Android services across real Binder processes. It is not
replaced by the existing host-side request-policy tests or Gradle unit tests.

## Preconditions

- A full Android 16 AOSP checkout with the MANA project synced at
  `vendor/mana/genesis`.
- A verified product configuration that includes `ManaBrainService` and `ManaLauncher`.
- An unsigned Gradle release APK containing `ManaEngineBridgeService`.
- The APK staged through `tools/stage_mana_aosp_prebuilt.sh`, so Soong signs it with the
  same platform certificate as `ManaBrainService`.
- A compatible emulator/device already running an image built from that product. This test
  does not flash an image or change device partitions.

## Run

From a Linux AOSP build host:

```sh
vendor/mana/genesis/tools/run_mana_aosp_binder_integration.sh \
  "$ANDROID_BUILD_TOP" <verified-lunch-target> \
  /path/to/app-release-unsigned.apk
```

The runner invokes Soong for `mana-brain-interface`, `ManaBrainService`, `ManaLauncher`
and `mana-brain-binder-integration-tests`, checks that both services are already installed
on the connected test image, then invokes:

```sh
atest mana-brain-binder-integration-tests
```

## Current execution status

The test module, test case and guarded runner are committed as source. GitHub CI performs
static contract checks and shell syntax validation. No full AOSP checkout is present in the
project environment, so a real Soong compile, platform certificate comparison and device-side
Binder test remain pending. Do not report this test as passed until Tradefed executes it on a
running MANA product image and the test result is green.
