# MANA local model catalog

This catalog lists candidate models for the owner's RMX5566RU (reported 8 GB physical RAM). A listed model is not automatically installed or declared compatible. The phone must be benchmarked with the exact model, backend, context length and thermal conditions before selecting a default.

## Candidate models

| Model | Download size | License | Role | Device status |
|---|---:|---|---|---|
| Gemma 4 E2B IT LiteRT-LM | about 2.58 GB | Apache-2.0 | Primary candidate for first device benchmark | Not tested on RMX5566RU |
| Gemma 4 E4B IT LiteRT-LM | about 3.65 GB | Apache-2.0 | Higher-capacity optional candidate | Not recommended as initial default; not tested on RMX5566RU |
| Gemma 3 1B IT LiteRT-LM | about 1.0 GB | Model-card terms | Small fallback candidate | Not tested on RMX5566RU |

## Sources

- Gemma 4 E2B LiteRT-LM model repository: https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm
- Official Google Gemma 4 LiteRT-LM guide: https://developers.google.com/edge/litert-lm/models/gemma-4
- LiteRT-LM Android API guide: https://developers.google.com/edge/litert-lm/android

Google's published Gemma 4 measurements are from other hardware (for example, S26 Ultra and iPhone 17 Pro). They are not performance guarantees for the RMX5566RU. The E2B reference reports a 2.58 GB model and 1,733 MB peak CPU memory on S26 Ultra; E4B reports 3.65 GB and 3,283 MB peak CPU memory on that device. The target phone's Snapdragon 6-series SoC, thermal envelope, available RAM and GPU driver differ, so the E2B candidate must be measured locally.

## Selection policy

1. Keep the existing local model unless the owner chooses to replace it.
2. Never silently download several gigabytes or consume the owner's data plan.
3. Verify source, license, file size, SHA-256 and free space before install.
4. Test CPU first; test GPU only after checking the device's OpenCL/Vulkan driver support and stability.
5. Benchmark time-to-first-token, decode tokens/second, peak resident memory, temperature, battery drain and crash recovery.
6. Reject a model that causes memory pressure, thermal throttling or foreground-app instability.
7. Keep a known-good previous model so rollback is possible.

The app currently imports a user-selected `.litertlm` file and now tries LiteRT-LM GPU initialization first, falling back to CPU if GPU initialization fails. The manifest declares optional `libvndksupport.so` and `libOpenCL.so` libraries. This is only a backend selection attempt: it does not prove that the target's Adreno driver supports the model or that GPU inference is stable. Automatic catalog download, cryptographic publisher verification and device benchmark gating are still future implementation tasks.
