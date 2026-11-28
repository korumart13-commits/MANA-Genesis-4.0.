#!/usr/bin/env python3
"""Static contract guard for the one-button realme C85 4G MANA installer.

This validates source/documentation wiring only; it is not a device or model-runtime test.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
activity = (root / "launcher/app/src/main/java/com/mana/launcher/MainActivity.kt").read_text()
coordinator = (root / "launcher/app/src/main/java/com/mana/launcher/ManaOneTapSetupCoordinator.kt").read_text()
catalog = (root / "launcher/app/src/main/java/com/mana/launcher/ManaModelCatalog.kt").read_text()
setup_doc = (root / "launcher/LOCAL_MODEL_SETUP.md").read_text()
requirements = (root / "docs/MANA_GENESIS_4_0_NON_NEGOTIABLE_REQUIREMENTS_AND_GAPS.md").read_text()
workflow = (root / ".github/workflows/mana-launcher.yml").read_text()

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
assert "models/Gemma/MODEL_MANIFEST_RU.txt" in workflow
assert "SHA256SUMS.txt" in workflow
assert "MANA-Omega-FULL-INSTALLER.zip" in workflow
assert "MANA-Omega-FULL-INSTALLER" in workflow
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
