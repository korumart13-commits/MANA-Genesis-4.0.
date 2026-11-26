#!/usr/bin/env bash
set -Eeuo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
LOCK="$ROOT/device/realme/RE645F/oem-source-lock.json"
python3 - "$LOCK" <<'PY'
import json, pathlib, sys
d=json.loads(pathlib.Path(sys.argv[1]).read_text())
t=d["target"]
print("Target:", t["product"], t["model"], t["device"])
print("Runtime kernel:", t["runtime_kernel"])
for s in d["sources"]:
    print(f"Candidate: {s['name']} @ {s['revision']} — {s['compatibility']}")
    if "known_kernel_release" in s:
        print("Candidate kernel:", s["known_kernel_release"], "| target:", s["target_kernel_release"])
        if s["known_kernel_release"] != s["target_kernel_release"]:
            print("BLOCKED: kernel release mismatch")
policy=d["build_policy"]
blocked=[k for k,v in policy.items() if k.startswith("allow_") and v is False]
print("Image generation gates:", ", ".join(blocked))
print("ROM_READY=false (until exact device sources, blobs, partition/AVB/recovery validation and boot test exist)")
PY
