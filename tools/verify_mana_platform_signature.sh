#!/usr/bin/env bash
# Verify the final AOSP-built MANA APK is signed by the exact platform certificate.
# Usage: tools/verify_mana_platform_signature.sh <AOSP_TOP> <SIGNED_APK> [PLATFORM_CERT]
set -euo pipefail

usage() {
  echo "Usage: $0 <AOSP_TOP> <SIGNED_APK> [PLATFORM_CERT]" >&2
  exit 2
}
[[ $# -ge 2 && $# -le 3 ]] || usage

AOSP_TOP="$(cd "$1" && pwd)"
APK="$(cd "$(dirname "$2")" && pwd)/$(basename "$2")"
CERT_INPUT="${3:-$AOSP_TOP/build/target/product/security/platform.x509.pem}"
CERT="$(cd "$(dirname "$CERT_INPUT")" && pwd)/$(basename "$CERT_INPUT")"
APKSIGNER="${APKSIGNER:-apksigner}"
APKANALYZER="${APKANALYZER:-apkanalyzer}"

[[ -f "$APK" ]] || { echo "ERROR: signed APK not found: $APK" >&2; exit 1; }
[[ -f "$CERT" ]] || { echo "ERROR: platform certificate not found: $CERT" >&2; exit 1; }
command -v "$APKSIGNER" >/dev/null 2>&1 || { echo "ERROR: apksigner is required." >&2; exit 1; }
command -v "$APKANALYZER" >/dev/null 2>&1 || { echo "ERROR: apkanalyzer is required." >&2; exit 1; }
command -v openssl >/dev/null 2>&1 || { echo "ERROR: openssl is required." >&2; exit 1; }

PACKAGE="$("$APKANALYZER" manifest application-id "$APK")"
[[ "$PACKAGE" == "com.mana.launcher" ]] || {
  echo "ERROR: expected com.mana.launcher, got $PACKAGE" >&2
  exit 1
}

SIGNER_OUTPUT="$("$APKSIGNER" verify --verbose --print-certs "$APK")"
ACTUAL="$(sed -n 's/.*Signer #1 certificate SHA-256 digest: *//p' <<<"$SIGNER_OUTPUT" | head -n 1 | tr '[:upper:]' '[:lower:]')"
[[ "$ACTUAL" =~ ^[0-9a-f]{64}$ ]] || {
  echo "ERROR: apksigner did not report a valid signer SHA-256 digest." >&2
  exit 1
}
EXPECTED="$(openssl x509 -in "$CERT" -outform DER | sha256sum | awk '{print $1}')"
[[ "$ACTUAL" == "$EXPECTED" ]] || {
  echo "ERROR: MANA APK signer does not match the supplied AOSP platform certificate." >&2
  echo "Expected: $EXPECTED" >&2
  echo "Actual:   $ACTUAL" >&2
  exit 1
}

echo "MANA APK signature verified."
echo "Package: $PACKAGE"
echo "Platform certificate SHA-256: $ACTUAL"
