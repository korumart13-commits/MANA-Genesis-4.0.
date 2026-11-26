# MANA Genesis 4.0 — CI verification update

Verification date: 2026-11-27 (Europe/Moscow)
Repository: korumart13-commits/MANA-Genesis-4.0.
Branch: main

## Confirmed GitHub Actions results

### Android ARM64 llama.cpp JNI
- Workflow run: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45899364905
- Run ID: 45899364905
- Commit: effb26e6c2db2806cb40d171e04d7b9a46c4817e
- Conclusion: success
- Job: Build llama.cpp JNI (arm64-v8a) — success
- Successful steps include Android SDK verification, NDK/CMake setup, ARM64 configuration, native compilation, native-library verification, and artifact publication.
- Artifact: mana-llama-jni-arm64-v8a
- Artifact ID: 15154858603
- SHA-256: c940bf7046bb9d9f88f3a1c34e108cfbc904120d8c2445b5299b52bb6b71901f
- Artifact expiration: 2026-12-10T21:39:14Z

This confirms that the pinned llama.cpp JNI native library was compiled and passed the workflow's ARM64 verification in GitHub Actions. It does NOT confirm loading a GGUF model, running inference, thermal/performance behavior, or compatibility on RMX5566RU.

### MANA OS Core / Microworld JVM
- Workflow run: https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45897757711
- Run ID: 45897757711
- Commit: 5cc4b7a1e6e1a8bffc7c173a14ef265a7ee4845a
- Conclusion: success
- Job: test — success

This confirms the JVM test workflow for that commit. It is not a full Android product build or device test.

## Current release boundary

The project is still not a complete Android 16 AOSP product and is not a flashable ROM. The existing implementation ledger remains authoritative for module-by-module status. In particular, the exact matching Qualcomm SM6225/bengal RMX5566RU/RE645F kernel source, DTS/DTBO, vendor modules/blobs, VINTF/HAL data, partition metadata, AVB signing constraints, and tested stock recovery/rollback path remain unverified.

Do not flash experimental images or label a source ZIP/native CI artifact as firmware. Continue by unifying and validating the Android product build and by resolving the exact-device OEM inputs. Keep the owner's daily phone untouched until all device and recovery gates pass.

## Artifact download

The native artifact is available from the run's Artifacts section while unexpired:
https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/runs/45899364905
