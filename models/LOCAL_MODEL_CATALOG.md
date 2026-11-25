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
3. Verify the official repository, Apache-2.0 license, expected file size, SHA-256 and free space before activation.
4. The current prototype tries the LiteRT-LM GPU backend first and falls back to CPU if initialization fails; benchmark both paths on the target phone before choosing a default.
5. Benchmark time-to-first-token, decode tokens/second, peak resident memory, temperature, battery drain and crash recovery.
6. Reject a model that causes memory pressure, thermal throttling or foreground-app instability.
7. Keep a known-good previous model so rollback is possible.

The launcher now offers an owner-initiated download of the official Gemma 4 E2B IT LiteRT-LM file from the LiteRT Community Hugging Face repository. The download uses Android Download Manager, disallows metered/roaming connections, checks free space, enforces an expected size range, and verifies the publisher-listed SHA-256 `181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c` before the file is activated. A previously verified file can be reactivated without downloading it again. The model repository lists Apache-2.0 licensing. The app stores the file in app-specific external storage; it does not silently start a download.

The runtime tries the GPU backend first and falls back to CPU if GPU initialization fails. The manifest declares optional `libvndksupport.so` and `libOpenCL.so` libraries. This is only a backend selection attempt: it does not prove that the target's Adreno driver supports this model or that GPU inference is stable. The exact RMX5566RU still needs a real-device benchmark covering time-to-first-token, decode speed, peak RAM, temperature, battery drain and app stability before this model can be called compatible or default.
