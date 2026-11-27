# MANA Genesis 4.0 — CI remediation and RMX5566RU firmware gate
Date: 2026-11-27

## CI investigation

Repository: https://github.com/korumart13-commits/MANA-Genesis-4.0.

### Latest launcher workflow before remediation
- Run: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45936715424
- Run number: 1011
- Commit: 2b5f24c9831aec5939394e73a4a50394ccf26208
- Final result: failure.
- Confirmed successful steps: MANA Brain/source validations, Android launcher compilation and unit tests, and `:app:assembleRelease`.
- Failure: the Gemma download step returned HTTP 404 after retries. No artifacts were published by this run.

### Root cause and correction
The workflow referenced a filename that does not exist in the official Hugging Face repository. The official file is:
`gemma-4-E2B-it.litertlm`

Official source:
https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/blob/main/gemma-4-E2B-it.litertlm

The workflow was corrected to download:
https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it.litertlm

The official file page reports size 2.59 GB and SHA-256:
`181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c`

### APK dialer-removal verification
The current source manifest was inspected at blob SHA `d277313cb27557e242dc421381e6a59d0116072d`. It contains no matches for:
- `CALL_PHONE`, `READ_CALL_LOG`
- `ACTION_DIAL`, `ACTION_CALL`
- `ManaDialerActivity`, `ManaInCallService`
- `ManaCallScreeningService`, `ManaCallActionReceiver`
- `ROLE_DIALER`, `ROLE_CALL_SCREENING`

The current `MainActivity.kt` was inspected at blob SHA `c07ddba0bf5dec299327666e33075ada8226e3fc`; a scan found no matches for the old MANA Phone UI, dialer activity, call-screening names, or dialer/call permissions.

The workflow was strengthened so that both the debug APK and unsigned release APK are checked with Android `apkanalyzer` for the expected package ID and MANA Brain bridge, and rejected if forbidden dialer/call declarations appear in their compiled manifests.

### Remediation commit and new run
- Workflow fix commit: `f8afd2310733bdb8b925c15a3e88750e6946a7c9`
- Workflow file: `.github/workflows/mana-launcher.yml`
- New launcher run: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45937236960
- Run number: 1012
- At report creation the new run was queued; no success or artifact is claimed yet.

Important packaging distinction:
- The standalone release artifact is `app-release-unsigned.apk`, intended for AOSP platform signing; it is not directly installable as-is.
- The full offline bundle is configured to contain the debug-signed installable APK, the Gemma model, source archive, Russian README and checksums.
- The bundle is not a ROM or OTA image.

## RMX5566RU target identity

The repository's owner-supplied audit identifies the device as:
- realme C85 4G, model RMX5566 / RMX5566RU
- device RE645F; board/platform bengal
- Qualcomm SM6225 / Snapdragon 685 family; arm64-v8a
- Android 16 / API 36; build RMX5566_16.0.5.1310(EX01)
- runtime kernel 5.15.197-android13-8-o-01074-gdfa4acfa411e
- 8 GB physical RAM; 16 GB expansion is virtual memory
- boot state reported locked and Verified Boot green; AVB 1.2; active slot _a; dynamic partitions and file-based encryption enabled.

Do not confuse this with realme C85 5G / RMX5253 / MediaTek Dimensity 6300. Do not borrow its device tree, kernel or vendor images.

## Missing inputs blocking a real flashable RMX5566RU firmware

| Component | Required evidence | Status |
|---|---|---|
| Device tree | Exact RMX5566RU/RE645F product files, BoardConfig, device.mk, fstab, init and overlays | Not verified |
| Kernel | Exact OEM source/commit for this build, matching defconfig, DTS/DTSI, DTBO and module ABI | Not verified |
| Vendor/ODM | Matching proprietary blobs and firmware with provenance and hashes | Not verified |
| VINTF | Device manifest and framework/device compatibility matrix passing against the intended AOSP build | Inventory collected; compatibility not tested |
| Partitions | Exact boot, init_boot, vendor_boot, dtbo, vbmeta, super sizes/layout and slot metadata | Incomplete; unprivileged audit could not read block metadata |
| AVB/signing | Complete verified chain, rollback indexes and legitimate signing path | Not verified |
| Recovery | Exact-build stock restore package and tested rollback/recovery route | Not verified |
| AOSP platform build | Synced Android 16 source tree and successful Soong product build | Not completed |
| Hardware validation | Boot, display, touch, audio, cameras, radio, Wi-Fi, Bluetooth, sensors, charging, suspend/resume | Not run |

## Safe next steps

1. Let launcher run 1012 execute against the corrected official Gemma URL.
2. Confirm APK manifest checks pass for both APK variants.
3. Confirm model download size and SHA-256 before packaging.
4. Confirm the full offline artifact and separate APK artifacts are published; record artifact IDs and hashes.
5. Keep device-specific boot/recovery images blocked until every required source, partition, AVB and stock-restore input is verified.
6. Continue userspace, app, MANA Brain and platform-service development without unlocking, rooting or flashing the phone.

No firmware image was built or flashed. This report records verified repository and workflow evidence only; it does not claim on-device validation.
