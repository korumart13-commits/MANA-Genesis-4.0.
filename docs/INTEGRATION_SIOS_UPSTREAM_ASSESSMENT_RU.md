# SIOS upstream integration assessment for MANA Genesis 4.0

Status: PROPOSED INTEGRATION / NOT MERGED / NOT DEVICE-VALIDATED
Date: 2026-11-27

## Decision

Use SIOS as a selectively evaluated upstream architecture and component source for MANA Genesis 4.0. Do not replace MANA's product identity, requirements, existing modules, or device-specific bring-up work with SIOS. Do not treat the SIOS GSI as a flashable build for RMX5566RU.

Upstream: https://github.com/superintelligence-os/superintelligence-os
Website: https://superintelligence-os.github.io/
License declared upstream: AGPL-3.0-or-later. Upstream states contributions require signing its CLA; review that requirement before contributing upstream or copying/adapting code.

## Verified upstream facts (from upstream README as inspected 2026-11-27)

- SIOS describes itself as Android 16 GSI based on LineageOS 23.2.
- It exposes an AI agent through shared phone tools/MCP, with user confirmation for sensitive actions.
- It documents system-wide entry points, accessibility-based interaction, widgets/mini-apps/routines, a Termux-hosted agent service, local-model and cloud-provider options, and OTA A/B rollback design.
- The upstream project labels itself early (0.1); its end-to-end validation is in the Android emulator, not on a physical phone.
- Its ROM build instructions are macOS/Homebrew-oriented and require prepared LineageOS GSI base images and Android build tools.
- Its documented desktop installer supports a temporary DSU trial and a persistent fastboot path. Persistent installation requires an unlocked bootloader and erases the device.
- The repository is AGPL-3.0-or-later and includes third-party components with their own licenses. Do not assume every bundled component can be redistributed under the same license.

## What this means for MANA

Potentially reusable after code-level review:
1. AI-provider adapter and model-selection concepts.
2. MCP phone-tool contract and confirmation boundary.
3. Agent service / background task architecture.
4. System integration entry points and accessibility interaction model.
5. OTA metadata, signing, A/B rollback concepts.
6. Build and emulator smoke-test workflow patterns.

Must remain MANA-owned:
- MANA Brain identity, memory, skills, Russian voice/avatar, provider routing and offline fallback.
- MANA feature inventory and product requirements.
- RMX5566RU device profile, device audit, firmware readiness gates and recovery plan.
- MANA update/consent model, permissions explanations and privacy/security policy.

Do not copy or vendor upstream source until a file-by-file license and dependency audit is complete. Prefer a pinned upstream commit, a documented fork, or small independently reviewed patches. Preserve copyright and license notices. Confirm whether upstream CLA terms are acceptable before submitting contributions upstream.

## Device compatibility gate: RMX5566RU / RE645F

Current MANA device records describe a Qualcomm SM6225/bengal target, Android 16/API 36, locked bootloader and green Verified Boot. SIOS itself has not demonstrated compatibility with this device. The SIOS README's generic Treble/vendor requirements are not proof that this exact handset can boot the image.

Before any device image can be considered:
- Re-detect exact product/device/board/SoC and active slot on the phone.
- Prove Treble/VINTF compatibility and inspect vendor API level.
- Obtain exact stock firmware, partition map, boot/recovery images and verified restore route.
- Obtain matching kernel/device tree/vendor sources and required proprietary blobs with provenance.
- Verify AVB/vbmeta, signing, dynamic partitions, super layout, A/B or non-A/B topology.
- Build and run emulator tests, then non-destructive DSU testing if the device supports it.
- Keep persistent flashing disabled until all evidence is reviewed and the owner explicitly confirms.

Never substitute files from a related Realme model or an unrelated Qualcomm device. Never report READY on missing evidence.

## Integration architecture proposal

MANA remains the top-level product. Treat SIOS as an optional upstream adapter layer:

MANA Brain
- Provider router: OpenAI, Anthropic, Gemini, xAI/Grok, other OpenAI-compatible endpoints.
- Local inference fallback: MANA's selected local runtime/model.
- Memory, skills, planning and task ledger remain MANA-owned.

Phone capability bridge
- Evaluate SIOS MCP tool schemas and permission gates.
- Map only supported, consented actions into MANA's capability registry.
- Keep sensitive actions behind explicit owner confirmation.
- Keep untrusted screen, notification, SMS and web content marked as data, never instructions.

System/build layer
- Evaluate SIOS ROM scripts as reference only.
- Build MANA images in reproducible CI/build workers; do not assume the user's phone can compile a full Android ROM.
- Produce signed, checksummed artifacts and machine-readable compatibility reports.
- Separate APK/bootstrapper artifacts from GSI/ROM/OTA artifacts.

## Required next tasks

1. Read upstream README, LICENSE, NOTICE, THIRD_PARTY, SECURITY, CONTRIBUTING, CLA and inspect the actual source tree.
2. Pin the exact upstream commit SHA and produce a file-level architecture map.
3. Produce a license/CLA/dependency compatibility report before copying code.
4. Compare SIOS MCP actions and consent gates against MANA's current capability/approval model.
5. Compare build scripts and CI requirements with MANA's existing GitHub Actions workflows.
6. Identify reusable code versus concepts that should be reimplemented in MANA.
7. Run static analysis and emulator tests on any proposed adapted component.
8. Keep device readiness unchanged (BLOCKED/NOT_READY) unless RMX5566RU evidence independently satisfies every existing firmware gate.

## Explicit non-goals

- No automatic bootloader unlock.
- No silent or unattended flashing.
- No bypass of Verified Boot, SELinux, Android permission controls or OEM protections.
- No generated or guessed proprietary drivers/vendor blobs.
- No claim that SIOS is already installed, connected to the phone, or compatible with RMX5566RU.
- No replacement of MANA Genesis 4.0 by an unrelated ROM.

## Acceptance criteria

- A review report with upstream commit SHA, exact paths, license findings and implementation-vs-documentation status.
- A mapping of SIOS features to MANA modules, with conflicts and missing functions recorded.
- A minimal proposed integration PR with tests and rollback plan, or a clear evidence-based recommendation not to port a component.
- No change to firmware readiness from this assessment alone.
