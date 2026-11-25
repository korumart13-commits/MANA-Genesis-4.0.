# MANA Genesis 4.0 — progress snapshot (2026-11-25)

This file supplements IMPLEMENTATION_LEDGER.md with the latest source changes. Status is intentionally conservative: a passing JVM test or Android compile does not prove the feature works on the owner's phone.

## Newly added implementation work

| Workstream | Current state | What is actually present | What remains |
|---|---|---|---|
| 3D avatar asset | Prototype asset validated in CI | Original local glTF 2.0 full-body character, 49 nodes, 12 meshes, 12 materials, embedded geometry and one idle/blink animation; validator reports 7,052 embedded geometry bytes | It is not yet a production skinned/bone-rigged model and has no complete blendshape/viseme set; outfit and expression changes need full renderer integration; device GPU/thermal testing pending |
| Floating avatar | Android source integration in progress | SceneView/Filament Compose overlay hosted by a foreground service; user-granted overlay permission; drag, snap-to-edge, tap-to-open assistant and explicit stop notification | Current Android CI compile must pass; target-device lifecycle, transparent composition, battery, memory and touch tests pending; not integrated into AOSP Soong product |
| 3D preview | Source added | Dedicated preview Activity uses SceneView and bundled glTF asset with orbit controls and auto animation | Verify Android CI and on-device rendering |
| Local model manager | Implemented + JVM tested | Staged LiteRT-LM installer checks owner license acceptance, filename, exact byte count, SHA-256, free-space reserve, temporary file, atomic replacement and metadata; tests pass in Actions run 45687689117 | Android import/downloader must call the shared manager; model signature/provenance and device compatibility benchmark still required |
| Gemma 4 E2B | Owner-initiated download prototype | DownloadManager Wi-Fi-only download from the official LiteRT Community Hugging Face repository; 2.59 GB source artifact; SHA-256 checked against upstream value | The model has not yet been benchmarked on RMX5566RU; RAM, thermal, latency, token/s, backend compatibility and actual conversation tests pending |
| Cloud model gateway | Android prototype | OpenAI, Anthropic, Gemini and xAI provider selection; encrypted credential file using Android Keystore AES-GCM; cloud fallback is opt-in per request and local-first | Current cloud-adapter test/Android compile must pass; provider-specific quotas, errors, cancellation, redirect hardening and privacy tests pending |
| Yandex / VK | Connector prototype | Yandex AI Studio connector and read-only VK connector added; Android CI run 45688031072 passed | Live owner-token testing, scopes/consent, rate limits, revocation and UI integration still pending |
| Minecraft | Companion pack source and package | Bedrock behavior-pack manifest, official Script API starter, owner-tagged command path; pack syntax/package workflows passed (45687934475, 45687940684, 45688019233) | Phone-to-world transport, user pairing, supported server/world testing and richer autonomous behaviors remain; no anti-cheat bypass |
| Smart Receptionist | Android service implementation in progress | Consent-based Android CallScreeningService and role-selection flow being added | Must pass Android build; call-screening role must be granted by owner; carrier/device behavior and local-law review required |

## Verified CI references

- MANA Brain cloud-routing tests: run 45687583299 passed.
- MANA Model Manager staged-install tests: run 45687689117 passed.
- Yandex/VK connector source build: run 45688031072 passed.
- Minecraft companion pack and owner-tagged command workflows: runs 45687934475, 45687940684 and 45688019233 passed.
- glTF structure validation: run 45687912951 passed the asset validation step, but the Android compile step failed due to a malformed Gradle dependency line; that superseded Filament dependency path has since been removed in favour of SceneView. A later Android compile is still required.
- Newer SceneView floating-overlay Android compile: run 45688104938 was in progress at the time this snapshot was written.
- Newer Yandex/VK, cloud hardening, Gemma downloader and call-screening changes: their workflows were still in progress.

## Device and ROM gate

The target diagnostics identify RMX5566RU / RE645F / Qualcomm SM6225 / bengal / qcom / kernel 5.15.197. Public realme pages list different chipsets for the retail C85 and C85 5G pages, so the runtime identifiers must be reconciled before any device tree or kernel is selected. The public realme_C85_5G-AndroidV-kernel-source candidate's latest commit explicitly targets RMX5253 and a MediaTek release; it is rejected for the owner's Qualcomm runtime target.

No full AOSP checkout, exact RMX5566RU device tree, matching OEM kernel source, vendor blobs/modules, VINTF manifest, partition metadata, AVB signing chain or tested recovery image is present. Therefore there is still no bootable MANA OS image and no safe firmware-flash candidate.

## Completion rule

A feature is complete only when it is integrated into the final MANA OS product, relevant automated tests pass, and hardware-dependent behavior is verified on the RMX5566RU. Until then it remains a prototype or blocked item even if its source compiles.
