#!/usr/bin/env bash
set -Eeuo pipefail

# Builds MANA's source-backed Soong modules inside an already initialized AOSP tree.
# A valid lunch target must be selected by the build host first.
# This does not build a device image, sign a release, or flash a phone.

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
TOP="${ANDROID_BUILD_TOP:-}"
[[ -n "$TOP" && -f "$TOP/build/envsetup.sh" ]] || die "Set ANDROID_BUILD_TOP to a synced AOSP source tree."
[[ -d "$TOP/vendor/mana/genesis/system/mana" ]] || die "MANA source tree is not synced at vendor/mana/genesis."
[[ -n "${TARGET_PRODUCT:-}" ]] || die "Select a valid AOSP lunch target before running this script."

cd "$TOP"
source build/envsetup.sh
[[ -n "${TARGET_PRODUCT:-}" ]] || die "No TARGET_PRODUCT is selected. Run lunch first."

MODULES=(
  mana-brain-interface
  ManaBrainService
  mana-brain-request-policy-tests
  ManaSettings
  ManaControlCenter
)

printf 'Building MANA Soong modules for selected platform target: %s\n' "$TARGET_PRODUCT"
m "${MODULES[@]}"

printf '\nMANA platform module build completed for %s.\n' "$TARGET_PRODUCT"
printf 'This is not a device-specific ROM build and does not produce approved RMX5566RU flash images.\n'
