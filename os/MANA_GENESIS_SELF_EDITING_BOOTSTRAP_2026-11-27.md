# MANA Genesis self-editing and staged system completion

Date: 2026-11-27

## Preserve the existing project

This feature extends the existing MANA Genesis 4.0 repository and its Brain, local inference, encrypted memory, Microworld, AIDL/Binder platform service, launcher bridge, diagnostics, recovery and update verifier. It does not replace these components or create a parallel assistant.

## Intended owner experience

1. Install and boot the smallest complete, compatible MANA bootstrap.
2. On first run, MANA performs a read-only inventory of device/build/API/ABI, installed MANA modules, permissions already granted, storage and service health.
3. MANA identifies missing software features and creates a development plan.
4. It searches trusted source/documentation endpoints, generates source patches and records provenance.
5. Generated changes are built and tested in an isolated CI/build workspace. The live installed app/system is never edited in place.
6. MANA packages a versioned update, verifies the publisher signature, hash, target device/build, dependencies and rollback metadata, then stages it.
7. App/user-space modules may be installed within their granted Android permission boundary. MANA cannot grant itself runtime, signature, privileged, root or Device Owner permissions.
8. Changes affecting protected platform partitions, boot chain, kernel, vendor, SELinux, AVB or recovery remain staged until the owner explicitly approves the exact verified transaction.
9. Only after a protected update is installed through the supported platform updater and its fallback is confirmed may the platform schedule the requested reboot. The reboot itself is not a way to bypass missing boot support.

## Self-editing contract

The first implementation is `ManaSelfDevelopmentEngine` in the JVM core. It coordinates:

- analysis of a requested improvement;
- generated source diff/patch;
- path validation to prevent workspace traversal and writes to Git metadata, build output or signing-key directories;
- isolated unit/static/compatibility/security checks;
- signed artifact staging with SHA-256 metadata and rollback availability;
- owner approval for protected platform changes;
- install result and post-install reboot scheduling.

The engine delegates actual code generation, sandbox execution, signing, installation and reboot scheduling to a platform host. It does not pretend those adapters already exist. The running APK cannot rewrite its own installed binary in place; it must produce a new signed artifact and install it through Android's supported update mechanism.

## Permission and safety model

- MANA may inspect its own app state and public Android device facts.
- It requests only permissions required by a specific feature, through Android's user-visible permission flow.
- It never self-grants permissions, silently elevates privileges, bypasses SELinux/Verified Boot, unlocks the bootloader, or flashes arbitrary images.
- Generated code is untrusted until reviewed and tested in isolation.
- Protected changes require an exact-device package, verified signature/hash, anti-rollback policy, atomic staging and a tested recovery route.
- Failed tests, signature checks, compatibility checks or missing rollback support stop the transaction.
- No reboot occurs merely because code generation completed.

## RMX5566-specific boundary

Target remains realme C85 4G, RMX5566RU / RE645F, Snapdragon 685 (SM6225/bengal), Android 16, build RMX5566_16.0.5.1310(EX01). The OEM realme/OPlus kernel, vendor, firmware and boot chain must be preserved unless exact compatible replacement sources and a tested restore route are available. Self-development cannot synthesize undocumented hardware interfaces from nothing.

## Current implementation boundary

The JVM orchestration contract and host-side tests are added first. Android-specific sandbox/build integration, CI patch builder, trusted signing service, platform update-engine adapter, A/B rollback integration, permission onboarding UI and actual-device validation remain subsequent engineering tasks. A passing JVM test is not a flashable ROM or proof of RMX5566 installation.
