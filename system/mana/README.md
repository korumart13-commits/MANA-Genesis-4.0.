# MANA system layer

This is the planned AOSP-integrated MANA system layer. Components will be built as platform/product modules and privileged services, not as a separately installed launcher app.

## Planned packages

- `ManaSystemCore`: boot phases, module dependency graph and health reporting (implemented in `os/core` as a JVM-testable foundation).
- `ManaSystemModuleCatalog`: typed catalog of platform and MANA modules, dependency order, criticality and failure policy (implemented and covered by JVM tests).
- `ManaBrainService`: local-first inference service; cloud providers remain opt-in.
- `ManaModelManager`: model import, signature/hash checks, license acknowledgement, storage/RAM budgeting and model lifecycle.
- `ManaAvatarService`: character state, animation, voice and assistant invocation.
- `ManaLauncher` and `ManaAssistant`: first Android/Soong product source modules under `apps/` (not yet AOSP-built).
- `ManaSystemUI`, `ManaSettings`, `ManaControlCenter`: remaining integrated platform UI modules.
- `ManaReceptionist`, `ManaDiary`, `ManaLifeManager`, `ManaContactIntelligence`, `ManaGameIntelligence`: permission-gated system features.
- `ManaUpdateService`: signed update verification, staged install and rollback checks.

The current dependency and failure-policy map is documented in [os/SYSTEM_MODULE_GRAPH.md](../../os/SYSTEM_MODULE_GRAPH.md).

## Integration rule

Each service must declare its Android permissions, SELinux domain, Binder/API boundary, data ownership and failure behavior before it is added to a product image. No service may silently acquire root, bypass permission prompts or transmit private content to a cloud model.

The catalog is architecture metadata, not proof that Android Binder services or privileged packages are already built. The launcher and assistant currently exist as ordinary product-app source modules; they are not yet compiled or included in a device image. Android `init` service declarations, package definitions and SELinux policy will be added only against a checked-out AOSP tree and verified target configuration.

## Platform Binder contract

The first platform-facing MANA Brain API now lives in `interfaces/`:
- Soong module: `mana-brain-interface`.
- AIDL entry point: `com.mana.brain.IManaBrain`.
- Requests are asynchronous and carry explicit cloud, sensitive-data and personal-memory flags.
- Responses identify the provider, local/cloud execution and memory-use count.
- `ManaAssistant` declares the interface library as a static dependency.

This is an API boundary only. The Android platform service implementation, Binder caller authorization, SELinux domain and connection to the service-owned Gradle runtime are not yet implemented. Do not mark the platform Brain service as available until those parts are built and tested in AOSP.


## Android Binder service endpoint

A permission-protected Soong module now provides `ManaBrainService`, an Android `Service`
implementing `IManaBrain`. It requires the signature-level `com.mana.permission.BIND_BRAIN`
permission and checks it again inside Binder calls. The request contract includes separate
consent for cloud inference and personal-memory export. The service deliberately returns
`ENGINE_UNAVAILABLE` until a real local backend adapter is integrated; it never fabricates
answers. See [services/ManaBrainService/README.md](services/ManaBrainService/README.md).

The module is source-only at this stage: it is not included in an AOSP product image, Soong-built,
or covered by device SELinux policy yet.
