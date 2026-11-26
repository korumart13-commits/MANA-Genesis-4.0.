#!/usr/bin/env python3
"""Validate the full MANA Genesis 4.0 owner-feature manifest against the OS catalog."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "os" / "MANA_GENESIS_4.0_COMPLETE_FEATURE_MANIFEST.json"
CATALOG = ROOT / "os/core/src/main/kotlin/com/mana/os/core/ManaSystemModuleCatalog.kt"
REQUIRED = {
    "platform.android16", "platform.kernel", "platform.hardware", "platform.security",
    "platform.storage", "mana.systemcore", "mana.brain", "mana.localruntime",
    "mana.modelmanager", "mana.memory", "mana.autodev", "mana.futureupdates", "mana.updater", "mana.recovery",
    "mana.systemui", "mana.launcher", "mana.settings", "mana.controlcenter",
    "mana.stockappbridge", "mana.avatar", "mana.avatar3d", "mana.voice",
    "mana.diary", "mana.lifemanager", "mana.lifestudio", "mana.finance",
    "mana.contacts", "mana.receptionist", "mana.calltelephony",
    "mana.gameintelligence", "mana.gamebridge", "mana.microuniverse",
    "mana.yandex", "mana.vk", "mana.integrationhub", "mana.developercenter",
    "mana.localnode", "mana.deviceecosystem", "mana.kernellab", "mana.owneridentity",\n    "mana.sleep", "mana.camera", "mana.messaging", "mana.lifecontrolcenter",
}


def validate(data: dict, catalog_text: str) -> None:
    if data.get("schema_version") != 1:
        raise ValueError("Unsupported feature manifest schema")
    if data.get("product") != "MANA Genesis 4.0":
        raise ValueError("Wrong product identity")
    modules = data.get("modules")
    if not isinstance(modules, list) or not modules:
        raise ValueError("Manifest modules must be a non-empty list")
    ids = [item.get("id") for item in modules if isinstance(item, dict)]
    if len(ids) != len(modules) or len(ids) != len(set(ids)):
        raise ValueError("Module entries must be objects with unique IDs")
    missing = REQUIRED - set(ids)
    if missing:
        raise ValueError("Missing owner-requested modules: " + ", ".join(sorted(missing)))
    for item in modules:
        if not item.get("name") or item.get("status") not in {
            "prototype", "partial", "architecture", "blocked"
        }:
            raise ValueError(f"Invalid module name/status: {item}")
        if not isinstance(item.get("required"), bool):
            raise ValueError(f"Module required flag must be boolean: {item.get('id')}")
    catalog_ids = set(re.findall(r'SystemModuleDescriptor\("([^"]+)"', catalog_text))
    not_in_manifest = catalog_ids - set(ids)
    if not_in_manifest:
        raise ValueError("Catalog modules missing from feature manifest: " + ", ".join(sorted(not_in_manifest)))
    target = data.get("target", {})
    if target.get("model") != "RMX5566RU" or target.get("device") != "RE645F" or target.get("android_api") != 36:
        raise ValueError("Device target identity mismatch")
    if target.get("kernel_replacement_allowed") is not False:
        raise ValueError("Unverified OEM kernel must remain protected")
    if not target.get("observed_stock_driver_modules") or not target.get("driver_source_status"):
        raise ValueError("Stock driver preservation provenance is incomplete")
    if data.get("first_release_version") != "MANA +1":
        raise ValueError("First MANA release must use the independent MANA +1 label")
    receptionist = data.get("receptionist", {})
    if receptionist.get("auto_answer_delay_seconds") != 10 or not receptionist.get("conversation_summaries") or not receptionist.get("unknown_number_handling"):
        raise ValueError("Smart Receptionist owner settings are incomplete")
    avatar = data.get("avatar_voice", {})
    if avatar.get("identity_mutable") is not False or avatar.get("outfits_and_accessories_mutable") is not True:
        raise ValueError("Avatar identity/outfit invariants are missing")
    autonomy = data.get("autonomy", {})
    if not autonomy.get("offline_local_inference") or not autonomy.get("local_encrypted_memory") or not autonomy.get("self_code_build_and_test"):
        raise ValueError("Local-first and self-development invariants are missing")
    adaptive = data.get("adaptive_intelligence", {})
    if not adaptive.get("source_validation") or not adaptive.get("local_knowledge_base") or not adaptive.get("resource_aware_scheduling"):
        raise ValueError("Adaptive learning requirements are incomplete")
    future = data.get("future_updates", {})
    if future.get("pipeline") != ["idea", "plan", "development", "build", "test", "signed_candidate", "owner_approval", "install"]:
        raise ValueError("Future Updates pipeline is incomplete or out of order")
    security = data.get("security", {})
    if not security.get("selinux_enforcing") or not security.get("verified_boot_preserved"):
        raise ValueError("Android security and Verified Boot invariants are missing")


def main() -> int:
    data = json.loads(MANIFEST.read_text(encoding="utf-8"))
    validate(data, CATALOG.read_text(encoding="utf-8"))
    print(f"Validated {len(data['modules'])} owner-requested modules and all release invariants.")
    print("Manifest status labels describe implementation maturity; they do not claim all modules are packaged.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        print(f"MANA FEATURE MANIFEST REJECTED: {exc}", file=sys.stderr)
        raise SystemExit(2)
