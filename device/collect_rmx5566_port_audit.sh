#!/data/data/com.termux/files/usr/bin/bash
# Read-only RMX5566RU port audit. No root, no partition writes, no flashing.
set -u

if ! command -v getprop >/dev/null 2>&1; then
  echo "Run this script in Termux on the target Android phone." >&2
  exit 1
fi

STAMP="$(date -u +%Y%m%dT%H%M%SZ 2>/dev/null || echo unknown)"
OUT="$HOME/mana_rmx5566_port_audit_$STAMP"
mkdir -p "$OUT"/{vintf,partition_links,system_info}
REPORT="$OUT/PORT_AUDIT.txt"

{
  echo "MANA RMX5566RU read-only port audit"
  echo "UTC: $STAMP"
  echo "No root requested. No block devices opened for writing."
  echo
  echo "=== Exact identity / build ==="
  for key in \
    ro.product.model ro.product.device ro.product.name \
    ro.product.vendor.device ro.product.vendor.name \
    ro.product.system.device ro.product.system.name \
    ro.build.fingerprint ro.system.build.fingerprint \
    ro.vendor.build.fingerprint ro.odm.build.fingerprint \
    ro.build.version.release ro.build.version.sdk \
    ro.build.version.incremental ro.build.version.security_patch \
    ro.board.platform ro.boot.hardware ro.soc.model \
    ro.boot.project_name ro.boot.project ro.product.oplus_model \
    ro.boot.hardware.sku ro.boot.slot_suffix \
    ro.boot.flash.locked ro.boot.verifiedbootstate \
    ro.boot.vbmeta.device_state ro.boot.avb_version \
    ro.build.ab_update ro.virtual_ab.enabled ro.boot.dynamic_partitions;
  do
    printf '%s=' "$key"
    getprop "$key" 2>/dev/null || true
  done
  echo
  echo "=== Kernel / architecture ==="
  uname -a
  getconf LONG_BIT 2>/dev/null || true
  echo
  echo "=== Memory / storage ==="
  grep -E 'MemTotal|MemAvailable|SwapTotal|SwapFree' /proc/meminfo 2>/dev/null || true
  df -h / /data /vendor /product /odm 2>&1 || true
  echo
  echo "=== Security state ==="
  printf 'SELinux='
  getenforce 2>/dev/null || cat /sys/fs/selinux/enforce 2>/dev/null || echo unknown
  echo
  echo "=== Readable partition symlink names (no raw reads) ==="
  ls -la /dev/block/by-name 2>&1 || true
  ls -la /dev/block/bootdevice/by-name 2>&1 || true
  ls -la /dev/block/mapper 2>&1 || true
} > "$REPORT"

# Collect VINTF declarations only when readable; these are configuration files,
# not proprietary firmware binaries.
for dir in /vendor/etc/vintf /odm/etc/vintf /system/etc/vintf /system_ext/etc/vintf /product/etc/vintf; do
  if [ -d "$dir" ]; then
    find "$dir" -maxdepth 2 -type f \( -name '*.xml' -o -name '*.manifest' \) -print 2>/dev/null |
    while IFS= read -r file; do
      rel="${file#/}"
      mkdir -p "$OUT/vintf/$(dirname "$rel")"
      cp "$file" "$OUT/vintf/$rel" 2>/dev/null || true
    done
  fi
done

# Record service/HAL names and installed system package names without collecting
# account data, phone numbers, serial numbers, IMEIs, MAC addresses or app data.
service list > "$OUT/system_info/binder_services.txt" 2>&1 || true
dumpsys -l > "$OUT/system_info/dumpsys_services.txt" 2>&1 || true
pm list packages -s > "$OUT/system_info/system_packages.txt" 2>&1 || true
if command -v lshal >/dev/null 2>&1; then
  lshal > "$OUT/system_info/hidl_services.txt" 2>&1 || true
fi
if [ -r /proc/config.gz ]; then
  gzip -dc /proc/config.gz 2>/dev/null | grep -E '^(CONFIG_ANDROID|CONFIG_ARM64|CONFIG_MODULES|CONFIG_DM_|CONFIG_SECURITY_SELINUX|CONFIG_BLK_DEV_DM)=' > "$OUT/system_info/kernel_config_summary.txt" || true
fi

{
  echo
  echo "=== Collected file inventory ==="
  find "$OUT" -type f -printf '%P\n' 2>/dev/null | sort || find "$OUT" -type f 2>/dev/null
  echo
  echo "Audit directory: $OUT"
  echo "No partition contents were dumped. No device settings were changed."
} >> "$REPORT"

if command -v tar >/dev/null 2>&1; then
  tar -czf "$OUT.tar.gz" -C "$(dirname "$OUT")" "$(basename "$OUT")" 2>/dev/null || true
fi

echo "Audit complete."
echo "Report: $REPORT"
[ -f "$OUT.tar.gz" ] && echo "Archive: $OUT.tar.gz"
echo "This script is read-only with respect to Android partitions and firmware."
