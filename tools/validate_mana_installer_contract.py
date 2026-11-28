#!/usr/bin/env python3
"""Static contract guard for the one-button realme C85 4G MANA installer.

This validates source/documentation wiring only; it is not a device or model-runtime test.
"""
from pathlib import Path
import json
import re
import subprocess

root = Path(__file__).resolve().parents[1]
activity = (root / "launcher/app/src/main/java/com/mana/launcher/MainActivity.kt").read_text()
coordinator = (root / "launcher/app/src/main/java/com/mana/launcher/ManaOneTapSetupCoordinator.kt").read_text()
catalog = (root / "launcher/app/src/main/java/com/mana/launcher/ManaModelCatalog.kt").read_text()
setup_doc = (root / "launcher/LOCAL_MODEL_SETUP.md").read_text()
wallpaper_studio = (root / "launcher/app/src/main/java/com/mana/launcher/ManaWallpaperStudioActivity.kt").read_text()
wallpaper_worker = (root / "launcher/app/src/main/java/com/mana/launcher/ManaWallpaperWorker.kt").read_text()
requirements = (root / "docs/MANA_GENESIS_4_0_NON_NEGOTIABLE_REQUIREMENTS_AND_GAPS.md").read_text()
workflow = (root / ".github/workflows/mana-launcher.yml").read_text()
bootstrap = (root / "platform/bootstrap_aosp16.sh").read_text()
manifest_root = __import__("xml.etree.ElementTree", fromlist=["ElementTree"]).parse(root / "platform/local_manifests/mana.xml").getroot()
package_manifest = json.loads((root / "distribution/Ω/config/mana-package.json").read_text())
module_manifest = json.loads((root / "distribution/Ω/config/modules.json").read_text())
default_settings = json.loads((root / "distribution/Ω/config/default-settings.json").read_text())

for required in (
    '"УСТАНОВИТЬ MANA XZ"',
    '"ПРОДОЛЖИТЬ УСТАНОВКУ MANA XZ"',
    "DownloadManager.ACTION_DOWNLOAD_COMPLETE",
    "registerModelDownloadReceiver()",
    "activateCompletedModelDownload()",
    "startModelProgressUpdates()",
    "startGemmaDownload(catalog)",
    "hasAcknowledgedGemmaLicense()",
):
    assert required in activity, f"Installer UI/flow missing: {required}"

for required in (
    'Build.MANUFACTURER.equals("realme", ignoreCase = true)',
    'Build.MODEL.contains("RMX5566", ignoreCase = true)',
    "Build.VERSION.SDK_INT == 36",
    'it.equals("arm64-v8a", ignoreCase = true)',
):
    assert required in coordinator, f"Target profile guard missing: {required}"
assert "Build.FINGERPRINT.startsWith" not in coordinator
assert "Build.DEVICE.equals" not in coordinator
assert "Build.PRODUCT.equals" not in coordinator

for required in (
    "fun hasAcknowledgedGemmaLicense()",
    "DownloadManager.Request",
    "fun downloadProgressText(): String?",
    ".setAllowedOverMetered(false)",
    "MODEL_SIZE = 2_588_147_712L",
    "SHA256 =",
    "ModelArtifactIntegrity.matches",
):
    assert required in catalog, f"Gemma download/integrity guard missing: {required}"

assert "УСТАНОВИТЬ MANA XZ" in setup_doc
assert "Termux" in requirements
assert "УСТАНОВИТЬ MANA XZ" in requirements
assert "INSTALL_MANA_XZ.apk" in workflow
assert "Engine/Inference/Models/Gemma/MODEL_MANIFEST_RU.txt" in workflow
assert "models/Gemma/MODEL_MANIFEST_RU.txt" not in workflow
assert "SHA256SUMS.txt" in workflow
assert "MANA-Omega-FULL-INSTALLER.zip" in workflow
assert "MANA-Omega-FULL-INSTALLER" in workflow
for wallpaper_source in (wallpaper_studio, wallpaper_worker):
    assert "WallpaperManager.FLAG_LOCK" in wallpaper_source
    assert "FLAG_SYSTEM" not in wallpaper_source
