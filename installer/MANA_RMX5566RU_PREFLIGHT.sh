#!/data/data/com.termux/files/usr/bin/bash
# MANA Genesis 4.0 — read-only firmware preflight for realme RMX5566RU.
# This script NEVER writes partitions, changes boot state, unlocks the bootloader,
# disables Verified Boot, requests root, or installs firmware.

set -u

OUT="${HOME}/MANA_RMX5566RU_PREFLIGHT_$(date +%Y%m%d_%H%M%S).txt"
EXPECTED_MODEL="RMX5566RU"
EXPECTED_DEVICE="RE645F"
EXPECTED_ANDROID_API="36"
EXPECTED_PLATFORM="bengal"

prop() {
  getprop "$1" 2>/dev/null | tr -d '\r'
}

{
  echo "MANA GENESIS 4.0 — READ-ONLY FIRMWARE PREFLIGHT"
  echo "Generated: $(date -Iseconds 2>/dev/null || date)"
  echo
  echo "[IDENTITY]"
  for key in \
    ro.product.model ro.product.device ro.product.name \
    ro.product.vendor.device ro.build.fingerprint \
    ro.build.version.release ro.build.version.sdk \
    ro.build.version.incremental ro.board.platform \
    ro.boot.hardware ro.soc.model ro.boot.project_name \
    ro.boot.project ro.oplus.regionmark ro.separate.soft \
    ro.build.version.oplusrom; do
    printf '%s=%s\n' "$key" "$(prop "$key")"
  done
  echo
  echo "[BOOT AND SECURITY]"
  for key in ro.boot.flash.locked ro.boot.verifiedbootstate \
    ro.boot.vbmeta.device_state ro.boot.slot_suffix \
    ro.build.type ro.build.tags; do
    printf '%s=%s\n' "$key" "$(prop "$key")"
  done
  echo
  echo "[ABI]"
  printf 'primary_abi=%s\n' "$(prop ro.product.cpu.abi)"
  printf 'abilist=%s\n' "$(prop ro.product.cpu.abilist)"
  echo
  echo "[GATE RESULTS]"

  MODEL="$(prop ro.product.model)"
  DEVICE="$(prop ro.product.device)"
  API="$(prop ro.build.version.sdk)"
  PLATFORM="$(prop ro.board.platform)"
  LOCKED="$(prop ro.boot.flash.locked)"
  VBOOT="$(prop ro.boot.verifiedbootstate)"

  HOLD=0
  if [ "$MODEL" != "$EXPECTED_MODEL" ]; then
    echo "HOLD: model is not the expected RMX5566RU."
    HOLD=1
  else
    echo "PASS: model matches RMX5566RU."
  fi
  if [ "$DEVICE" != "$EXPECTED_DEVICE" ]; then
    echo "HOLD: product/device is not the expected RE645F."
    HOLD=1
  else
    echo "PASS: device matches RE645F."
  fi
  if [ "$API" != "$EXPECTED_ANDROID_API" ]; then
    echo "HOLD: Android API is not 36 (Android 16)."
    HOLD=1
  else
    echo "PASS: Android API 36."
  fi
  if [ "$PLATFORM" != "$EXPECTED_PLATFORM" ]; then
    echo "HOLD: board platform is not bengal."
    HOLD=1
  else
    echo "PASS: board platform bengal."
  fi
  if [ "$LOCKED" = "1" ]; then
    echo "HOLD: bootloader reports locked; no system image installation is authorized by this preflight."
    HOLD=1
  elif [ -z "$LOCKED" ]; then
    echo "HOLD: bootloader state unavailable."
    HOLD=1
  else
    echo "INFO: bootloader does not report locked; this alone does not establish flash readiness."
  fi
  if [ "$VBOOT" = "green" ]; then
    echo "INFO: Verified Boot reports green; preserve the verified boot chain."
  else
    echo "HOLD: Verified Boot state is not green or is unavailable."
    HOLD=1
  fi

  echo
  echo "[FINAL]"
  if [ "$HOLD" -eq 0 ]; then
    echo "PREFLIGHT_ONLY: identity checks passed. This is NOT proof that a ROM is flashable."
  else
    echo "INSTALLATION BLOCKED: one or more compatibility/security gates are unresolved."
  fi
  echo "No partition was read or written. No firmware was installed."
  echo "A real ROM still requires exact matching device tree, kernel, vendor blobs/HALs,"
  echo "VINTF, partition metadata, AVB signing, stock restore images, and hardware tests."
} | tee "$OUT"

echo
echo "Report saved to: $OUT"
