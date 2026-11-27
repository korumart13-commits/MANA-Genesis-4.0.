#!/usr/bin/env bash
# Verify MANA APK signer certificates against the exact product platform certificate.
# Usage: tools/verify_mana_platform_signature.sh <AOSP_TOP> <SIGNED_LAUNCHER_APK> [PLATFORM_CERT] [SIGNED_BRAIN_APK]
set -euo pipefail

usage() {
  echo "Usage: $0 <AOSP_TOP> <SIGNED_LAUNCHER_APK> [PLATFORM_CERT] [SIGNED_BRAIN_APK]" >&2
  exit 2
}
[[ $# -ge 2 && $# -le 4 ]] || usage

AOSP_TOP="$(cd "$1" && pwd)"
LAUNCHER_APK="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
CERT_INPUT="${3:-$AOSP_TOP/build/target/product/security/platform.x509.pem}"
CERT="$(cd "$(dirname "$CERT_INPUT")" && pwd)/$(basename "$CERT_INPUT")"
BRAIN_APK=""
if [[ $# -eq 4 ]]; then
  BRAIN_APK="$(cd "$(dirname "$4")" && pwd)/$(basename "$4")"
fi
APKSIGNER="${APKSIGNER:-apksigner}"
APKANALYZER="${APKANALYZER:-apkanalyzer}"

[[ -f "$LAUNCHER_APK" ]] || { echo "ERROR: signed launcher APK not found: $LAUNCHER_APK" >&2; exit 1; }
[[ -f "$CERT" ]] || { echo "ERROR: platform certificate not found: $CERT" >&2; exit 1; }
if [[ -n "$BRAIN_APK" && ! -f "$BRAIN_APK" ]]; then
  echo "ERROR: signed Brain service APK not found: $BRAIN_APK" >&2
  exit 1
fi
command -v "$APKSIGNER" >/dev/null 2>&1 || { echo "ERROR: apksigner is required." >&2; exit 1; }
command -v "$APKANALYZER" >/dev/null 2>&1 || { echo "ERROR: apkanalyzer is required." >&2; exit 1; }
command -v openssl >/dev/null 2>&1 || { echo "ERROR: openssl is required." >&2; exit 1; }

EXPECTED="$(openssl x509 -in "$CERT" -outform DER | sha256sum | awk '{print $1}')"

verify_apk() {
  local apk="$1"
  local expected_package="$2"
  local label="$3"
  local package signer_output actual

  package="$("$APKANALYZER" manifest application-id "$apk")"
  [[ "$package" == "$expected_package" ]] || {
    echo "ERROR: expected $expected_package, got $package in $apk" >&2
    exit 1
  }

  signer_output="$("$APKSIGNER" verify --verbose --print-certs "$apk")"
  actual="$(sed -n 's/.*Signer #1 certificate SHA-256 digest: *//p' <<<"$signer_output" | head -n 1 | tr '[:upper:]' '[:lower:]')"
  [[ "$actual" =~ ^[0-9a-f]{64}$ ]] || {
    echo "ERROR: apksigner did not report a valid signer SHA-256 digest for $label." >&2
    exit 1
  }
  [[ "$actual" == "$EXPECTED" ]] || {
    echo "ERROR: $label signer does not match the supplied AOSP platform certificate." >&2
    echo "Expected: $EXPECTED" >&2
    echo "Actual:   $actual" >&2
    exit 1
  }
  echo "$label signature verified."
  echo "Package: $package"
  echo "Platform certificate SHA-256: $actual"
}

verify_apk "$LAUNCHER_APK" "com.mana.launcher" "MANA Launcher"
if [[ -n "$BRAIN_APK" ]]; then
  verify_apk "$BRAIN_APK" "com.mana.system.brain" "MANA Brain Service"
fi
