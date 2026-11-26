# MANA Genesis 4.0 — AutoDev and kernel development status

Audit date: 2026-11-26
Target: RMX5566RU / RE645F / Android 16 API 36

## Engine self-development components added

- `ManaSelfDevelopmentCoordinator` asks the single MANA Brain for a minimal unified diff. Local inference is the default; cloud code processing is disabled unless the host explicitly opts in.
- `ManaSelfDevelopmentPolicy` separates ENGINE, ANDROID_MODULES and KERNEL_PATCHSET tracks, rejects traversal, binary changes and deletions, and restricts changed paths.
- `tools/mana_autodev.py` accepts a staged patch, requires a clean checkout, creates a detached temporary Git worktree, applies the diff, runs fixed validation, checks the diff and creates a local candidate branch only after passing.
- Candidate branches and patch artifacts are retained separately from the active branch. The runner does not push, merge, sign a release, install an APK, create an OTA or flash the phone.
- The Termux bridge exposes explicit, owner-confirmed actions for engine, app/module and kernel patch candidates.

## Important handoff limitation

The Kotlin coordinator and the host-side candidate runner exist, but a concrete `ManaCandidateWorkspace` Android-to-Termux adapter is not yet connected. The current Termux screen expects a staged patch file in `~/.mana/autodev/`. The local LiteRT-LM companion does not yet transfer its generated diff into that directory automatically. Therefore the complete autonomous Brain → source checkout → candidate runner loop is not yet device-operational.

The Git worktree is an isolated source workspace, not an OS-level security sandbox. Running Gradle tests executes candidate code under the Termux app user's permissions. The Termux bridge therefore displays the exact command and requires owner confirmation. Do not treat this as protection against arbitrary malicious source code.

## Kernel and driver development

- `mana.kernellab` and `kernel/mana-patches/` define a separate kernel track.
- `tools/validate_mana_kernel_patch.py` accepts only text patchsets that add new files below `drivers/misc/mana/`; existing kernel files and binary patches are rejected.
- The current exact RMX5566RU kernel source/config/device-tree/vendor/module ABI remains unverified. The kernel lane is review-only; no kernel compile, boot image, DTBO, vendor_boot, vbmeta, super image or flashable OTA is generated.
- The stock kernel and hardware drivers remain the preservation baseline. A new MANA driver must be proven against the exact source and ABI before it can be applied or built.

## Complete feature inventory

`os/MANA_GENESIS_4.0_COMPLETE_FEATURE_MANIFEST.json` is the release source-of-truth inventory. It contains 41 modules and verifies one-to-one coverage against the 41 entries in `ManaSystemModuleCatalog`.

Coverage includes the Android 16/kernel/hardware/security base; MANA System Core, Brain, local runtime, model manager, cloud gateway, encrypted memory, adaptive intelligence, AutoDev, Kernel Lab, updater and recovery; launcher/System UI/settings/control center; avatar/3D/voice; Diary, Life Manager, Life Studio and Finance; Contact Intelligence, Smart Receptionist and telephony; Game Intelligence, Minecraft bridge and Micro-universe; Yandex, VK, integration hub, Developer Center, local node, device ecosystem and stock-app preservation.

The manifest records target behavior and configuration (including the playful receptionist, 10-second delay, summaries, unknown-number handling; stable avatar identity with changeable outfits; local-first/offline AI; and MANA +1 independent update channel). A status of `architecture`, `partial` or `blocked` is intentional and means the module must not be presented as implemented or packaged yet.

## CI evidence and release status

The unified workflow now runs Kotlin Brain tests, system catalog tests, Python AutoDev policy tests, kernel patch validation, signed MANA update-channel tests, Android prototype builds and manifest completeness checks. Individual jobs are reported by GitHub Actions; a passing source gate is not a full AOSP build or device test.

Current status: working source-level foundation for candidate generation/policy and a local Git-worktree runner; Android handoff remains incomplete; kernel source matching remains blocked; complete MANA +1 installer/OTA is not yet ready.