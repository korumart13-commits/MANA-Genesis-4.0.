# Android 16 platform source

MANA Genesis 4.0 is an AOSP-derived OS. This workstream must start from the official Android 16 platform manifest and then add only verified device/vendor projects.

## Source checkout baseline

Use the official AOSP manifest repository and pin the Android 16 release branch. Record the resolved manifest commit in the build manifest before attempting a product build. Do not silently track a moving branch for release builds.

Example source acquisition on a Linux build host:

```sh
mkdir -p aosp && cd aosp
repo init -u https://android.googlesource.com/platform/manifest -b android16-release
repo sync -c -j"$(nproc)" --no-tags
```

This repository does not yet contain the AOSP checkout. The source tree is several hundred gigabytes including build outputs; it must be fetched by a build host/CI with sufficient disk and bandwidth, not pretended to exist in this Git repository.

## Device integration gate

Do not add a product lunch target until the exact RMX5566RU/RE645F device tree, matching vendor blobs, kernel source/config and VINTF compatibility data are available. Shared SM6225 support from another brand is useful research, not a drop-in device port.

## Build outputs

The OS build pipeline will eventually build Android platform targets and MANA system packages, not a separate launcher APK. Image packaging and signing remain disabled until the device's partition and AVB metadata are verified.


## MANA repository attachment

A local repo manifest is provided at `platform/local_manifests/mana.xml`. It pins this MANA source
repository to a specific Git commit and places it at `vendor/mana/genesis` in the AOSP checkout.

After initializing the official Android 16 AOSP manifest, copy the local manifest into the checkout:

```sh
mkdir -p .repo/local_manifests
cp vendor/mana/genesis/platform/local_manifests/mana.xml .repo/local_manifests/mana.xml
repo sync -c -j"$(nproc)" --no-tags
```

The product configuration fragment is at
`vendor/mana/genesis/system/mana/mana_product.mk`. It must be included by a verified product
makefile only after the actual AOSP checkout and product target exist. This manifest attachment
does not create a device target, supply proprietary blobs, or make the product flashable.
