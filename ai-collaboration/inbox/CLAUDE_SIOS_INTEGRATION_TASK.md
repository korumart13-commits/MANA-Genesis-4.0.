# Claude task: independently audit and integrate SIOS upstream into MANA

Branch: `claude/sios-integration-audit` (create from current main after reviewing the ChatGPT proposal branch)
MANA repository: https://github.com/korumart13-commits/MANA-Genesis-4.0.
SIOS upstream: https://github.com/superintelligence-os/superintelligence-os
Proposal document: `docs/INTEGRATION_SIOS_UPSTREAM_ASSESSMENT_RU.md`

## Objective

Independently determine which SIOS components, if any, can be safely and legally reused in MANA Genesis 4.0. MANA remains the product; SIOS is only a candidate upstream. Do not blindly copy the repository or replace MANA requirements.

## Required audit

1. Inspect upstream at a pinned commit SHA. Read README, LICENSE, NOTICE, THIRD_PARTY, SECURITY, CONTRIBUTING, CLA and relevant source files.
2. Verify actual implementation versus README claims: agent lifecycle, MCP tool server, permissions/confirmation, provider adapters, local model, background execution, OTA signing/rollback, ROM build and installer.
3. Identify the exact build host/toolchain and CI feasibility. Distinguish SIOS ROM build from its desktop installer and APK/service.
4. Compare upstream modules to MANA's existing modules and list duplicate, conflicting, missing and reusable components.
5. Audit AGPL-3.0-or-later obligations and CLA implications. Do not copy code until the legal/dependency review is complete.
6. Compare SIOS assumptions with RMX5566RU / RE645F / SM6225 / Android 16 target records. SIOS has not been validated on this handset; do not infer compatibility from generic Treble support.
7. Do not download the multi-gigabyte MANA Actions artifact or copy proprietary blobs.
8. Do not run any flash/unlock/erase operation. No device readiness promotion.

## Deliverables

Create a dated report under `ai-collaboration/inbox/` containing:
- pinned upstream commit SHA;
- exact source paths and symbols inspected;
- license/CLA/dependency report;
- implementation-vs-documentation matrix;
- MANA feature mapping with all existing requirements preserved;
- RMX5566RU compatibility blockers;
- CI/build/test results and reproducible commands;
- prioritized minimal port plan, or evidence-based no-port decision;
- security/privacy/permission analysis;
- rollback plan.

If a safe component is clearly reusable, implement only a small isolated adapter on a new branch and open a PR. Include tests. Never push directly to main. If no component is safe to port yet, submit the audit report and proposed next steps only.

## Safety and truthfulness

Never claim the ROM is ready, installed or device-compatible without evidence. Keep firmware gates fail-closed. All sensitive phone actions, data deletion, messages/calls, payments, bootloader operations and partition writes require explicit owner approval.
