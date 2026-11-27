# MANA Genesis first-boot completion backlog

This backlog is part of the existing MANA Genesis 4.0 source foundation. It is not a replacement project.

## Purpose

The initial install should boot with the features that can be built and validated now. On first run, MANA should inventory the actual device and installed modules, load this backlog, compare required capabilities with observed capabilities, and continue the unfinished engineering work in dependency order.

## First-boot behavior

1. Collect a read-only device and system snapshot (model, build fingerprint, API, ABI, hardware facts exposed by Android, installed MANA modules, granted permissions, available storage and service health).
2. Persist the snapshot and backlog in encrypted app-private storage.
3. Reconcile each work item against real evidence. Never mark a task complete because an API was merely requested or a source file exists.
4. Prioritize tasks and respect dependencies. Do not start blocked hardware/ROM work before exact device sources and restore evidence exist.
5. For software tasks, ask MANA Brain to research and propose a patch; use GPT/OpenAI only with owner consent and the configured API key.
6. Build and test patches in isolated CI. Keep logs, provenance, source revisions, checksums and test reports.
7. Stage signed app/user-space updates. Protected platform updates require explicit owner approval, exact device compatibility and verified rollback.
8. Resume unfinished work after reboot. Never reboot solely because a generated patch exists.

## Initial backlog

- GPT engineering link and review loop — integration pending.
- Self-editing patch/build/test/signing pipeline — integration pending.
- Defensive security research lab — integration pending.
- Android permission onboarding — integration pending.
- Android 16 AOSP, Binder, Soong and SELinux integration — integration pending.
- Exact RMX5566 kernel/device/vendor/restore sources — blocked pending verified OEM material.
- Device ROM build and physical validation — gated on all hardware evidence.
- First-boot self-completion orchestrator — integration pending.

## Capability truth

The Kotlin backlog planner orders tasks, validates dependencies and preserves blocked work. The Android persistence adapter, first-boot UI/service, research connector, CI patch executor and update installer must be connected before the phone can autonomously execute this plan. A backlog entry is not a completed capability.


## Android first-boot checkpoint implementation

The launcher now contains `ManaGenesisFirstBootService`:

- Runs after the MANA Engine startup path and builds a conservative capability snapshot from Android build facts and actual process-owned MANA components.
- Does not infer availability of OEM kernel/device/vendor sources from a device model string.
- Encrypts the checkpoint with a dedicated AES-256-GCM key held by Android Keystore; each write uses a fresh nonce and a bounded payload.
- Publishes the checkpoint via an atomic file replacement where supported; a failed write preserves the previous checkpoint.
- Stores ordered and blocked backlog entries, dependency IDs, required evidence and owner-approval flags.
- Exposes counts in the read-only MANA System Status screen.
- Failure to write the advisory checkpoint is logged and does not prevent the Brain from starting.

This is a persistent inventory/checkpoint, not yet an autonomous code executor or update installer. The generated plan remains conservative: exact RMX5566 sources and ROM build stay blocked until verified evidence exists.
