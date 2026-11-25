# MANA Genesis 4.0 — source-level security review (2026-11-25)

## Scope

Review of the repository's current JVM core, MANA Brain routing contract, device profile, CI configuration and newly added Android UI source. This is not a penetration test, AOSP build, device test, or certification.

## Findings and corrections

1. The previous core CI run had 19 tests with one failure: dependency-cycle validation happened after the preflight validation block, and the boot method swallowed the cycle exception into a degraded health result. The dependency order is now computed during preflight, before any module starts; fatal JVM errors are no longer caught as ordinary service failures.
2. The target profile contained an obsolete instruction to build a standalone APK first. It now follows the owner's later direction: integrate the launcher and assistant into the OS source; do not start a separate APK workflow.
3. ManaBrain remains a routing contract. It prefers a local provider and only permits cloud fallback when explicitly enabled and the request is not sensitive. No real provider is wired in this repository.
4. Security and storage services remain host-JVM policy models. They do not enforce Android permissions, SELinux, filesystem access controls or encryption.
5. The assistant UI does not claim to be an operational AI. Until Binder and LiteRT-LM are integrated, submitted text is not sent to a model or network.
6. Android modules request no runtime-dangerous permissions and are not marked privileged. The product make fragment must only be included after the exact AOSP product is verified.

## Residual risks / release blockers

- No AOSP source checkout, validated device tree, exact kernel source, vendor blobs, VINTF, SELinux policy, AVB/signing metadata or tested recovery route.
- No LiteRT-LM runtime/model, cloud provider adapters or secure credential storage.
- No Android Binder boundary, service SELinux domains, app sandbox integration or end-to-end permission enforcement.
- UI modules have not been compiled against an AOSP tree and have not been run on the target phone.
- Device bootloader is reported locked and Verified Boot green; no boot-chain modification or flash package is authorized.

## Verdict

The current source foundation is suitable for continued controlled development, but it is not a complete operating system, not a bootable ROM, and not ready to install. The launcher and assistant have been added as OS product source modules only. Do not flash or install generated images from this repository until all device-specific gates are passed.
