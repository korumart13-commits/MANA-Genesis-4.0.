# MANA Genesis 4.0 — 3D avatar asset and runtime

## Asset

The original MANA character is stored as embedded-buffer glTF 2.0 assets under
`launcher/app/src/main/assets/`:

- `mana_avatar_genesis.gltf` — base dark-navy outfit and accessory.
- `mana_avatar_genesis_no_accessory.gltf` — base outfit without the hair ornament.
- `mana_avatar_genesis_casual.gltf` and `..._no_accessory.gltf`.
- `mana_avatar_genesis_evening.gltf` and `..._no_accessory.gltf`.

Each variant contains the same 49 named full-body character parts, 12 PBR materials,
and four named animation clips: `Idle`, `Speaking`, `Happy`, and `Playful`.
The asset is original procedural geometry authored for MANA; it does not reuse
MiSide character meshes, textures, or extracted game assets.

## Android rendering

- SceneView 4.18.0 uses Filament/glTF rendering on Android.
- `ManaAvatarPreviewActivity` previews the selected outfit.
- `ManaFloatingAvatarService` hosts the model in a transparent, draggable,
  non-focusable overlay after the owner grants Android's display-over-other-apps permission.
- The overlay snaps to the left or right edge and opens the MANA assistant when tapped.
- The owner can change outfit/accessory in MANA settings; the service reloads the
  selected asset. Expression state selects the named animation. TTS start/done/error
  updates the shared speaking state to select the `Speaking` clip.

## Honest technical boundary

This is a real animated full-body glTF model and an Android Filament renderer,
not a static avatar icon. The current asset is a stylized articulated node model:
it uses named mesh parts and node-transform animation, not a production-quality
weighted skeletal skin or facial blendshape rig. The speaking clip is a looping
mouth animation selected during TTS; phoneme-level viseme synchronization is not
implemented yet. The Android CI build validates source compilation and glTF
structure, but only on-device testing can verify rendering, frame rate, thermal
load, touch behavior, and overlay lifecycle on the realme RMX5566RU.
