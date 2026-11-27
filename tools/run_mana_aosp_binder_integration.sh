#!/usr/bin/env bash
# Run actual Soong builds and on-device Binder integration tests in a prepared AOSP tree.
# This script never flashes, unlocks, wipes, or writes to device partitions.
set -euo pipefail

usage() {
  echo "Usage: $0 <AOSP_TOP> <LUNCH_TARGET> <UNSIGNED_MANA_RELEASE_APK>" >&2
  exit 2
}
[[ $# -eq 3 ]] || usage

AOSP_TOP="$(cd "$1" && pwd)"
LUNCH_TARGET="$2"
APK="$(cd "$(dirname "$3")" && pwd)/$(basename "$3")"
[[ -f "$AOSP_TOP/build/envsetup.sh" ]] || { echo "ERROR: not an AOSP source tree." >&2; exit 1; }
[[ -f "$AOSP_TOP/build/make/core/envsetup.mk" ]] || { echo "ERROR: AOSP build core is missing." >&2; exit 1; }
[[ -f "$APK" ]] || { echo "ERROR: unsigned release APK not found: $APK" >&2; exit 1; }

cd "$AOSP_TOP"
source build/envsetup.sh
lunch "$LUNCH_TARGET"

MANA_ROOT="$AOSP_TOP/vendor/mana/genesis"
[[ -d "$MANA_ROOT" ]] || { echo "ERROR: MANA source is not synced at vendor/mana/genesis." >&2; exit 1; }
"$MANA_ROOT/tools/stage_mana_aosp_prebuilt.sh" "$AOSP_TOP" "$APK"

# Build the actual Soong modules. No ROM image or device partition is produced by this command.
m mana-brain-interface ManaBrainService ManaLauncher mana-brain-binder-integration-tests

command -v adb >/dev/null 2>&1 || { echo "ERROR: adb is required for device-side tests." >&2; exit 1; }
adb get-state >/dev/null 2>&1 || {
  echo "ERROR: connect a test device/emulator already running a compatible MANA product image." >&2
  exit 1
}
adb shell pm path com.mana.system.brain >/dev/null 2>&1 || {
  echo "ERROR: ManaBrainService is not installed in the running test image." >&2
  exit 1
}
adb shell pm path com.mana.launcher >/dev/null 2>&1 || {
  echo "ERROR: ManaLauncher with ManaEngineBridgeService is not installed in the running test image." >&2
  exit 1
}

atest mana-brain-binder-integration-tests
