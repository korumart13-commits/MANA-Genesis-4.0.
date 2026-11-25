# MANA avatar and voice prototype

## Current source implementation

- The floating avatar and full-screen preview use SceneView/Filament to render an original full-body animated glTF character.
- Three wardrobe styles (base, casual and evening) are available, each with accessories enabled or removed.
- Every wardrobe/accessory combination has neutral, playful and open-mouth speaking geometry variants. All variants preserve the four glTF clips: `Idle`, `Speaking`, `Happy` and `Playful`.
- `ManaAvatarState` is Compose-observable and shared by the launcher, preview and floating service. The selected wardrobe and accessory are persisted in private app preferences.
- The launcher cycles neutral, playful and happy expressions. TextToSpeech utterance callbacks switch the shared state to the open-mouth speaking variant and `Speaking` animation. This is not phoneme-level viseme lip-sync.
- The floating overlay is draggable, non-focusable, user-enabled through Android's “display over other apps” permission, and runs with a visible stoppable foreground-service notification. Tapping it opens the MANA assistant.
- The full-screen preview supports orbit gestures and follows the same appearance state.
- Russian speech recognition uses Android's recognizer intent; Russian TextToSpeech uses an installed engine voice where available. Actual voice quality, gender presentation and offline availability depend on installed TTS voice data.
- `ManaAvatar3DView.kt` remains as a legacy procedural OpenGL renderer; the current floating service uses the glTF/Filament renderer.
- CI validates every embedded glTF asset and runs avatar selection unit tests before Android assembly. The overlay still requires GPU, frame-time, thermal, battery and touch testing on the owner's realme RMX5566RU.

## Important limitations

This is a real-time procedural 3D prototype, not yet the final high-detail character asset. It uses parametric primitive meshes rather than a skinned GLB/ glTF model with a proper skeleton, inverse-kinematics, facial blendshapes, detailed hair strands, clothing physics and interchangeable wardrobe. It has not yet been verified on the owner's RMX5566RU GPU, display cutouts, battery use, overlay behavior after process death, or Android 16 vendor firmware.

The overlay lives in the legacy Gradle launcher prototype. It is not yet part of the Soong MANA OS product or privileged System UI. The Android permission is granted by the user; the app does not bypass Android security.

## Build and validation

Workflow: [MANA Launcher — Gradle source verification](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/workflows/mana-launcher.yml)

The integrated launcher, Brain source mirror, local memory, diary and Life Manager source compiled successfully in earlier runs. The OpenGL renderer, TTS, outfit/accessory/expression controls and foreground overlay source also compiled in earlier runs. The current foreground-service lifecycle and latest articulation refinements are being recompiled now. A successful Gradle compile validates Kotlin/Android packaging only, not GPU rendering or device behavior.

## Next acceptance gates

1. Pass Gradle compile after the 3D renderer and TTS commits.
2. Add emulator/instrumentation coverage for shader initialization, overlay permission flow, touch drag/tap, and service teardown.
3. Install and test on the realme RMX5566RU with the owner's consent; measure frame time, thermal and battery cost.
4. Replace primitive meshes with an original rigged glTF/GLB character asset with documented licensing and stable identity; add a proper skeleton and facial blendshapes.
5. Add outfit/accessory variants, expression states, speech visemes and animation controls.
6. Move the renderer and service contract into the integrated Soong MANA OS product after the target AOSP product is available.
