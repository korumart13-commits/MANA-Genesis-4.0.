#!/usr/bin/env python3
"""Build a signed .manapack using an offline ECDSA P-256 private key and OpenSSL."""
import argparse
import base64
import hashlib
import json
import re
import subprocess
import tempfile
import zipfile
from pathlib import Path

ID = re.compile(r"[a-z][a-z0-9_.-]{1,63}\Z")
VERSION = re.compile(r"[A-Za-z0-9][A-Za-z0-9._+-]{0,63}\Z")
EXTENSIONS = {".bin", ".apk", ".jar", ".so", ".dat"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--spec", required=True, type=Path, help="UTF-8 JSON package specification")
    parser.add_argument("--payload-dir", required=True, type=Path)
    parser.add_argument("--private-key", required=True, type=Path, help="Offline PEM EC private key; never commit it")
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    spec = json.loads(args.spec.read_text(encoding="utf-8"))
    pack_id, pack_version = spec["pack_id"], spec["pack_version"]
    device, min_api, max_api = spec["target_device"], int(spec["min_api"]), int(spec["max_api"])
    if not ID.fullmatch(pack_id) or not VERSION.fullmatch(pack_version):
        raise SystemExit("Invalid pack ID or version")
    if not device or min_api < 1 or max_api < min_api:
        raise SystemExit("Invalid target device/API range")
    modules = spec["modules"]
    if not 1 <= len(modules) <= 64:
        raise SystemExit("A pack must contain 1..64 modules")

    prepared = []
    ids = set()
    for module in modules:
        module_id, version = module["id"], module["version"]
        if not ID.fullmatch(module_id) or not VERSION.fullmatch(version) or module_id in ids:
            raise SystemExit("Invalid or duplicate module ID/version")
        ids.add(module_id)
        source_input = args.payload_dir / module["file"]
        if source_input.is_symlink():
            raise SystemExit("Symlink payloads are not allowed")
        source = source_input.resolve()
        root = args.payload_dir.resolve()
        if root not in source.parents or not source.is_file():
            raise SystemExit("Payload must be a regular file inside --payload-dir")
        if source.suffix.lower() not in EXTENSIONS:
            raise SystemExit("Unsupported payload extension")
        deps = sorted(set(module.get("dependencies", [])))
        if any(not ID.fullmatch(dep) or dep == module_id for dep in deps):
            raise SystemExit("Invalid module dependency")
        min_os = module["min_os"]
        if not isinstance(min_os, str) or not VERSION.fullmatch(min_os):
            raise SystemExit("module.min_os is required")
        entry = f"modules/{module_id}/payload{source.suffix.lower()}"
        data = source.read_bytes()
        prepared.append({
            "id": module_id, "version": version, "entry": entry,
            "sha256": hashlib.sha256(data).hexdigest(), "min_os": min_os,
            "dependencies": deps, "data": data,
        })

    prepared.sort(key=lambda item: item["id"])
    if sum(len(item["data"]) for item in prepared) > 4 * 1024 * 1024 * 1024:
        raise SystemExit("Combined payload exceeds the 4 GiB format limit")
    lines = ["MANA-PACK/1", pack_id, pack_version, device, str(min_api), str(max_api)]
    for module in prepared:
        lines.append("|".join([
            module["id"], module["version"], module["entry"], module["sha256"],
            module["min_os"], ",".join(module["dependencies"]),
        ]))
    canonical = "\n".join(lines).encode("utf-8")

    with tempfile.TemporaryDirectory(prefix="manapack-sign-") as temp:
        canonical_file = Path(temp) / "canonical.txt"
        signature_file = Path(temp) / "signature.der"
        canonical_file.write_bytes(canonical)
        subprocess.run([
            "openssl", "dgst", "-sha256", "-sign", str(args.private_key.resolve()),
            "-out", str(signature_file), str(canonical_file),
        ], check=True)
        signature = base64.b64encode(signature_file.read_bytes()).decode("ascii")

    props = [
        ("pack.id", pack_id), ("pack.version", pack_version), ("target.device", device),
        ("target.min_api", str(min_api)), ("target.max_api", str(max_api)),
        ("modules.count", str(len(prepared))),
    ]
    for index, module in enumerate(prepared):
        prefix = f"module.{index}."
        props.extend([
            (prefix + "id", module["id"]), (prefix + "version", module["version"]),
            (prefix + "entry", module["entry"]), (prefix + "sha256", module["sha256"]),
            (prefix + "min_os", module["min_os"]),
            (prefix + "dependencies", ",".join(module["dependencies"])),
        ])
    props.append(("signature", signature))
    manifest = "".join(f"{key}={value}\n" for key, value in props).encode("ascii")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        archive.writestr("META-INF/manapack.properties", manifest)
        for module in prepared:
            archive.writestr(module["entry"], module["data"])
    print(f"Created {args.output} ({args.output.stat().st_size} bytes)")
    print("Verify the output with the matching trusted public key before distribution.")


if __name__ == "__main__":
    main()
