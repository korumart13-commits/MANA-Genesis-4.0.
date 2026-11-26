# ADR-001: MANA local inference runtime — llama.cpp primary

Status: accepted architecture direction; implementation and target-device benchmark pending.
Date: 2026-11-26

## Decision

For MANA Genesis 4.0, adopt **llama.cpp as the primary local language-model inference runtime** behind the project-owned MANA Engine runtime interface. Keep the existing LiteRT-LM adapter as an optional secondary backend during migration and for models/formats/features where it has a proven advantage. Do not remove working LiteRT-LM code until the llama.cpp Android adapter passes parity and device tests.

This changes the local model execution backend, not the system orchestrator:
- Shogun MANA remains the central planner and decision coordinator.
- Hydra remains the set of permission-bounded capability modules.
- MANA Engine remains the project-owned integration, scheduling and runtime abstraction.
- llama.cpp executes compatible local GGUF models.
- Cloud adapters remain optional and policy-gated.

## Why llama.cpp

The upstream project now includes an Android Studio binding under `examples/llama.android/lib`, with a Kotlin `InferenceEngine` API, token `Flow`, GGUF metadata reader and JNI/CMake implementation. The upstream Android and NDK build guides document arm64-v8a support and optional Arm KleidiAI acceleration. The source revision inspected for this integration is pinned to upstream commit `e8d7128fea197f1cdc4dcfb1af3bf33564197e8c` (2026-11-26); do not silently track `master`. This gives MANA a portable CPU-first path across Android ARM64 devices and a broad quantized GGUF model ecosystem. The runtime can select compatible kernels based on the actual CPU rather than assuming one specific accelerator.

Official references:
- https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- https://github.com/ggml-org/llama.cpp/blob/master/docs/build.md

## RMX5566RU backend policy

The verified target profile is Qualcomm SM6225 / bengal with Adreno 610 and Android 16, arm64-v8a. The exact application-visible RAM budget remains unverified.

1. CPU / ARM64 is the required baseline.
2. Build with portable Android NDK settings; enable KleidiAI only where upstream build support and runtime CPU detection permit it.
3. Treat Vulkan/OpenCL/Adreno acceleration as experimental until measured on this exact phone. Do not make GPU acceleration a release requirement.
4. Do not assume Qualcomm QNN/Hexagon NPU availability. Current ExecuTorch QNN documentation lists verification on newer Qualcomm platforms such as SM8550 and SM8450, not this target profile.
5. Start with quantized 1B–3B-class models and a conservative context window. The final model is chosen only after measuring peak resident memory, tokens/second, first-token latency, thermal throttling, battery draw and sustained-session stability. Model size alone is not a reliable RAM estimate.
6. Keep larger models as optional downloads only if device measurements show adequate memory and thermals.

## Capability boundary

An inference runtime runs neural-network models; it does not itself implement phone control, calling, contacts, system settings, diary, game automation, browser access or recovery. Those are Hydra capabilities exposed through permission-checked Android/MANA services. Shogun chooses and coordinates tools; the runtime generates model outputs. Never grant the model unrestricted shell/root access.

For multimodal tasks, register separate model/runtime capability profiles (text, vision, audio, embeddings). Do not advertise a modality until the selected model and runtime have passed its tests.

## Migration plan

1. Add the backend-neutral `LocalInferenceRuntime` contract to the Android module. The existing LiteRT-LM implementation now implements this contract, and `ManaBrainBridge` depends on the interface rather than a LiteRT-specific class.
2. Integrate the upstream Android binding/JNI implementation at the pinned revision, retaining its license notices and exposing it through the same interface.
3. Build the native library for Android arm64-v8a with portable CPU settings; enable KleidiAI only where the pinned upstream build supports it and device testing confirms benefit.
4. Add model metadata validation, quantization/context settings, bounded memory, cancellation, crash isolation and explicit owner-controlled model import.
5. Run identical prompt/latency/memory/thermal tests against both backends on the RMX5566RU.
6. Make llama.cpp the default only after successful device tests. Keep LiteRT-LM as a selectable fallback until parity and stability are established.
7. Remove the old backend only in a separate reviewed change after migration is proven.

## Acceptance criteria

- Android arm64-v8a build succeeds reproducibly.
- CPU inference works on the exact RMX5566RU.
- GPU acceleration is optional and automatically disabled if unsupported or unstable.
- Streaming, cancellation, context truncation and model unload/reload work without leaking resources.
- Local-first and sensitive-request no-cloud policies remain unchanged.
- No credentials or model files are embedded in source or firmware.
- Device benchmark results are recorded before choosing the default model.


## Android integration source inspected

- Upstream Android sample: https://github.com/ggml-org/llama.cpp/tree/e8d7128fea197f1cdc4dcfb1af3bf33564197e8c/examples/llama.android
- Android library API: `lib/src/main/java/com/arm/aichat/InferenceEngine.kt`
- Android JNI implementation: `lib/src/main/java/com/arm/aichat/internal/InferenceEngineImpl.kt`
- Native JNI bridge: `lib/src/main/cpp/ai_chat.cpp`
- Native CMake integration: `lib/src/main/cpp/CMakeLists.txt`

The upstream sample currently declares minSdk 33 and NDK 29.0.13113456, with CMake 3.31.6. MANA must reconcile these with its Android product configuration before integration; do not copy only the Kotlin facade and assume native inference exists.
