#!/usr/bin/env bash
set -Eeuo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${ROOT}/out"
mkdir -p "$OUT"
MODE="${1:-audit}"
PYTHON="$(command -v python3 || true)"

have() { command -v "$1" >/dev/null 2>&1; }
status_dir() { [[ -n "${1:-}" && -d "$1" ]] && printf VERIFIED || printf MISSING; }

case "$MODE" in
  audit)
    if [[ -z "$PYTHON" ]]; then
      echo "ОШИБКА: нужен python3 для безопасного формирования JSON." >&2
      exit 2
    fi
    export MANA_BOOTSTRAP_ROOT="$ROOT" MANA_BOOTSTRAP_OUT="$OUT"
    "$PYTHON" - <<'PY'
import json, os, platform, shutil, subprocess, time
from pathlib import Path
out = Path(os.environ["MANA_BOOTSTRAP_OUT"])
def cmd(args):
    try:
        p = subprocess.run(args, text=True, capture_output=True, timeout=4, check=False)
        return p.stdout.strip() if p.returncode == 0 else None
    except Exception:
        return None
def read(path):
    try: return Path(path).read_text(errors="replace").strip()
    except Exception: return None
props = {}
for key in ("ro.product.model","ro.product.device","ro.product.vendor.device",
            "ro.build.version.release","ro.build.version.sdk","ro.build.fingerprint",
            "ro.boot.slot_suffix","ro.boot.verifiedbootstate","ro.boot.vbmeta.device_state"):
    value = cmd(["getprop", key]) if shutil.which("getprop") else None
    props[key] = {"value": value, "status": "VERIFIED" if value else "UNKNOWN"}
partitions = sorted(p.name for p in Path("/dev/block/by-name").glob("*")) if Path("/dev/block/by-name").exists() else []
report = {
 "schema":"mana.device-audit.v1",
 "generated_utc":time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
 "mode":"read-only",
 "device_target":{"model":"RMX5566RU / RE645F","status":"TARGET_PROFILE_ONLY"},
 "host":{"system":platform.system(),"release":platform.release(),"machine":platform.machine(),
         "python":platform.python_version(),"termux":bool(os.environ.get("TERMUX_VERSION"))},
 "properties":props,
 "kernel":{"release":platform.release(),"cmdline_available":bool(read("/proc/cmdline"))},
 "runtime":{"root_privilege":"NOT_REQUESTED","getprop_available":bool(shutil.which("getprop")),
            "adb_available":bool(shutil.which("adb"))},
 "partitions":{"visible_names":partitions,"status":"VERIFIED" if partitions else "UNKNOWN",
               "note":"Только видимые имена; содержимое разделов не читается."},
 "safety":{"writes_to_partitions":False,"bootloader_changes":False,"dialer_included":False},
 "limitations":["Данные зависят от разрешений Android/Termux.",
                "Отсутствие доступа не означает отсутствие компонента.",
                "Профиль RMX5566RU должен быть повторно подтверждён по фактическим свойствам." ]
}
path=out/"device-audit.json"
path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n")
print("Отчёт:", path)
print("Модель из getprop:", props["ro.product.model"]["value"] or "UNKNOWN")
print("Видимых разделов:", len(partitions))
PY
    ;;
  readiness)
    if [[ "$(uname -s)" != Linux ]]; then
      echo "ROM readiness gate предназначен для Linux; обнаружено: $(uname -s)" >&2
      exit 3
    fi
    if [[ -z "$PYTHON" ]]; then echo "Нужен python3." >&2; exit 2; fi
    export MANA_BOOTSTRAP_OUT="$OUT"
    "$PYTHON" - <<'PY'
import json, os, shutil, subprocess, time
from pathlib import Path
def dircheck(name):
    v=os.environ.get(name)
    return {"path":v,"status":"VERIFIED" if v and Path(v).is_dir() else "MISSING"}
inputs={k:dircheck(k) for k in ("MANA_AOSP_DIR","MANA_DEVICE_TREE","MANA_KERNEL_DIR","MANA_VENDOR_DIR")}
tools={k:shutil.which(k) is not None for k in ("repo","git","python3","make","clang","ninja")}
try:
    disk=shutil.disk_usage(Path.home()).free
except Exception: disk=None
required=[]
for k in ("MANA_AOSP_DIR","MANA_DEVICE_TREE","MANA_KERNEL_DIR","MANA_VENDOR_DIR"):
    v=inputs[k]["path"]
    if not v or not Path(v).is_dir(): continue
    p=Path(v)
    if k=="MANA_AOSP_DIR": required += [str(p/"build/make/core/envsetup.mk"), str(p/".repo")]
    if k=="MANA_DEVICE_TREE": required += [str(p/"AndroidProducts.mk")]
    if k=="MANA_KERNEL_DIR": required += [str(p/"Makefile")]
    if k=="MANA_VENDOR_DIR": required += [str(p/"proprietary") ]
file_checks={p:("VERIFIED" if Path(p).exists() else "MISSING") for p in required}
gates=[v["status"]=="VERIFIED" for v in inputs.values()]
gates += list(tools.values())
gates += [v=="VERIFIED" for v in file_checks.values()]
ready=bool(gates) and all(gates) and disk is not None and disk >= 300*1024**3
report={"schema":"mana.rom-readiness.v1","generated_utc":time.strftime("%Y-%m-%dT%H:%M:%SZ",time.gmtime()),
 "target":"RMX5566RU / RE645F","status":"READY_FOR_MANUAL_REVIEW" if ready else "BLOCKED",
 "inputs":inputs,"tools":tools,"required_paths":file_checks,
 "free_disk_bytes":disk,"disk_gate":"VERIFIED" if disk is not None and disk>=300*1024**3 else "MISSING_OR_INSUFFICIENT",
 "build_started":False,"flash_started":False,
 "blockers":["Readiness is only a preliminary gate; device-tree/vendor/kernel compatibility and licenses need human review.",
             "No OEM source provenance or AVB signing validation is inferred.",
             "This script intentionally does not start a ROM build or flash the phone."]}
p=Path(os.environ["MANA_BOOTSTRAP_OUT"])/"rom-readiness.json"
p.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n")
print("Отчёт:",p)
print("Статус:",report["status"])
PY
    ;;
  *)
    echo "Использование: $0 {audit|readiness}" >&2
    exit 64
    ;;
esac
