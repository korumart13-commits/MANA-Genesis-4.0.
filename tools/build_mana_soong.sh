#!/usr/bin/env bash
set -euo pipefail
AOSP_TOP="${1:-${ANDROID_BUILD_TOP:-}}"
UNSIGNED_APK="${2:-}"
if [[ -z "$AOSP_TOP" || -z "$UNSIGNED_APK" ]]; then
  echo "Usage: $0 <ANDROID_BUILD_TOP> <app-release-unsigned.apk>" >&2
  exit 2
fi
AOSP_TOP="$(cd "$AOSP_TOP" && pwd)"
UNSIGNED_APK="$(cd "$(dirname "$UNSIGNED_APK")" && pwd)/$(basename "$UNSIGNED_APK")"
[[ -x "$AOSP_TOP/build/soong/soong_ui.bash" ]] || { echo "ERROR: no complete AOSP/Soong checkout at $AOSP_TOP" >&2; exit 3; }
[[ -f "$UNSIGNED_APK" ]] || { echo "ERROR: unsigned Gradle APK not found: $UNSIGNED_APK" >&2; exit 4; }
export ANDROID_BUILD_TOP="$AOSP_TOP"
cd "$AOSP_TOP"
MANA_ROOT="$AOSP_TOP/vendor/mana/genesis"
[[ -x "$MANA_ROOT/tools/stage_mana_aosp_prebuilt.sh" ]] || { echo "ERROR: MANA source missing; install pinned local manifest and repo sync first." >&2; exit 5; }
"$MANA_ROOT/tools/stage_mana_aosp_prebuilt.sh" "$AOSP_TOP" "$UNSIGNED_APK"
# Generic AOSP target compiles modules only; it is not an RMX5566 product.
source build/envsetup.sh
lunch aosp_x86_64-userdebug
m -j"$(nproc)" ManaLauncher ManaBrainService mana-brain-interface mana-brain-request-policy-tests mana-brain-binder-integration-tests
echo "Soong module build completed for generic AOSP target."
echo "This does not build an RMX5566 image, verify device compatibility, or authorize flashing."
