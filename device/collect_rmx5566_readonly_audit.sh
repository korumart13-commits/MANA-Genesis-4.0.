#!/data/data/com.termux/files/usr/bin/bash
# MANA RMX5566 read-only audit v3. Metadata only; no root or partition reads.
set -u

STAMP="$(date +%Y%m%d_%H%M%S 2>/dev/null || echo unknown_time)"
WORK="${HOME}/mana_rmx5566_audit_${STAMP}"
mkdir -p "$WORK" || { echo "Cannot create audit folder"; exit 1; }

redact() {
  sed -E \
    -e 's/(serial(no)?|android_id|imei|meid|imsi)[=: ]+[A-Za-z0-9._-]+/\1=[REDACTED]/Ig' \
    -e 's/[0-9]{14,17}/[REDACTED_NUMBER]/g'
}
capture() {
  local name="$1"; shift
  { echo "### $(date -Is 2>/dev/null || date)"; "$@" 2>&1 || true; } | redact > "$WORK/$name"
}
capture_shell() {
  local name="$1"; shift
  { echo "### $(date -Is 2>/dev/null || date)"; sh -c "$*" 2>&1 || true; } | redact > "$WORK/$name"
}

cat > "$WORK/00_README.txt" <<'EOF'
MANA RMX5566 READ-ONLY AUDIT v3
Metadata only. No root, bootloader unlock, partition content reads, writes,
mounts, flashing, or setting changes. Access-denied results are acceptable.
Review the archive before sharing and redact any unexpected personal data.
EOF

# Whitelisted non-personal build/device properties only.
{
  for key in \
    ro.product.manufacturer ro.product.model ro.product.device ro.product.name \
    ro.product.board ro.board.platform ro.soc.model ro.hardware ro.product.cpu.abi \
    ro.build.version.release ro.build.version.sdk ro.build.version.security_patch \
    ro.build.display.id ro.build.version.incremental ro.build.fingerprint \
    ro.vendor.build.fingerprint ro.vendor.build.security_patch ro.vendor.api_level \
    ro.vndk.version ro.product.first_api_level ro.boot.hardware ro.boot.bootdevice \
    ro.boot.verifiedbootstate ro.boot.flash.locked ro.boot.vbmeta.device_state \
    ro.boot.vbmeta.avb_version ro.boot.avb_version ro.boot.slot_suffix \
    ro.boot.dynamic_partitions ro.boot.super_partition ro.crypto.state ro.crypto.type
  do
    value="$(getprop "$key" 2>/dev/null || true)"
    [ -n "$value" ] && printf '%s=%s\n' "$key" "$value"
  done
} | redact > "$WORK/01_build_properties.txt"

capture_shell "02_kernel_and_os.txt" 'uname -a; cat /proc/version 2>/dev/null'
capture_shell "03_kernel_cmdline.txt" 'cat /proc/cmdline 2>/dev/null'
capture_shell "04_partition_links_metadata.txt" '
for d in /dev/block/by-name /dev/block/bootdevice/by-name /dev/block/mapper /dev/block/platform; do
  if [ -e "$d" ]; then echo "### $d"; ls -la "$d" 2>&1; fi
done
echo "### /sys/class/block"
ls -la /sys/class/block 2>&1
'
capture_shell "05_partition_sizes_sysfs.txt" '
for d in /sys/class/block/*; do
  [ -e "$d" ] || continue
  echo "### ${d##*/}"
  readlink -f "$d" 2>/dev/null
  for f in size ro dev partition; do
    [ -r "$d/$f" ] && { printf "%s=" "$f"; cat "$d/$f" 2>/dev/null; }
  done
done
'
capture_shell "06_mounts_filesystems.txt" 'cat /proc/mounts 2>/dev/null'
capture_shell "07_selinux_state.txt" '
if command -v getenforce >/dev/null 2>&1; then getenforce; else echo "getenforce unavailable"; fi
if [ -r /sys/fs/selinux/enforce ]; then printf "enforce="; cat /sys/fs/selinux/enforce; else echo "/sys/fs/selinux/enforce NOT_READABLE"; fi
if [ -r /sys/fs/selinux/policyvers ]; then printf "policyvers="; cat /sys/fs/selinux/policyvers; else echo "policyvers NOT_READABLE"; fi
'
capture_shell "08_vintf_availability.txt" '
for f in /vendor/etc/vintf/manifest.xml /vendor/etc/vintf/manifest \
 /vendor/etc/vintf/compatibility_matrix.xml /vendor/etc/vintf/manifest/*.xml \
 /odm/etc/vintf/manifest.xml /odm/etc/vintf/manifest/*.xml \
 /system/etc/vintf/manifest.xml /system/etc/vintf/compatibility_matrix.xml \
 /system_ext/etc/vintf/manifest.xml /system_ext/etc/vintf/compatibility_matrix.xml; do
  [ -e "$f" ] || continue
  if [ -r "$f" ]; then echo "READABLE: $f"; else echo "NOT_READABLE: $f"; fi
done
'
# Copy only readable VINTF XML, preserving relative paths; no other vendor files.
for base in /vendor/etc/vintf /odm/etc/vintf /system/etc/vintf /system_ext/etc/vintf; do
  [ -d "$base" ] || continue
  find "$base" -maxdepth 2 -type f -name '*.xml' 2>/dev/null | while IFS= read -r f; do
    [ -r "$f" ] || continue
    rel="${f#/}"
    mkdir -p "$WORK/vintf/$(dirname "$rel")"
    redact < "$f" > "$WORK/vintf/$rel"
  done
done
capture_shell "09_kernel_config_availability.txt" '
if [ -r /proc/config.gz ]; then
 echo "/proc/config.gz readable"
 gzip -dc /proc/config.gz 2>/dev/null | grep -E "^(CONFIG_ARM64|CONFIG_MODULES|CONFIG_MODVERSIONS|CONFIG_LOCALVERSION|CONFIG_IKCONFIG)="
else echo "/proc/config.gz NOT_READABLE"; fi
[ -r /sys/kernel/kheaders.tar.xz ] && echo "kernel headers archive readable" || echo "kernel headers archive NOT_READABLE"
'
capture_shell "10_device_tree_metadata.txt" '
for f in /proc/device-tree/model /proc/device-tree/compatible /sys/firmware/devicetree/base/model /sys/firmware/devicetree/base/compatible; do
 if [ -r "$f" ]; then echo "### $f"; tr "\\000" "\\n" < "$f" 2>/dev/null; fi
done
'
capture_shell "11_vendor_modules.txt" '
for d in /vendor/lib/modules /vendor_dlkm/lib/modules /odm/lib/modules /odm_dlkm/lib/modules /system/lib/modules /system_dlkm/lib/modules; do
 if [ -d "$d" ]; then echo "### $d"; find "$d" -maxdepth 2 -type f \( -name "*.ko" -o -name "modules.load" -o -name "modules.dep" -o -name "modules.alias" \) -print 2>&1; fi
done
'
capture_shell "12_tool_availability.txt" 'for c in getprop uname ls lsmod modinfo readlink find sha256sum zip getenforce; do command -v "$c" 2>/dev/null || echo "$c: not installed"; done'

ZIP="${WORK}.zip"
if command -v zip >/dev/null 2>&1; then
  (cd "$(dirname "$WORK")" && zip -qr "$ZIP" "$(basename "$WORK")")
else
  echo "Install zip with: pkg install zip"
  echo "Audit folder: $WORK"
  exit 2
fi
echo "Audit archive: $ZIP"
echo "Audit folder: $WORK"
echo "Read-only complete. Review archive before sharing."
