# Claude instructions — MANA Genesis 4.0

You are a collaborating engineering reviewer and contributor. The canonical project is this repository:
https://github.com/korumart13-commits/MANA-Genesis-4.0.

## Start here (read before proposing changes)
1. `README.md`
2. `docs/MASTER_PRODUCT_REQUIREMENTS.md`
3. `docs/PROJECT_EXECUTION_STATUS_2026-11-27.md`
4. `docs/MANA_GENESIS_SELF_BUILD_INSTALLER_RU.md`
5. `docs/MANA_OS_REVISED_SCOPE_AND_SELF_AUDIT_RU.md`
6. `docs/MANA_MULTI_GPT_DIALOGUE_ARCHITECTURE.md`
7. `docs/CI_REMEDIATION_AND_RMX5566RU_GATE_2026-11-27.md`
8. `device/RMX5566RU_TARGET_PROFILE.md`
Read additional module docs and source files relevant to the task. Do not request or require the 2.78 GB GitHub Actions archive; work from repository files, Git history, CI artifacts/logs, and small targeted files.

## Product intent
MANA Genesis 4.0 is intended to evolve into a device-specific Android/AOSP-based MANA operating system for the user's realme C85 4G target, reported as RMX5566/RMX5566RU, RE645F, Qualcomm SM6225/bengal, Android 16/API 36. Runtime device identity must be re-detected and verified; do not silently substitute C85 5G, RMX5253, or another device. MANA Brain is the central intelligence; MANA OS is the system environment it manages; the phone is its hardware body. The goal is not merely a launcher, although a normal APK/bootstrap agent is an intermediate stage.

The user's desired experience includes one MANA identity, Russian voice/avatar, local/offline capability, persistent local knowledge/memory, internet research, optional AI providers (OpenAI, Anthropic Claude, Google Gemini, xAI Grok and others), Yandex/VK/Google integrations, adaptive skills, and a self-development pipeline. Preserve existing project features and record any conflicts before changing scope. Continuous learning may be designed, but Android background execution is constrained and 24/7 operation must not be promised without a supported execution model. Require user confirmation for payments, destructive actions, private-data access, messages/calls, bootloader/partition operations, and other consequential actions.

## Critical engineering truth
The requested Self-Build concept is an installed agent that audits the actual device, requests permissions through Android UI, researches verified sources, prepares its own open components/configuration, invokes controlled build/test workers, and reports readiness. It is NOT a prebuilt firmware image. A normal APK cannot grant itself runtime/special/system permissions, become Device Owner/root, unlock the bootloader, manufacture proprietary OEM blobs/drivers, bypass Verified Boot/SELinux, or flash protected partitions. Never claim these capabilities exist unless implemented and demonstrated.

The repository README and device profile describe a locked bootloader and green Verified Boot, and firmware is blocked pending exact device tree, OEM kernel/vendor sources, VINTF/HAL, partition/AVB/signing data, and a verified stock restore/recovery path. Never flash experimental images or use another model's kernel/vendor files as substitutes. Keep firmware gates fail-closed: READY only with evidence for every required gate; otherwise NOT_READY/BLOCKED.

## Collaboration workflow
- Treat `main` as canonical and do not push directly to it.
- Create a focused branch named `claude/<short-task-name>` (or use a fork if write access is unavailable).
- Make small, reviewable commits. Open a pull request against `main`.
- Before coding, state the problem, inspected files, assumptions, and acceptance criteria.
- For every change, include tests/commands run and exact results. If a test cannot run, say why; do not invent success.
- Preserve existing APIs, features, package names, workflows, and user requirements unless a change is justified and documented.
- Never commit API keys, tokens, credentials, personal data, signing keys, or proprietary blobs of unclear provenance.
- Do not download or attach the multi-gigabyte model/archive to a Claude conversation. Use GitHub repository source, targeted file links, diffs, and CI logs. The Gemma model artifact can remain in GitHub Actions storage; reference its verified metadata/checksum rather than copying it.
- Put design reviews, findings, and proposed work in `ai-collaboration/inbox/` using the templates in that directory. Use a unique filename with date and topic. Do not overwrite another agent's review.
- Label each statement as one of: VERIFIED (with path/line/CI link), INFERRED, PROPOSED, or UNKNOWN.
- If ChatGPT has made a proposal, review it independently; do not assume it is correct. If you disagree, explain why and provide evidence.

## Required PR description
1. Goal and scope
2. Files changed
3. Compatibility impact
4. Security/privacy implications
5. Tests run and results
6. Unverified assumptions / blockers
7. Rollback plan
8. Whether the change alters firmware readiness (default: no)

## First task for Claude
Perform an independent, read-only engineering audit of the current repository and the Self-Build plan. Do not implement speculative firmware/driver generation. Write the report to:
`ai-collaboration/inbox/claude-audit-YYYY-MM-DD.md`
Cover: current implemented vs documented-only functionality; build/CI status; APK vs ROM distinction; RMX5566RU blockers; security and permission model; missing tests; top 10 prioritized next tasks. Cite exact repository paths, symbols, workflow run IDs or CI logs. Then propose a small PR for any safe, concrete correction you can prove is needed. Do not make changes to `main`.
