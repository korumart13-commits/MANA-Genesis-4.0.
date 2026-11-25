# MANA Genesis 4.0 — unified build and release contract

This file defines what the single unified source archive means and what it does not mean.

## One source package

The unified CI workflow runs the existing independent checks as one release gate:

1. `os/core`: JVM tests for lifecycle, module catalog, encrypted personal services, storage and security policy models.
2. `engine/mana-brain`: JVM tests for local-first routing and consent-gated cloud fallback.
3. `launcher`: Android Gradle debug build and unit tests for the app prototype, LiteRT-LM adapter and related Android integration.
4. `system/mana/apps`: Android API 36 Java compilation plus Soong product-list, manifest and non-privileged boundary checks.
5. Avatar glTF structural validation.
6. Only after all checks pass, CI creates one artifact named `MANA_Genesis_4.0_UNIFIED_SOURCE.zip` with the source modules and a SHA-256 checksum.

This is a unified source snapshot, not a claim that all modules are already one executable Android system. It contains the current generations of source code, documentation and product definitions.

## Known integration gaps that remain release-blocking

- The Gradle launcher prototype and Soong product apps are separate implementations. The Gradle app has the local inference, avatar and personal-feature prototypes; the Soong assistant shell is not yet wired to the Brain service.
- The MANA System Core is JVM-tested but is not integrated as Android system_server/Binder services.
- The current MANA Control Center is a non-privileged settings hub, not a replacement for AOSP SystemUI or Quick Settings.
- The repository does not contain a complete pinned Android 16 AOSP checkout/product build.
- The exact RMX5566RU device tree, matching Linux 5.15.197 OEM kernel source/config/DTS/DTBO, vendor blobs/modules and VINTF/HAL set are not available.
- Partition metadata, AVB signing chain, verified boot integration, tested recovery/stock restore and hardware acceptance results are missing.
- Local model download/integrity checks exist, but actual LiteRT-LM inference and resource use have not been validated on the target phone.
- Live provider credentials, Yandex/VK owner-authorized tests, telephony role/device tests and connected-device tests remain owner/device dependent.

## Release states

| State | Meaning |
|---|---|
| Source checks passed | Current modules compile/test in their respective host/Android source environments. |
| Unified source archive produced | One ZIP contains the current project source and a checksum. |
| AOSP product built | Requires a pinned AOSP tree and successful Soong product image build. Not achieved by this workflow. |
| Device ROM built | Requires exact device tree, kernel, vendor/HAL, partition and AVB inputs. Not achieved. |
| Device validated | Requires safe installation/recovery and hardware acceptance on RMX5566RU. Not achieved. |

The unified workflow must never label the source ZIP as an installable OS, OTA or firmware. No workflow in this repository may flash the owner's phone. The bootloader was reported locked and Verified Boot green; keep the stock system intact until all device gates are satisfied.
