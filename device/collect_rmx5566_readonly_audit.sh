#!/data/data/com.termux/files/usr/bin/bash
# Read-only RMX5566 audit. No root, unlock, writes to partitions, or flashing.
set -u

OUT="${HOME}/rmx5566_readonly_audit.txt"
if [ -d "${HOME}/storage/shared/Download" ]; then
  OUT="${HOME}/storage/shared/Download/rmx5566_readonly_audit.txt"
fi

{
  echo "MANA Genesis 4.0 — RMX5566 read-only audit"
  echo "Generated: $(date -Iseconds 2>/dev/null || date)"
  echo
  echo "== Approved non-personal build/device properties =="
  for key in \
    ro.product.model ro.product.device ro.product.name ro.product.board \
    ro.board.platform ro.soc.model ro.hardware ro.product.cpu.abi \
    ro.build.version.release ro.build.version.sdk ro.build.version.security_patch \
    ro.build.display.id ro.build.version.incremental ro.build.fingerprint \
    ro.vendor.build.security_patch ro.vendor.api_level ro.vndk.version \
    ro.product.first_api_level ro.boot.hardware ro.boot.bootdevice \
    ro.boot.verifiedbootstate ro.boot.vbmeta.device_state ro.boot.flash.locked \
    ro.boot.avb_version ro.crypto.state ro.crypto.type
  do
    value="$(getprop "$key" 2>/dev/null || true)"
    [ -n "$value" ] && printf '%s=%s\n' "$key" "$value"
  done

  echo
  echo "== Kernel =="
  uname -a 2>/dev/null || true
  cat /proc/version 2>/dev/null || true

  echo
  echo "== Partition block inventory (read-only) =="
  cat /proc/partitions 2>/dev/null || echo "NOT_READABLE"
  echo
  echo "-- /dev/block/by-name --"
  ls -la /dev/block/by-name 2>/dev/null || echo "NOT_READABLE"
  echo
  echo "-- /dev/block/bootdevice/by-name --"
  ls -la /dev/block/bootdevice/by-name 2>/dev/null || echo "NOT_READABLE"

  echo
  echo "== SELinux enforcement =="
  if [ -r /sys/fs/selinux/enforce ]; then
    cat /sys/fs/selinux/enforce
  else
    echo "NOT_READABLE"
  fi

  echo
  echo "== VINTF file availability =="
  for file in \
    /vendor/etc/vintf/manifest.xml \
    /vendor/etc/vintf/manifest \
    /vendor/etc/vintf/compatibility_matrix.xml \
    /system/etc/vintf/manifest.xml \
    /system/etc/vintf/compatibility_matrix.xml \
    /odm/etc/vintf/manifest.xml
  do
    if [ -r "$file" ]; then
      echo "-- $file --"
      cat "$file"
    else
      echo "-- $file: NOT_READABLE_OR_ABSENT --"
    fi
  done

  echo
  echo "== End of report =="
  echo "This report intentionally excludes Android ID, serial, IMEI, MAC, accounts and user data."
} > "$OUT" 2>&1

echo "Готово: $OUT"
echo "Перед отправкой проверь файл и удали любые случайно попавшие личные данные."
