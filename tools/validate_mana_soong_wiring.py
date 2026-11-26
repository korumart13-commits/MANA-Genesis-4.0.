#!/usr/bin/env python3
'''Static contract checks for MANA Android 16 Soong and companion AIDL wiring.'''
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
brain_bp = (root / 'engine/mana-brain/Android.bp').read_text()
assistant_bp = (root / 'system/mana/apps/ManaAssistant/Android.bp').read_text()
aidl_bp = (root / 'system/mana/brain/aidl/Android.bp').read_text()
product = (root / 'system/mana/mana_product.mk').read_text()
brain_sources = sorted((root / 'engine/mana-brain/src/main/kotlin').rglob('*.kt'))

assert 'name: "mana-brain-core"' in brain_bp
assert 'src/main/kotlin/**/*.kt' in brain_bp
assert brain_sources, 'Canonical Kotlin MANA Brain sources are missing'
assert 'kotlin_lang_version: "2"' in brain_bp
for dependency in ('"kotlinx-coroutines-core"', '"gson"'):
    assert dependency in brain_bp, f'Missing Android 16 Soong dependency: {dependency}'
assert '"mana.brain-java"' in assistant_bp and '"mana-brain-core"' in assistant_bp
assert 'name: "mana.brain"' in aidl_bp and 'java: {' in aidl_bp

aidl_root = root / 'system/mana/brain/aidl'
aidl_files = (
    'com/mana/brain/BrainBinderRequest.aidl',
    'com/mana/brain/BrainBinderResponse.aidl',
    'com/mana/brain/IBrainCallback.aidl',
    'com/mana/brain/IBrainService.aidl',
    'com/mana/brain/ILocalInferenceCallback.aidl',
    'com/mana/brain/ILocalInferenceService.aidl',
)
for relative in aidl_files:
    assert (aidl_root / relative).is_file(), f'Missing AIDL source: {relative}'
    assert f'"{relative}"' in aidl_bp, f'Soong does not compile AIDL source: {relative}'
assert not (aidl_root / 'com/mana/brain/BrainRequest.aidl').exists(), 'AIDL request conflicts with Kotlin engine request'
assert not (aidl_root / 'com/mana/brain/BrainResponse.aidl').exists(), 'AIDL response conflicts with Kotlin engine response'

gradle_settings = (root / 'launcher/settings.gradle.kts').read_text()
binder_gradle = (root / 'system/mana/android-binder/build.gradle.kts').read_text()
assert ':system-binder' in gradle_settings
assert 'ManaBrainBinderService.java' in binder_gradle and 'ManaHomeActivity.java' in binder_gradle
assert '../brain/aidl' in binder_gradle
gradle_app = (root / 'launcher/app/build.gradle.kts').read_text()
assert 'aidl = true' in gradle_app
assert 'rootProject.file("../system/mana/brain/aidl")' in gradle_app
assert (root / 'launcher/app/src/test/java/com/mana/brain/AidlContractCompileTest.kt').is_file()
assert (root / 'system/mana/apps/ManaLauncher/tests/Android.bp').is_file(), 'Launcher Binder integration test is missing'
assert (root / 'system/mana/apps/ManaLauncher/tests/src/com/mana/launcher/tests/ManaLauncherBrainIntegrationTest.java').is_file()
assert (root / 'launcher/app/src/main/java/com/mana/launcher/ManaLocalInferenceService.kt').is_file()
assert (root / 'launcher/app/src/main/java/com/mana/launcher/ManaLocalModelRuntimeHolder.kt').is_file()

android_uri = 'http://schemas.android.com/apk/res/android'
android_ns = '{' + android_uri + '}'
assistant_manifest = ET.parse(root / 'system/mana/apps/ManaAssistant/AndroidManifest.xml').getroot()
assistant_queries = [node.attrib.get(android_ns + 'name') for node in assistant_manifest.findall('./queries/package')]
assert 'com.mana.genesis' in assistant_queries, 'Assistant cannot see companion provider package'
launcher_manifest = ET.parse(root / 'system/mana/apps/ManaLauncher/AndroidManifest.xml').getroot()
launcher_queries = [node.attrib.get(android_ns + 'name') for node in launcher_manifest.findall('./queries/package')]
assert 'com.mana.assistant' in launcher_queries, 'Launcher cannot see Assistant package'
assert 'com.mana.permission.BIND_BRAIN' in [
    node.attrib.get(android_ns + 'name') for node in launcher_manifest.findall('uses-permission')
], 'Launcher does not request protected Binder permission'

companion_manifest = ET.parse(root / 'launcher/app/src/main/AndroidManifest.xml').getroot()
provider = companion_manifest.find("./application/service[@android:name='.ManaLocalInferenceService']", {'android': android_uri})
assert provider is not None and provider.attrib.get(android_ns + 'exported') == 'true'
companion_queries = [node.attrib.get(android_ns + 'name') for node in companion_manifest.findall('./queries/package')]
assert 'com.mana.assistant' in companion_queries, 'Provider cannot identify Assistant caller'

product_packages = {line.strip().rstrip(chr(92)).strip() for line in product.splitlines()}
for app in ('ManaLauncher', 'ManaAssistant', 'ManaSettings', 'ManaControlCenter'):
    assert app in product_packages, f'{app} is not installed in MANA product fragment'
assert 'mana.brain-java' not in product and 'mana-brain-core' not in product

tile_manifest = ET.parse(root / 'system/mana/apps/ManaControlCenter/AndroidManifest.xml').getroot()
tile = tile_manifest.find("./application/service[@android:permission='android.permission.BIND_QUICK_SETTINGS_TILE']", {'android': android_uri})
assert tile is not None, 'Quick Settings tile must use the platform bind permission'
assert 'android.service.quicksettings.action.QS_TILE' in [
    node.attrib.get(android_ns + 'name') for node in tile.findall('intent-filter/action')
]

print(f'MANA Android contract valid: {len(brain_sources)} Kotlin engine sources, {len(aidl_files)} shared AIDL sources, LiteRT-LM Binder provider, Launcher-UID integration test, 4 product apps, Quick Settings integration.')
