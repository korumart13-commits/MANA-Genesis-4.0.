# MANA system layer

This is the planned AOSP-integrated MANA system layer. Components here will be built as platform/product modules and privileged services, not as a separately installed launcher app.

## Planned packages

- `ManaSystemCore`: boot phases, module dependency graph and health reporting (implemented in `os/core` as a JVM-testable foundation).
- `ManaBrainService`: local-first inference service; cloud providers remain opt-in.
- `ManaModelManager`: model import, signature/hash checks, license acknowledgement, storage/RAM budgeting and model lifecycle.
- `ManaAvatarService`: character state, animation, voice and assistant invocation.
- `ManaLauncher`, `ManaSystemUI`, `ManaSettings`, `ManaControlCenter`: integrated platform UI.
- `ManaReceptionist`, `ManaDiary`, `ManaLifeManager`, `ManaContactIntelligence`, `ManaGameIntelligence`: permission-gated system features.
- `ManaUpdateService`: signed update verification, staged install and rollback checks.

## Integration rule

Each service must declare its Android permissions, SELinux domain, Binder/API boundary, data ownership and failure behavior before it is added to a product image. No service may silently acquire root, bypass permission prompts or transmit private content to a cloud model.
