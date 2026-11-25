# MANA avatar and voice prototype

## Current source implementation

- `ManaAvatar3DView.kt` renders a full-body stylized character using OpenGL ES 2.0 lit sphere meshes.
- Character design: long dark hair, blue highlights/eyes, dark navy clothing, face, hands/arms, legs and boots.
- Idle animation: body bob, head/hair sway, subtle articulated arm and leg swing, periodic blinking.
- Appearance controls: three outfit palettes, accessory toggle and three facial expression states; body/face/hair identity remains fixed.
- Speaking animation: mouth movement is toggled by the Android TextToSpeech utterance listener.
- `ManaFloatingAvatarService.kt` hosts the renderer in a draggable overlay window. The owner must explicitly grant Android's special “display over other apps” permission. The launcher stores an explicit enable/disable preference, starts a foreground service with a persistent stop action, and resumes the GL surface when attached.
- The overlay does not capture keyboard focus or inject touches into the foreground application.
- The launcher has Russian speech recognition through Android's recognizer intent and Russian TextToSpeech output. It tries a Russian voice whose engine-provided name looks female; actual voice gender and offline availability depend on the installed TTS engine and installed voice data.

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
