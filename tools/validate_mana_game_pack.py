#!/usr/bin/env python3
"""Static sanity checks for the MANA XZ Godot companion-world package."""
from pathlib import Path
import hashlib
import json
ROOT = Path(__file__).resolve().parents[1]
required = [
    "project.godot", "scenes/mana_apartment.tscn",
    "scripts/mana_apartment.gd", "scripts/mana_companion.gd",
    "scripts/mana_voice_bridge.gd",
    "assets/characters/mana_avatar_genesis.gltf",
    "assets/characters/mana_avatar_genesis_casual.gltf",
    "assets/characters/mana_avatar_genesis_evening.gltf",
]
for relative in required:
    path = ROOT / relative
    assert path.is_file() and path.stat().st_size > 0, f"Missing/empty game file: {relative}"
companion = (ROOT / "scripts/mana_companion.gd").read_text(encoding="utf-8")
for token in ("mood_interval_seconds", "_choose_target", "_load_outfit", "mood_changed"):
    assert token in companion, f"Autonomy behavior missing: {token}"
scene = (ROOT / "scenes/mana_apartment.tscn").read_text(encoding="utf-8")
assert "res://scripts/mana_apartment.gd" in scene
for name in (
    "mana_avatar_genesis.gltf",
    "mana_avatar_genesis_casual.gltf",
    "mana_avatar_genesis_evening.gltf",
    "mana_avatar_genesis_no_accessory.gltf",
    "mana_avatar_genesis_casual_no_accessory.gltf",
    "mana_avatar_genesis_evening_no_accessory.gltf",
):
    game_asset = ROOT / "assets/characters" / name
    android_asset = ROOT.parents[2] / "launcher/app/src/main/assets" / name
    assert game_asset.is_file() and android_asset.is_file(), f"Missing shared avatar: {name}"
    assert hashlib.sha256(game_asset.read_bytes()).digest() == hashlib.sha256(android_asset.read_bytes()).digest(), f"Game avatar differs from launcher: {name}"
    document = json.loads(game_asset.read_text(encoding="utf-8"))
    assert document.get("asset", {}).get("version") == "2.0", f"Invalid glTF 2.0 asset: {name}"
readme = (ROOT / "README_RU.md").read_text(encoding="utf-8")
assert "ещё не встроен в Android APK" in readme
print("MANA XZ Godot game package: static file/contract checks passed.")
print("This is not a Godot import/export or Android-device test.")
