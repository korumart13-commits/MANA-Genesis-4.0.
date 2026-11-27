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

The local manifest must be downloaded **before** the first `repo sync`; otherwise the
MANA project would not yet exist in the checkout. After `repo init`, run:

```sh
mkdir -p .repo/local_manifests
curl -fL "https://raw.githubusercontent.com/korumart13-commits/MANA-Genesis-4.0./main/platform/local_manifests/mana.xml" -o .repo/local_manifests/mana.xml
repo sync -c -j"$(nproc)" --no-tags
```

The downloaded manifest pins the MANA source project to an immutable commit. Do not replace its
`revision` with a moving branch for reproducible builds.

The product configuration fragment is at
`vendor/mana/genesis/system/mana/mana_product.mk`. It must be included by a verified product
makefile only after the actual AOSP checkout and product target exist. This manifest attachment
does not create a device target, supply proprietary blobs, or make the product flashable.


## MANA APK import and signing

After the Gradle release APK is built, stage it into the AOSP checkout:

```sh
vendor/mana/genesis/tools/stage_mana_aosp_prebuilt.sh "$ANDROID_BUILD_TOP" /path/to/app-release-unsigned.apk
```

The staging script first rejects already-signed input APKs, validates package identity and the engine Binder declarations, then
replaces the Java-only launcher stub with an `android_app_import` module using
`presigned: false` and `certificate: "platform"`. The APK is installed as a product
application, not a privileged application. The platform Brain service is separately built
with the same `certificate: "platform"` setting and is also non-privileged. The signature
permission therefore depends on both packages being signed by the exact same platform key.

After the product build, verify the final launcher APK against the platform certificate
actually used by that product:

```sh
vendor/mana/genesis/tools/verify_mana_platform_signature.sh "$ANDROID_BUILD_TOP" /path/to/ManaLauncher.apk
```

The verifier defaults to the AOSP development certificate at
`build/target/product/security/platform.x509.pem`; pass a third argument for a different
product certificate. A production private key must remain in a protected signing system and
must never be added to this repository. A successful source CI run is not a substitute for
this post-build certificate check or a Soong build.


The verified product fragment installs one central MANA app (`ManaLauncher`) and the separate
platform Binder host (`ManaBrainService`). The legacy Java-only `ManaAssistant` stub is not
included in the product package list, avoiding a second, duplicate MANA assistant entry point.
Settings and Control Center remain separate OS configuration surfaces, not separate AI agents.
