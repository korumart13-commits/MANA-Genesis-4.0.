# MANA Brain Binder integration test

## What the test covers

The canonical Soong instrumentation module is
`system/mana/tests/ManaBrainBinderIntegration`. It is platform-signed and binds to
`com.mana.system.brain/.ManaBrainService`. It checks the signature permission, AIDL protocol
version, backend handshake with `ManaEngineBridgeService`, and delivery of a callback for a
local-only request. A model-not-ready or inference diagnostic is acceptable; a disconnected
backend is a failure.

This is a device-side cross-process Binder test, not a host-side policy test and not a Gradle
unit test.

## Run on a provisioned AOSP build host

Prerequisites:

- Full Android 16 AOSP checkout with the pinned MANA project at
  `vendor/mana/genesis`.
- Verified product configuration including `ManaBrainService` and `ManaLauncher`.
- Unsigned Gradle release APK containing `ManaEngineBridgeService`.
- Booted emulator/device already running an image built from that product for the runtime
  test. The runner does not flash, unlock, wipe or write partitions.

Build Soong modules and run the host policy test:

```sh
vendor/mana/genesis/tools/run_mana_soong_integration.sh \
  "$ANDROID_BUILD_TOP" <verified-lunch-target> \
  /path/to/app-release-unsigned.apk
```

To additionally run the device-side Binder instrumentation test:

```sh
vendor/mana/genesis/tools/run_mana_soong_integration.sh \
  "$ANDROID_BUILD_TOP" <verified-lunch-target> \
  /path/to/app-release-unsigned.apk --run-device-tests
```

The runner invokes Soong's own test checks, builds `mana-brain-interface`,
`ManaBrainService`, staged `ManaLauncher`, `mana-brain-request-policy-tests` and
`ManaBrainBinderIntegrationTests`; it then uses `atest` for the host policy test and,
when explicitly requested, the device Binder test.

## Current execution status

The instrumentation test and runner are committed. GitHub CI checks their source contract and
the shell runner syntax. A full AOSP Android 16 checkout is not present in the repository
workspace, so Soong compilation, platform certificate comparison and Tradefed execution on a
running MANA product image remain pending. Do not report the test as passed until the actual
Soong build and `atest ManaBrainBinderIntegrationTests` both complete successfully.
