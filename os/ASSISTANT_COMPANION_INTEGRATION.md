# MANA system assistant ↔ Genesis companion integration

The AOSP product app `com.mana.assistant` is a lightweight system entry point. It now explicitly opens `com.mana.genesis/com.mana.launcher.MainActivity`, requests the companion's avatar/chat panel, and forwards a non-empty initial prompt as an intent extra.

The richer Gradle application now uses applicationId `com.mana.genesis` while retaining Kotlin namespace `com.mana.launcher`. This avoids a package collision with the AOSP HOME launcher, which remains `com.mana.launcher`, and allows both packages to be installed in one product. The Android companion now includes the canonical `engine/mana-brain` Gradle project directly; the duplicate Brain routing source was removed so JVM tests and Android runtime consume the same implementation.

The companion handles the handoff by opening the MANA avatar/chat panel and placing the initial prompt into its input field. It does not auto-submit the prompt. The owner must tap Send, preserving user control and avoiding accidental network or model processing.

## Runtime requirements and limitations

- Both packages must be installed: AOSP `ManaAssistant` and the Gradle companion APK `com.mana.genesis`.
- The local LiteRT-LM model must be separately downloaded/selected and successfully loaded before local inference works.
- Cloud providers require the owner's credentials and explicit consent; sensitive requests remain local-only.
- If the companion is absent, the system entry point explains that it is unavailable instead of claiming the request was processed.
- This integration is an app-level handoff, not a privileged Binder service and not a replacement for Android SystemUI/framework/kernel.
