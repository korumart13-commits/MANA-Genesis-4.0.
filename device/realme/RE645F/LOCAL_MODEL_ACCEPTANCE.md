# MANA Genesis 4.0 — on-device local inference acceptance

Status: prepared for owner-device validation; no result is claimed until the checks are run on the exact RMX5566RU handset.

## Why this stage is separate

The Android companion APK builds in GitHub Actions and its JVM/source gates pass. The companion contains the LiteRT-LM runtime and an owner-initiated Gemma 4 E2B IT model download flow. The AOSP Soong Brain service still has no in-process LiteRT-LM provider. Until the full AOSP image and a cross-process provider bridge are built and tested, the companion remains the working local-inference host and the system Binder reports inference unavailable rather than fabricating output.

## Safe validation sequence

1. Install the CI-produced `app-debug.apk` on the owner's Android 16 handset. This is an ordinary application install; do not unlock the bootloader, change slots, or flash partitions.
2. Open MANA Genesis and inspect the model status. If no model is present, use the app's explicit model-download action only after checking Wi-Fi and available storage. The initial Gemma 4 E2B IT file is approximately 2.58 GB; allow additional room for temporary download and runtime cache.
3. Wait for the app's SHA-256 verification and model activation to finish. Do not manually rename or replace a partially downloaded model.
4. Run a short Russian prompt with no personal or sensitive data. Confirm that the response is marked local and that the displayed backend is GPU or CPU.
5. Repeat after a full app restart to verify persistence. Record any initialization error verbatim.
6. Stop if Android reports low storage, repeated app crashes, excessive heating, or severe throttling. Do not force a GPU backend or disable Android thermal protections.

## Evidence to record

- Device model/build fingerprint and Android version (avoid sharing serial number, IMEI, Android ID, or account identifiers).
- Model file name, byte size, and verification status.
- Selected backend (GPU/CPU).
- Whether generation succeeds after cold start.
- Approximate time to first visible output and total response time.
- Whether the app crashes, heats excessively, or is killed under memory pressure.

## Acceptance criteria

- Model hash matches the catalog entry before activation.
- A local prompt returns a non-empty answer with local provider provenance.
- Cold-start reload succeeds at least twice.
- No fabricated success state when model initialization or generation fails.
- No sensitive prompt is sent to cloud providers during this local-only test.

## Remaining engineering gate

A separate Android Binder bridge is still required before the AOSP `ManaBrainBinderService` can invoke the companion's LiteRT-LM engine across process boundaries. That work must use an explicit, authenticated IPC contract and instrumentation tests; it must not rely on `ServiceLoader` to discover a provider in another app process. A successful companion APK build is not evidence that the full system image or this bridge is built.
