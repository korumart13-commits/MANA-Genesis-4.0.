#!/usr/bin/env bash
set -euo pipefail

required=(
  "engine/mana-brain/Android.bp"
  "engine/mana-brain/src/main/kotlin/com/mana/brain/ManaBrain.kt"
  "engine/mana-brain/src/main/kotlin/com/mana/brain/ManaBrainFacade.kt"
  "engine/mana-brain/src/main/kotlin/com/mana/brain/ManaBrainAndroidRuntime.kt"
  "system/mana/brain/aidl/Android.bp"
  "system/mana/apps/ManaAssistant/src/com/mana/assistant/ManaBrainBinderService.java"
  "system/mana/apps/ManaAssistant/tests/Android.bp"
  "system/mana/apps/ManaAssistant/tests/src/com/mana/assistant/tests/ManaBrainBinderTest.java"
  "system/mana/apps/ManaLauncher/src/com/mana/launcher/ManaHomeActivity.java"
)
for path in "${required[@]}"; do
  test -s "$path" || { echo "Missing integration file: $path" >&2; exit 1; }
done

grep -q '"mana-brain-core"' system/mana/apps/ManaAssistant/Android.bp
grep -q '"mana.brain-java"' system/mana/apps/ManaAssistant/Android.bp
grep -q 'ManaBrainBinderService' system/mana/apps/ManaAssistant/AndroidManifest.xml
grep -q 'bindService' system/mana/apps/ManaLauncher/src/com/mana/launcher/ManaHomeActivity.java
grep -q 'brainService.submit' system/mana/apps/ManaLauncher/src/com/mana/launcher/ManaHomeActivity.java
grep -q 'instrumentation_for: "ManaAssistant"' system/mana/apps/ManaAssistant/tests/Android.bp
grep -q 'ManaLauncher' system/mana/mana_product.mk
grep -q 'ManaAssistant' system/mana/mana_product.mk

echo "MANA Android integration source checks passed."
echo "This is a static source check, not an AOSP/Soong product build."
