#!/usr/bin/env python3
"""Validate signed MANA-layer update manifests without impersonating OEM OTA.

This tool validates release metadata only. It does not install packages, write
partitions, change Android build properties, or bypass AVB/SELinux.
Requires: python -m pip install cryptography
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlparse

from cryptography.exceptions import InvalidSignature
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey

CHANNEL = "com.mana.genesis.system"
ALLOWED_ARTIFACT_KINDS = {"companion_apk", "module_bundle", "model_bundle"}
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


class ManifestError(ValueError):
    pass


def canonical_payload(manifest: dict) -> bytes:
    unsigned = dict(manifest)
    unsigned.pop("signature", None)
    return json.dumps(
        unsigned, sort_keys=True, separators=(",", ":"), ensure_ascii=False
    ).encode("utf-8")


def load_public_key(path: Path) -> Ed25519PublicKey:
    key = serialization.load_pem_public_key(path.read_bytes())
    if not isinstance(key, Ed25519PublicKey):
        raise ManifestError("MANA release key must be Ed25519")
    return key


def validate_manifest(manifest: dict, public_key: Ed25519PublicKey,
                      expected_fingerprint: str | None = None,
                      installed_version_code: int | None = None) -> None:
    if not isinstance(manifest, dict):
        raise ManifestError("Manifest root must be an object")
    required = {
        "schema_version", "channel", "release_id", "version_code",
        "version_name", "target", "base", "artifacts", "signature"
    }
    missing = required - manifest.keys()
    if missing:
        raise ManifestError("Missing fields: " + ", ".join(sorted(missing)))
    if manifest["schema_version"] != 1:
        raise ManifestError("Unsupported manifest schema")
    if manifest["channel"] != CHANNEL:
        raise ManifestError("Wrong update channel; OEM and MANA channels are separate")
    if not isinstance(manifest["version_code"], int) or manifest["version_code"] < 1:
        raise ManifestError("version_code must be a positive integer")
    if manifest["version_name"] != f"MANA +{manifest['version_code']}":
        raise ManifestError("version_name must match the MANA channel version_code")
    if installed_version_code is not None and manifest["version_code"] <= installed_version_code:
        raise ManifestError("MANA update is not newer than the installed MANA channel version")
    if not isinstance(manifest["release_id"], str) or not manifest["release_id"].strip():
        raise ManifestError("release_id must be non-empty")

    target = manifest["target"]
    base = manifest["base"]
    if not isinstance(target, dict) or target.get("device") != "RE645F":
        raise ManifestError("Target device must be the declared RE645F")
    if target.get("model") != "RMX5566RU":
        raise ManifestError("Target model must be RMX5566RU")
    if not isinstance(base, dict) or base.get("android_api") != 36:
        raise ManifestError("This release line requires Android API 36")
    fingerprint = base.get("build_fingerprint")
    if not isinstance(fingerprint, str) or not fingerprint:
        raise ManifestError("Exact base build fingerprint is required")
    if expected_fingerprint and fingerprint != expected_fingerprint:
        raise ManifestError("Base fingerprint does not match this device build")

    artifacts = manifest["artifacts"]
    if not isinstance(artifacts, list) or not artifacts:
        raise ManifestError("At least one MANA-owned artifact is required")
    for index, artifact in enumerate(artifacts):
        if not isinstance(artifact, dict):
            raise ManifestError(f"Artifact {index} must be an object")
        if artifact.get("kind") not in ALLOWED_ARTIFACT_KINDS:
            raise ManifestError(f"Artifact {index} is not a MANA userspace artifact")
        url = urlparse(str(artifact.get("url", "")))
        if url.scheme != "https" or not url.netloc:
            raise ManifestError(f"Artifact {index} must use an HTTPS URL")
        if not isinstance(artifact.get("size"), int) or artifact["size"] <= 0:
            raise ManifestError(f"Artifact {index} must declare a positive size")
        if not SHA256_RE.fullmatch(str(artifact.get("sha256", ""))):
            raise ManifestError(f"Artifact {index} must declare a lowercase SHA-256")

    signature = manifest["signature"]
    if not isinstance(signature, dict) or signature.get("algorithm") != "Ed25519":
        raise ManifestError("A detached Ed25519 manifest signature is required")
    try:
        signature_bytes = bytes.fromhex(signature.get("value_hex", ""))
        public_key.verify(signature_bytes, canonical_payload(manifest))
    except (ValueError, InvalidSignature) as exc:
        raise ManifestError("MANA manifest signature verification failed") from exc


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("--public-key", required=True, type=Path)
    parser.add_argument("--expected-fingerprint")
    parser.add_argument("--installed-version-code", type=int, help="Current MANA channel version; not the Realme/Android version")
    args = parser.parse_args()
    try:
        manifest = json.loads(args.manifest.read_text(encoding="utf-8"))
        validate_manifest(
            manifest, load_public_key(args.public_key), args.expected_fingerprint,
            args.installed_version_code
        )
    except (OSError, json.JSONDecodeError, ManifestError) as exc:
        print(f"MANA UPDATE REJECTED: {exc}", file=sys.stderr)
        return 2
    print(
        f"MANA UPDATE METADATA VERIFIED: {manifest['version_name']} "
        f"({manifest['release_id']}); target={manifest['target']['device']}. "
        "This is not an OEM OTA or a firmware-installation authorization."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