assert "PeriodicWorkRequestBuilder<ManaWallpaperWorker>" in wallpaper_studio
assert "coerceIn(15, 180)" in wallpaper_studio
assert "coerceIn(15, 180)" in wallpaper_worker
for required in (
    "private fun showContactIntelligence()",
    "private fun showSmartReceptionist()",
    "ManaSmartReceptionistPolicy(settings)",
    "Minecraft Companion",
    "Локальный поиск сохранённого контакта",
):
    assert required in activity, f"Module UI missing: {required}"
for required in (
    'PACKAGE="release/offline/Ω"',
    'cp -a "distribution/Ω/." "$PACKAGE/"',
    'cp -a launcher/app/build/generated/manaVoiceAssets/mana_voice/.',
    'cp -a engine/mana-brain',
    'cp -a game/mana-xz-world "$PACKAGE/game/"',
    'cp -a game/minecraft-bedrock "$PACKAGE/game/"',
    'cp -a platform "$PACKAGE/firmware/"',
    'cp -a system/mana "$PACKAGE/firmware/system/"',
    'cp -a device/RMX5566RU "$PACKAGE/firmware/device/"',
    'MANA-Omega-debug.apk',
):
    assert required in workflow, f"Ω package component missing: {required}"
assert "MANA-Martynova-FULL-PACKAGE.zip" not in workflow

print("Validated static one-button MANA XZ installer contract for realme C85 4G / RMX5566.")
print("This does not prove physical-device compatibility or successful Gemma inference.")

assert 'MANA_SOURCE_REF="${MANA_SOURCE_REF:-Final}"' in bootstrap
assert '/${MANA_SOURCE_REF}/platform/local_manifests/mana.xml' in bootstrap
manifest_project = manifest_root.find("project")
assert manifest_project is not None
pinned_revision = manifest_project.attrib.get("revision", "")
assert re.fullmatch(r"[0-9a-f]{40}", pinned_revision)
subprocess.run(["git", "cat-file", "-e", f"{pinned_revision}^{{commit}}"], cwd=root, check=True)
subprocess.run(["git", "merge-base", "--is-ancestor", pinned_revision, "HEAD"], cwd=root, check=True)
assert package_manifest["runtime"]["localInferenceRequired"] is True
assert package_manifest["delivery"]["romPathPreserved"] is True
assert package_manifest["runtime"]["requiresGoogleMobileServices"] is False
assert package_manifest["productIdentity"]["productName"] == "MANA"
module_ids = {module["id"] for module in module_manifest["modules"]}
for module_id in ("brain.core", "brain.local_inference", "brain.shogun", "brain.hydra", "brain.microworld", "connectors.google_services", "os.launcher", "os.settings", "os.control_center", "os.system_ui", "os.boot_branding", "os.adaptive_system_merge", "os.recovery_ota"):
    assert module_id in module_ids, f"Ω module registry missing: {module_id}"
assert default_settings["autonomy"]["level"] == "maximum"
assert default_settings["integrations"]["requiresGoogleMobileServices"] is False
for required in ("node --check game/minecraft-bedrock/MANA_Companion_BP/scripts/main.js", "MANA_Companion_BP.mcpack", 'cat > "$PACKAGE/README_RU.md"', 'cat > "$PACKAGE/BUILD_REPORT_RU.txt"', "game/minecraft-bedrock/**", "compression-level: 0"):
    assert required in workflow, f"Omega archive contract missing: {required}"

assert '.github/workflows/mana-android-build.yml' in workflow
for emulator_workflow in (root / '.github/workflows/mana-android-emulators.yml', root / '.github/workflows/mana-emulator-smoke.yml'):
    emulator_text = emulator_workflow.read_text()
    assert 'disable-linux-hw-accel: false' in emulator_text, f'{emulator_workflow} must allow KVM acceleration'
    assert 'pre-emulator-launch-script:' in emulator_text and 'chmod 666 /dev/kvm' in emulator_text, f'{emulator_workflow} must initialize KVM/ADB before boot'

emulator_smoke_script = root / 'tools/run_mana_emulator_smoke.sh'
assert emulator_smoke_script.is_file(), 'The emulator launch smoke script must be a tracked source file'
assert 'script: bash "$GITHUB_WORKSPACE/tools/run_mana_emulator_smoke.sh"' in (root / '.github/workflows/mana-emulator-smoke.yml').read_text()
assert 'connectedDebugAndroidTest' in emulator_smoke_script.read_text()
assert 'adb install -r "$APK"' in emulator_smoke_script.read_text()
assert 'adb shell monkey -p com.mana.launcher' in emulator_smoke_script.read_text()
