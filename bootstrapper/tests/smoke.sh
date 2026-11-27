#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
bash -n "$ROOT/bootstrap.sh"
python3 -m json.tool "$ROOT/device/rmx5566ru.target.json" >/dev/null
grep -q '"no_builtin_dialer": true' "$ROOT/device/rmx5566ru.target.json"
grep -q 'build_started.*False' "$ROOT/bootstrap.sh"
echo "PASS: shell syntax, target JSON, no-dialer policy, build safety gate"
