#!/usr/bin/env bash
set -euo pipefail

SCRIPT="$(cd "$(dirname "$0")/.." && pwd)/MANA_RMX5566RU_PREFLIGHT.sh"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/bin" "$TMP/home"

cat > "$TMP/bin/getprop" <<'MOCK'
#!/usr/bin/env bash
case "$1" in
  ro.product.model) echo "${MOCK_MODEL:-RMX5566RU}" ;;
  ro.product.device) echo "${MOCK_DEVICE:-RE645F}" ;;
  ro.build.version.sdk) echo "${MOCK_API:-36}" ;;
  ro.board.platform) echo "${MOCK_PLATFORM:-bengal}" ;;
  ro.boot.flash.locked) echo "${MOCK_LOCKED:-1}" ;;
  ro.boot.verifiedbootstate) echo "${MOCK_VBOOT:-green}" ;;
  *) echo "" ;;
esac
MOCK
chmod +x "$TMP/bin/getprop"

run_case() {
  local name="$1" expected="$2"
  shift 2
  rm -f "$TMP/home"/MANA_RMX5566RU_PREFLIGHT_*.txt
  (
    export PATH="$TMP/bin:$PATH" HOME="$TMP/home"
    export MOCK_MODEL=RMX5566RU MOCK_DEVICE=RE645F MOCK_API=36
    export MOCK_PLATFORM=bengal MOCK_LOCKED=1 MOCK_VBOOT=green
    env "$@" bash "$SCRIPT" >/dev/null
  )
  local report
  report="$(find "$TMP/home" -name 'MANA_RMX5566RU_PREFLIGHT_*.txt' -print -quit)"
  test -n "$report"
  if grep -q "$expected" "$report"; then
    printf 'PASS: %s\n' "$name"
  else
    printf 'FAIL: %s\n' "$name" >&2
    cat "$report" >&2
    exit 1
  fi
}

run_case "correct identity is detected but locked bootloader blocks installation" "INSTALLATION BLOCKED"
run_case "wrong model is rejected" "model is not the expected RMX5566RU." MOCK_MODEL=RMX5253
run_case "wrong API is rejected" "Android API is not 36" MOCK_API=35
run_case "wrong platform is rejected" "board platform is not bengal" MOCK_PLATFORM=mt6877
run_case "unknown verified boot is rejected" "Verified Boot state is not green" MOCK_VBOOT=orange

echo "All MANA preflight tests passed."
