# MANA avatar and voice prototype

## Current source implementation

- The character is an original full-body glTF model with dark long hair, facial details, navy clothing and boots.
- The floating overlay and full-screen preview use SceneView/Filament and a shared observable `ManaAvatarState`.
- Three wardrobe styles (base, casual and evening) are available, each with accessories enabled or removed.
- Every wardrobe/accessory combination has neutral, playful and open-mouth speaking geometry variants. All variants preserve the four glTF animation clips: `Idle`, `Speaking`, `Happy` and `Playful`.
- The launcher cycles neutral, playful and happy expressions. TextToSpeech utterance callbacks update the speaking state; the model switches to its open-mouth speaking variant and `Speaking` animation. This is not phoneme-level viseme lip-sync.
- The floating overlay is draggable, non-focusable, user-enabled through Android's “display over other apps” permission, and runs with a visible stoppable foreground-service notification. Tapping it opens the MANA assistant.
- The full-screen preview supports orbit gestures and follows the same appearance state.
- Russian speech recognition uses Android's recognizer intent; Russian TextToSpeech uses an installed engine voice where available. Actual voice quality, gender presentation and offline availability depend on the installed TTS engine and voice data.
- `ManaAvatar3DView.kt` remains as a legacy procedural OpenGL renderer; the current floating service uses the glTF/Filament renderer.
- Automated glTF validation and launcher compilation are in CI. The overlay still requires GPU, frame-time, thermal, battery and touch testing on the owner's realme RMX5566RU.

## Important limitations

This is a real-time animated glTF prototype, not yet a production-quality rigged character. It uses separate mesh parts and node animations rather than a full skinned skeleton with inverse kinematics, phoneme-level facial blendshapes, detailed hair simulation or clothing physics. The wardrobe and expression variants are real model assets, but they are not a substitute for a fully rigged character. It has not yet been verified on the owner's RMX5566RU GPU, display cutouts, battery use, overlay behavior after process death, or Android 16 vendor firmware.

The overlay lives in the legacy Gradle launcher prototype. It is not yet part of the Soong MANA OS product or privileged System UI. The Android permission is granted by the user; the app does not bypass Android security.

## Build and validation

Workflow: [MANA Launcher — Gradle source verification](https://github.com/korumart13-commits/MANA-Genesis-4.0./actions/workflows/mana-launcher.yml)

The integrated launcher, Brain source mirror, local memory, diary and Life Manager source compiled successfully in earlier runs. The OpenGL renderer, TTS, outfit/accessory/expression controls and foreground overlay source also compiled in earlier runs. The current foreground-service lifecycle and latest articulation refinements are being recompiled now. A successful Gradle compile validates Kotlin/Android packaging only, not GPU rendering or device behavior.

## Next acceptance gates

1. Pass Gradle compile after the 3D renderer and TTS commits.
2. Add emulator/instrumentation coverage for shader initialization, overlay permission flow, touch drag/tap, and service teardown.
3. Install and test on the realme RMX5566RU with the owner's consent; measure frame time, thermal and battery cost.
4. Upgrade the current glTF mesh-part character to a fully rigged asset with a documented license, stable identity, skeleton, facial blendshapes and phoneme-level speech visemes.
5. Add emulator/instrumentation coverage for live wardrobe changes, expression animation, speech state and overlay lifecycle.
6. Move the renderer and service contract into the integrated Soong MANA OS product after the target AOSP product is available.
