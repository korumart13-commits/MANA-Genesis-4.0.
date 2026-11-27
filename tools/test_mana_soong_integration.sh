#!/usr/bin/env bash
set -euo pipefail

bash -n tools/run_mana_soong_integration.sh
grep -F 'm nothing --no-skip-soong-tests' tools/run_mana_soong_integration.sh
grep -F 'PLATFORM_SDK_VERSION="$(get_build_var PLATFORM_SDK_VERSION)"' tools/run_mana_soong_integration.sh
grep -F 'PLATFORM_SDK_VERSION" == "36"' tools/run_mana_soong_integration.sh
grep -F 'get_build_var PRODUCT_PACKAGES' tools/run_mana_soong_integration.sh
grep -F 'ManaBrainService ManaLauncher' tools/run_mana_soong_integration.sh
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
grep -F 'ManaBrainService.apk' tools/run_mana_soong_integration.sh
grep -F 'verify_mana_platform_signature.sh' tools/run_mana_soong_integration.sh
grep -F '"$BRAIN_SIGNED_APK"' tools/run_mana_soong_integration.sh
grep -F 'android_app_import' tools/stage_mana_aosp_prebuilt.sh
grep -F 'certificate: "platform"' tools/stage_mana_aosp_prebuilt.sh
grep -F 'android:permission="com.mana.permission.BIND_BRAIN"' launcher/app/src/main/AndroidManifest.xml
grep -F 'android:permission="com.mana.permission.BIND_BRAIN"' system/mana/services/ManaBrainService/AndroidManifest.xml
grep -F 'com.mana.launcher.ManaEngineBridgeService' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'MAX_BACKEND_REBIND_ATTEMPTS = 5' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'BACKEND_REBIND_BASE_MS = 2_000L' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'BACKEND_CONNECT_TIMEOUT_MS = 15_000L' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'REQUEST_CALLBACK_TIMEOUT_MS = 185_000L' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'end-to-end service deadline' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'pendingRequestTimeouts' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'boolean isDelivered()' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainTerminalCallbackGate.java
grep -F 'ENGINE_TIMEOUT' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'Ignoring duplicate or late MANA Engine result callback.' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'backendConnectTimeout' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'scheduleBackendRebind()' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'RemoteException | SecurityException' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'ENGINE_PERMISSION_DENIED' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'ManaBrainTerminalCallbackGate terminalGate' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'forwardToEngine(request, callback, terminalGate, requestTimeout)' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'boolean tryDeliver()' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainTerminalCallbackGate.java
grep -F 'concurrentCallbacksOnlyDeliverOnce' system/mana/services/ManaBrainService/tests/src/com/mana/system/brain/ManaBrainTerminalCallbackGateTest.java
grep -F 'ManaBrainTerminalCallbackGateTest.java' system/mana/services/ManaBrainService/Android.bp
grep -F '"READY_NO_LOCAL_MODEL".equals(engineState)' system/mana/services/ManaBrainService/src/com/mana/system/brain/ManaBrainService.java
grep -F 'getProtocolVersion()' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'UNAVAILABLE_ENGINE_BACKEND_DISCONNECTED' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'brain.think(request' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'IManaBrainCallback.Stub()' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F '"ENGINE_ERROR"' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'Local inference is unavailable and cloud processing is not permitted.' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F '"READY".equals(state)' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F '"READY_NO_LOCAL_MODEL".equals(state)' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'Boolean.TRUE.equals(resultLocal.get())' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'callbackCount.get()' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'callbackDelivered.await(180, TimeUnit.SECONDS)' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'android:targetPackage="com.mana.system.brain"' system/mana/tests/ManaBrainBinderIntegration/AndroidManifest.xml
grep -F 'getInstrumentation().getContext()' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
grep -F 'context.checkSelfPermission("com.mana.permission.BIND_BRAIN")' system/mana/tests/ManaBrainBinderIntegration/src/com/mana/system/brain/tests/ManaBrainBinderIntegrationTest.java
echo "MANA Soong and Binder integration contract checks passed."
