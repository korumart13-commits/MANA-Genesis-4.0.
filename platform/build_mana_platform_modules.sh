#!/usr/bin/env bash
set -Eeuo pipefail

# Builds MANA's source-backed Soong modules inside an initialized AOSP tree.
# A valid lunch target must be supplied by the build host.
# This does not build a device image, sign a release, or flash a phone.

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
TOP="${ANDROID_BUILD_TOP:-$PWD}"
TARGET="${1:-${MANA_LUNCH_TARGET:-}}"
[[ -f "$TOP/build/envsetup.sh" ]] || die "Run this from an AOSP source tree or set ANDROID_BUILD_TOP."
[[ -d "$TOP/vendor/mana/genesis/system/mana" ]] || die "MANA source tree is not synced at vendor/mana/genesis."
[[ -n "$TARGET" ]] || die "Pass a verified AOSP lunch target as the first argument."

cd "$TOP"
source build/envsetup.sh
lunch "$TARGET"
[[ -n "${TARGET_PRODUCT:-}" ]] || die "Lunch did not select a TARGET_PRODUCT."

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
