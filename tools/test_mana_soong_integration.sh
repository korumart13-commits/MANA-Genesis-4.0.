#!/usr/bin/env bash
set -euo pipefail

bash -n tools/run_mana_soong_integration.sh
grep -F 'm nothing --no-skip-soong-tests' tools/run_mana_soong_integration.sh
grep -F 'mana-brain-interface' tools/run_mana_soong_integration.sh
grep -F 'ManaBrainService' tools/run_mana_soong_integration.sh
grep -F 'ManaLauncher' tools/run_mana_soong_integration.sh
grep -F 'ManaBrainBinderIntegrationTests' tools/run_mana_soong_integration.sh
grep -F 'verify_mana_platform_signature.sh' tools/run_mana_soong_integration.sh
grep -F 'ManaLauncher.apk' tools/run_mana_soong_integration.sh
grep -F 'pm path com.mana.system.brain' tools/run_mana_soong_integration.sh
grep -F 'pm path com.mana.launcher' tools/run_mana_soong_integration.sh
grep -F 'atest --host mana-brain-request-policy-tests' tools/run_mana_soong_integration.sh
grep -F 'atest ManaBrainBinderIntegrationTests' tools/run_mana_soong_integration.sh
grep -F 'android_app_import' tools/stage_mana_aosp_prebuilt.sh
grep -F 'certificate: "platform"' tools/stage_mana_aosp_prebuilt.sh
grep -F 'android:permission="com.mana.permission.BIND_BRAIN"' launcher/app/src/main/AndroidManifest.xml
grep -F 'android:permission="com.mana.permission.BIND_BRAIN"' system/mana/services/ManaBrainService/AndroidManifest.xml
grep -F 'com.mana.launcher.ManaEngineBridgeService' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'getProtocolVersion()' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'brain.think(request' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'IManaBrainCallback.Stub()' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'ENGINE_UNAVAILABLE' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
echo "MANA Soong and Binder integration contract checks passed."
