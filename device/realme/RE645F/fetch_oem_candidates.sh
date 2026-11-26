#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
LOCK="$ROOT/device/realme/RE645F/oem-source-lock.json"
MODE="${1:---all}"
command -v git >/dev/null || { echo "git is required" >&2; exit 2; }
command -v python3 >/dev/null || { echo "python3 is required" >&2; exit 2; }
[[ -f "$LOCK" ]] || { echo "Missing source lock: $LOCK" >&2; exit 2; }

python3 - "$LOCK" "$ROOT" "$MODE" <<'PY'
import json, pathlib, subprocess, sys
lock_path, root, mode = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2]), sys.argv[3]
data = json.loads(lock_path.read_text())
sources = data["sources"]
if mode not in ("--all", "--kernel-only", "--vendor-only"):
    raise SystemExit("Usage: fetch_oem_candidates.sh [--all|--kernel-only|--vendor-only]")
selected = [s for s in sources if mode == "--all" or
            (mode == "--kernel-only" and "kernel-source" in s["name"]) or
            (mode == "--vendor-only" and "vendor-source" in s["name"])]
for source in selected:
    dest = root / source["path"]
    expected = source["revision"]
    if dest.exists() and (dest / ".git").exists():
        actual = subprocess.check_output(["git", "-C", str(dest), "rev-parse", "HEAD"], text=True).strip()
        if actual == expected:
            print(f"[OK] {source['name']} already at pinned revision {actual}")
            continue
        raise SystemExit(f"Refusing to overwrite {dest}: existing checkout is {actual}, expected {expected}. Move it aside manually.")
    if dest.exists() and any(dest.iterdir()):
        raise SystemExit(f"Refusing to overwrite non-empty directory: {dest}")
    dest.parent.mkdir(parents=True, exist_ok=True)
    print(f"[FETCH] {source['name']} -> {dest}")
    subprocess.run(["git", "init", str(dest)], check=True)
    subprocess.run(["git", "-C", str(dest), "remote", "add", "origin", source["url"]], check=True)
    # Fetch only the exact audited commit; no floating branch is used.
    subprocess.run(["git", "-C", str(dest), "fetch", "--depth=1", "--filter=blob:none", "origin", expected], check=True)
    subprocess.run(["git", "-C", str(dest), "checkout", "--detach", "FETCH_HEAD"], check=True)
    actual = subprocess.check_output(["git", "-C", str(dest), "rev-parse", "HEAD"], text=True).strip()
    if actual != expected:
        raise SystemExit(f"Commit mismatch for {source['name']}: expected {expected}, got {actual}")
    print(f"[PINNED] {source['name']} {actual}")
print("OEM source candidates fetched. These are research inputs only, not an accepted RMX5566RU kernel/DT port.")
PY
