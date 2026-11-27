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
  # The connected image must contain both product packages and use the same platform key.
  atest ManaBrainBinderIntegrationTests
fi

echo "MANA Soong module build completed."
if [[ "$RUN_DEVICE_TESTS" == "--run-device-tests" ]]; then
  echo "MANA Brain -> Engine Bridge Binder instrumentation test completed."
else
  echo "Device-side Binder test was not requested; no runtime connection is claimed."
fi
