# MANA Genesis 4.0 — Local model setup

## Runtime included in the launcher

The Android launcher now includes Google's LiteRT-LM Android runtime and a local model import flow. The runtime library is packaged in the APK; model weights are deliberately stored separately in app-private storage because a model is hundreds of megabytes and should not inflate the installer.

## Suggested starter model for RMX5566RU

- Model: Gemma 3 1B IT, INT4 LiteRT-LM
- File: `gemma3-1b-it-int4.litertlm`
- Approximate download size: 584 MB
- Backend configured initially: CPU (safe compatibility baseline)
- Model page: https://huggingface.co/litert-community/Gemma3-1B-IT

Before downloading, sign in to Hugging Face and accept the Gemma license terms on the model page. Then download the `.litertlm` file to the phone. In MANA, open the floating avatar, choose **Выбрать локальную модель**, and select that file. The app copies it to its private model folder and initializes LiteRT-LM on a dedicated background thread.

The model is not automatically embedded in the APK and the app does not bypass the model's license gate. No model is considered installed until initialization succeeds on the phone.

## Notes

The selected starter is a conservative 1B INT4 model, not a claim that it is the most capable model available. Its published LiteRT-LM artifact is about 584 MB; actual memory use also includes runtime, KV cache, and Android. The device's SM6225 chipset is Qualcomm, so Google Tensor-specific NPU artifacts must not be selected. CPU is the initial backend; GPU acceleration can be evaluated separately after device testing.

The current avatar artwork remains a lightweight drawn placeholder. A real animated 3D character needs a separately licensed rigged GLB/GLTF asset and a renderer; it is not falsely represented as included by the LLM runtime.
