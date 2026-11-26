#!/usr/bin/env python3
"""Static contract checks for the MANA OS firmware-only Android 16 product."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
product = (root / "system/mana/mana_product.mk").read_text()
apps = {
    "ManaLauncher": "com.mana.launcher",
    "ManaSettings": "com.mana.settings",
    "ManaControlCenter": "com.mana.controlcenter",
}
expected = set(apps)
declared = {
    token
    for line in product.splitlines()
    for token in line.replace("\\", " ").split()
    if token.startswith("Mana")
}
assert declared == expected, f"Firmware product must contain exactly {sorted(expected)}, got {sorted(declared)}"
assert "ManaAssistant" not in product, "Standalone AI bridge prototype must not be embedded in the firmware"
assert "mana-brain-core" not in product and "mana.brain-java" not in product, "MANA AI runtime belongs to the separate app"

android_uri = "http://schemas.android.com/apk/res/android"
android_ns = "{" + android_uri + "}"
for module, package in apps.items():
    folder = root / "system/mana/apps" / module
    bp = (folder / "Android.bp").read_text()
    manifest = ET.parse(folder / "AndroidManifest.xml").getroot()
    assert f'name: "{module}"' in bp
    assert "privileged: false" in bp, f"{module} must not receive unreviewed privileged status"
    assert manifest.attrib.get("package") == package
    for activity in manifest.findall("./application/activity"):
        assert activity.attrib.get(android_ns + "exported") in ("true", "false"), f"Missing exported attribute in {module}"

launcher_manifest = ET.parse(root / "system/mana/apps/ManaLauncher/AndroidManifest.xml").getroot()
launcher_filters = launcher_manifest.findall("./application/activity/intent-filter")
assert any(
    "android.intent.action.MAIN" in [n.attrib.get(android_ns + "name") for n in f.findall("action")]
    and "android.intent.category.HOME" in [n.attrib.get(android_ns + "name") for n in f.findall("category")]
    for f in launcher_filters
), "MANA Launcher must expose the Android HOME role"
launcher_queries = launcher_manifest.findall("./queries/intent")
assert any(
    "android.intent.action.MAIN" in [n.attrib.get(android_ns + "name") for n in q.findall("action")]
    and "android.intent.category.LAUNCHER" in [n.attrib.get(android_ns + "name") for n in q.findall("category")]
    for q in launcher_queries
), "Launcher must be able to enumerate launchable apps"

launcher_source = (root / "system/mana/apps/ManaLauncher/src/com/mana/launcher/ManaHomeActivity.java").read_text()
assert "collectApplications()" in launcher_source
assert "queryIntentActivities" in launcher_source
assert "getApplicationIcon" in launcher_source
assert "appGrid.addView(appTile(app)" in launcher_source
assert "Intent.CATEGORY_HOME" not in launcher_source or "CATEGORY_HOME" in launcher_source

control_manifest = ET.parse(root / "system/mana/apps/ManaControlCenter/AndroidManifest.xml").getroot()
tile = control_manifest.find("./application/service[@android:permission='android.permission.BIND_QUICK_SETTINGS_TILE']", {"android": android_uri})
assert tile is not None, "Quick Settings tile must use the platform bind permission"
assert "android.service.quicksettings.action.QS_TILE" in [
    node.attrib.get(android_ns + "name") for node in tile.findall("intent-filter/action")
]

# The AI/Binder prototype is retained as source for later, separately reviewed integration.
# It must not be installed as part of this firmware product.
assistant_bp = (root / "system/mana/apps/ManaAssistant/Android.bp").read_text()
aidl_bp = (root / "system/mana/brain/aidl/Android.bp").read_text()
assert 'name: "ManaAssistant"' in assistant_bp
assert 'name: "mana.brain"' in aidl_bp
assert (root / "system/mana/apps/ManaAssistant/src/com/mana/assistant/ManaBrainBinderService.java").is_file()
assert (root / "system/mana/device-bridge/README_RU.md").is_file()

print("MANA OS firmware product contract valid: 3 non-privileged UI apps, HOME launcher, app enumeration, Quick Settings tile; AI runtime remains outside firmware.")
