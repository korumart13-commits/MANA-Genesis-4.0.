# MANA system layer

This is the planned AOSP-integrated MANA system layer. Components will be built as platform/product modules and privileged services, not as a separately installed launcher app.

## Planned packages

- `ManaSystemCore`: boot phases, module dependency graph and health reporting (implemented in `os/core` as a JVM-testable foundation).
- `ManaSystemModuleCatalog`: typed catalog of platform and MANA modules, dependency order, criticality and failure policy (implemented and covered by JVM tests).
- `ManaBrainService`: local-first inference service; cloud providers remain opt-in.
- `ManaModelManager`: model import, signature/hash checks, license acknowledgement, storage/RAM budgeting and model lifecycle.
- `ManaAvatarService`: character state, animation, voice and assistant invocation.
- `ManaLauncher`, `ManaSystemUI`, `ManaSettings`, `ManaControlCenter`: integrated platform UI.
- `ManaReceptionist`, `ManaDiary`, `ManaLifeManager`, `ManaContactIntelligence`, `ManaGameIntelligence`: permission-gated system features.
- `ManaUpdateService`: signed update verification, staged install and rollback checks.

The current dependency and failure-policy map is documented in [os/SYSTEM_MODULE_GRAPH.md](../../os/SYSTEM_MODULE_GRAPH.md).

## Integration rule

Each service must declare its Android permissions, SELinux domain, Binder/API boundary, data ownership and failure behavior before it is added to a product image. No service may silently acquire root, bypass permission prompts or transmit private content to a cloud model.

The catalog is architecture metadata, not proof that Android Binder services or privileged packages are already built. Android `init` service declarations, package definitions and SELinux policy will be added only against a checked-out AOSP tree and verified target configuration.
