#!/usr/bin/env python3
"""Static contract checks for the MANA modules consumed by Android 16 Soong."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
brain_bp = (root / "engine/mana-brain/Android.bp").read_text()
assistant_bp = (root / "system/mana/apps/ManaAssistant/Android.bp").read_text()
aidl_bp = (root / "system/mana/brain/aidl/Android.bp").read_text()
product = (root / "system/mana/mana_product.mk").read_text()
brain_sources = sorted((root / "engine/mana-brain/src/main/kotlin").rglob("*.kt"))

assert 'name: "mana-brain-core"' in brain_bp
assert 'src/main/kotlin/**/*.kt' in brain_bp
assert brain_sources, "Canonical Kotlin MANA Brain sources are missing"
assert 'kotlin_lang_version: "2"' in brain_bp
for dependency in ('"kotlinx-coroutines-core"', '"gson"'):
    assert dependency in brain_bp, f"Missing Android 16 Soong dependency: {dependency}"
assert '"mana.brain-java"' in assistant_bp
assert '"mana-brain-core"' in assistant_bp
assert 'name: "mana.brain"' in aidl_bp and 'java: {' in aidl_bp
for app in ("ManaLauncher", "ManaAssistant", "ManaSettings", "ManaControlCenter"):
    assert re.search(rf"^\s+{app}\s*$", product, re.M), f"{app} is not installed in the MANA product fragment"
assert "mana.brain-java" not in product, "AIDL Java library is linked, not an installable product package"
assert "mana-brain-core" not in product, "Brain engine is linked, not an installable product package"

tile_manifest = ET.parse(root / "system/mana/apps/ManaControlCenter/AndroidManifest.xml").getroot()
android_uri = "http://schemas.android.com/apk/res/android"
android_ns = "{" + android_uri + "}"
tile = tile_manifest.find("./application/service[@android:permission='android.permission.BIND_QUICK_SETTINGS_TILE']", {"android": android_uri})
assert tile is not None, "MANA Quick Settings tile is not declared with the platform bind permission"
assert "android.service.quicksettings.action.QS_TILE" in [x.attrib.get(android_ns + "name") for x in tile.findall("intent-filter/action")]

print(f"MANA Soong contract valid: {len(brain_sources)} Kotlin engine sources, AIDL client/server wiring, 4 product apps, Quick Settings integration.")
