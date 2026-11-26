#!/usr/bin/env bash
# Stage the Gradle-built MANA app into an Android 16 AOSP checkout.
# This changes only the supplied AOSP source tree; it never touches a device.
set -euo pipefail

usage() {
  echo "Usage: $0 <AOSP_TOP> <MANA_RELEASE_APK>" >&2
  exit 2
}
[[ $# -eq 2 ]] || usage

AOSP_TOP="$(cd "$1" && pwd)"
APK="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
SOURCE_REL="vendor/mana/genesis"
MODULE_REL="$SOURCE_REL/system/mana/apps/ManaLauncher"
MODULE_DIR="$AOSP_TOP/$MODULE_REL"
ANDROID_BP="$MODULE_DIR/Android.bp"
DEST_DIR="$MODULE_DIR/prebuilts"
DEST_APK="$DEST_DIR/ManaLauncher.apk"
APKANALYZER="${APKANALYZER:-apkanalyzer}"

[[ -f "$AOSP_TOP/build/make/core/envsetup.mk" ]] || {
  echo "ERROR: AOSP_TOP does not look like a full Android source checkout." >&2
  exit 1
}
[[ -d "$AOSP_TOP/$SOURCE_REL" ]] || {
  echo "ERROR: MANA source is not present at $AOSP_TOP/$SOURCE_REL." >&2
  exit 1
}
[[ -f "$APK" ]] || { echo "ERROR: APK not found: $APK" >&2; exit 1; }
command -v "$APKANALYZER" >/dev/null 2>&1 || {
  echo "ERROR: apkanalyzer is required (set APKANALYZER to its full path)." >&2
  exit 1
}

PACKAGE="$("$APKANALYZER" manifest application-id "$APK")"
[[ "$PACKAGE" == "com.mana.launcher" ]] || {
  echo "ERROR: expected package com.mana.launcher, got: $PACKAGE" >&2
  exit 1
}
MANIFEST="$("$APKANALYZER" manifest print "$APK")"
grep -q 'ManaEngineBridgeService' <<<"$MANIFEST" || {
  echo "ERROR: APK manifest does not declare ManaEngineBridgeService." >&2
  exit 1
}
grep -q 'com.mana.permission.BIND_BRAIN' <<<"$MANIFEST" || {
  echo "ERROR: APK manifest is missing the MANA signature bind permission." >&2
  exit 1
}

mkdir -p "$DEST_DIR"
if [[ -f "$ANDROID_BP" && ! -f "$ANDROID_BP.mana-source-backup" ]]; then
  cp "$ANDROID_BP" "$ANDROID_BP.mana-source-backup"
fi
install -m 0644 "$APK" "$DEST_APK"
TMP_BP="$ANDROID_BP.tmp"
cat > "$TMP_BP" <<'SOONG'
android_app_import {
    name: "ManaLauncher",
    apk: "prebuilts/ManaLauncher.apk",
    presigned: false,
    certificate: "platform",
    privileged: true,
    product_specific: true,
    dex_preopt: {
        enabled: false,
    },
}
SOONG
mv "$TMP_BP" "$ANDROID_BP"

echo "MANA app staged for Soong platform signing."
echo "Module: $MODULE_REL"
echo "APK: $DEST_APK"
echo "SHA-256: $(sha256sum "$DEST_APK" | awk '{print $1}')"
echo "Next: include vendor/mana/genesis/system/mana/mana_product.mk in the verified device product, then build and inspect the signed APK certificate."
