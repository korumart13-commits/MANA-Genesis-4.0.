# Android 16 AOSP base for MANA Genesis 4.0

The platform compatibility baseline is pinned to the official AOSP manifest tag
`android-16.0.0_r4` (Android 16 QPR2), manifest commit
`15128c9e27cfa599c48d294babd39286ee8f1426`.

The pin is recorded in [manifest.lock](manifest.lock) and checked by
`tools/verify_aosp_manifest_pin.sh` in CI. The verifier checks the peeled
annotated-tag commit from the official Android Git host; it does not download
or build the full AOSP tree.

## Initialize a clean AOSP workspace

Use a Linux x86_64 host with the disk, RAM and build prerequisites documented by
the Android Open Source Project. Do not attempt a full AOSP build on the target
phone or in the small Android-app CI runner.

```bash
mkdir -p ~/android/mana-aosp16
cd ~/android/mana-aosp16
repo init -u https://android.googlesource.com/platform/manifest -b android-16.0.0_r4
repo sync -c -j"$(nproc)" --fail-fast
```

After syncing, place or link the MANA product source into the AOSP tree only
after its Soong modules and package names have been validated against that
checkout. The current repository does not yet contain a device-specific
`lunch` target for RMX5566RU; do not invent one.

## Build gate

AOSP manifest identity verification is automated in GitHub Actions. Full AOSP
sync, Soong product build, image generation, first boot and exact-device
acceptance remain separate gates. A successful manifest check is not a ROM
build and must never be reported as one.
