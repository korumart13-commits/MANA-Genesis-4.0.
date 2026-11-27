#!/usr/bin/env bash
# Execute real Soong module builds and, optionally, the on-device Binder integration test.
# This script changes only the supplied AOSP checkout and connected test target.
set -euo pipefail

usage() {
  cat >&2 <<'USAGE'
Usage:
  run_mana_soong_integration.sh <AOSP_TOP> <PRODUCT_TARGET> <UNSIGNED_RELEASE_APK> [--run-device-tests]

Requirements:
  - Full Android 16 AOSP checkout with vendor/mana/genesis attached.
  - Product target whose image includes ManaBrainService and the staged ManaLauncher.
  - Android build host with the AOSP-supported JDK/toolchain and sufficient disk.
  - For --run-device-tests: a booted emulator/device running that product image and adb/atest.
USAGE
  exit 2
}

[[ $# -ge 3 && $# -le 4 ]] || usage
AOSP_TOP="$(cd "$1" && pwd)"
PRODUCT_TARGET="$2"
APK="$(cd "$(dirname "$3")" && pwd)/$(basename "$3")"
RUN_DEVICE_TESTS="${4:-}"
[[ -z "$RUN_DEVICE_TESTS" || "$RUN_DEVICE_TESTS" == "--run-device-tests" ]] || usage

[[ -f "$AOSP_TOP/build/envsetup.sh" && -f "$AOSP_TOP/build/make/core/envsetup.mk" ]] || {
  echo "ERROR: AOSP_TOP is not a full Android source checkout." >&2
  exit 1
}
MANA_TOP="$AOSP_TOP/vendor/mana/genesis"
[[ -d "$MANA_TOP" ]] || {
  echo "ERROR: MANA source is not attached at vendor/mana/genesis." >&2
  exit 1
}
[[ -f "$APK" ]] || { echo "ERROR: unsigned release APK not found: $APK" >&2; exit 1; }

cd "$AOSP_TOP"
# shellcheck disable=SC1091
source build/envsetup.sh
lunch "$PRODUCT_TARGET"

# Refuse to run against a non-Android-16 tree or a product that omits either MANA package.
PLATFORM_SDK_VERSION="$(get_build_var PLATFORM_SDK_VERSION)"
[[ "$PLATFORM_SDK_VERSION" == "36" ]] || {
  echo "ERROR: expected Android 16 / API 36, got PLATFORM_SDK_VERSION=$PLATFORM_SDK_VERSION." >&2
  exit 1
}
PRODUCT_PACKAGES_LIST="$(get_build_var PRODUCT_PACKAGES)"
for required_package in ManaBrainService ManaLauncher; do
  if ! printf '%s\\n' "$PRODUCT_PACKAGES_LIST" | tr ' ' '\\n' | grep -Fxq "$required_package"; then
    echo "ERROR: lunch target $PRODUCT_TARGET does not include $required_package in PRODUCT_PACKAGES." >&2
    echo "Include vendor/mana/genesis/system/mana/mana_product.mk in the selected product." >&2
    exit 1
  fi
done

# Import the exact unsigned Gradle release artifact. The staging script rejects signed input.
"$MANA_TOP/tools/stage_mana_aosp_prebuilt.sh" "$AOSP_TOP" "$APK"

# Run Soong's own test suite, then compile the exact MANA product modules and test packages.
m nothing --no-skip-soong-tests
m -j"${MANA_BUILD_JOBS:-$(nproc)}" \
  mana-brain-interface \
  ManaBrainService \
  ManaLauncher \
  mana-brain-request-policy-tests \
  ManaBrainBinderIntegrationTests

# Verify the actual imported APK emitted by this product build against its platform cert.
PRODUCT_OUT="$AOSP_TOP/out/target/product/$TARGET_PRODUCT"
SIGNED_APK=""
if [[ -d "$PRODUCT_OUT" ]]; then
  SIGNED_APK="$(find "$PRODUCT_OUT" -type f -name ManaLauncher.apk -print -quit)"
fi
if [[ -z "$SIGNED_APK" ]]; then
  SIGNED_APK="$(find "$AOSP_TOP/out/soong/.intermediates" -type f -name ManaLauncher.apk -print -quit 2>/dev/null || true)"
fi
[[ -n "$SIGNED_APK" ]] || {
  echo "ERROR: Soong did not emit a ManaLauncher.apk; refusing to skip signature verification." >&2
  exit 1
}
BRAIN_SIGNED_APK=""
if [[ -d "$PRODUCT_OUT" ]]; then
  BRAIN_SIGNED_APK="$(find "$PRODUCT_OUT" -type f -name ManaBrainService.apk -print -quit)"
fi
if [[ -z "$BRAIN_SIGNED_APK" ]]; then
  BRAIN_SIGNED_APK="$(find "$AOSP_TOP/out/soong/.intermediates" -type f -name ManaBrainService.apk -print -quit 2>/dev/null || true)"
fi
[[ -n "$BRAIN_SIGNED_APK" ]] || {
  echo "ERROR: Soong did not emit a ManaBrainService.apk; refusing to skip signature verification." >&2
  exit 1
}
"$MANA_TOP/tools/verify_mana_platform_signature.sh" "$AOSP_TOP" "$SIGNED_APK" "${MANA_PLATFORM_CERT:-$AOSP_TOP/build/target/product/security/platform.x509.pem}" "$BRAIN_SIGNED_APK"

# Run the host policy tests as a separate test result.
atest --host mana-brain-request-policy-tests

if [[ "$RUN_DEVICE_TESTS" == "--run-device-tests" ]]; then
  command -v adb >/dev/null 2>&1 || {
    echo "ERROR: adb is required for device-side Binder tests." >&2
    exit 1
  }
  command -v atest >/dev/null 2>&1 || {
    echo "ERROR: atest is required for device-side Binder tests." >&2
    exit 1
  }
  adb get-state >/dev/null
  # Do not let atest report a misleading test failure when the product packages are absent.
  adb shell pm path com.mana.system.brain >/dev/null || {
    echo "ERROR: ManaBrainService is not installed in the running image." >&2
    exit 1
  }
  adb shell pm path com.mana.launcher >/dev/null || {
    echo "ERROR: ManaLauncher with ManaEngineBridgeService is not installed in the running image." >&2
    exit 1
  }
  atest ManaBrainBinderIntegrationTests
fi

echo "MANA Soong module build completed."
if [[ "$RUN_DEVICE_TESTS" == "--run-device-tests" ]]; then
  echo "MANA Brain -> Engine Bridge Binder instrumentation test completed."
else
  echo "Device-side Binder test was not requested; no runtime connection is claimed."
fi
