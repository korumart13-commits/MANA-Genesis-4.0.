#!/data/data/com.termux/files/usr/bin/bash
set -eu

if ! command -v getprop >/dev/null 2>&1; then
  echo "getprop is unavailable; run this script inside Termux on Android." >&2
  exit 1
fi

out_dir="$HOME/mana_rmx5566_audit"
mkdir -p "$out_dir"
out_file="$out_dir/oem_project_identity.txt"

{
  echo "MANA read-only RMX5566RU identity audit"
  echo "Collected: $(date 2>/dev/null || echo unknown)"
  echo
  for key in \
    ro.product.model ro.product.device ro.product.name \
    ro.product.vendor.device ro.build.fingerprint \
    ro.build.version.release ro.build.version.sdk \
    ro.build.version.incremental ro.board.platform ro.boot.hardware \
    ro.soc.model ro.boot.project_name ro.boot.project \
    ro.product.oplus_model ro.oplus.regionmark ro.separate.soft \
    ro.build.version.oplusrom ro.boot.hardware.sku \
    ro.boot.slot_suffix ro.boot.flash.locked \
    ro.boot.verifiedbootstate ro.boot.vbmeta.device_state;
  do
    printf '%s=' "$key"
    getprop "$key" 2>/dev/null || true
  done
  echo
  echo "=== kernel ==="
  uname -a
  echo
  echo "=== memory ==="
  grep -E 'MemTotal|MemAvailable' /proc/meminfo || true
  echo
  echo "=== storage ==="
  df -h / /data 2>&1 || true
} | tee "$out_file"

echo
echo "Saved read-only audit to: $out_file"
