import importlib.util
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "validate_mana_feature_manifest", ROOT / "validate_mana_feature_manifest.py"
)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class FeatureManifestTests(unittest.TestCase):
    def setUp(self):
        self.data = json.loads((ROOT.parent / "os/MANA_GENESIS_4.0_COMPLETE_FEATURE_MANIFEST.json").read_text())
        self.catalog = (ROOT.parent / "os/core/src/main/kotlin/com/mana/os/core/ManaSystemModuleCatalog.kt").read_text()

    def test_complete_owner_manifest_matches_catalog(self):
        module.validate(self.data, self.catalog)

    def test_life_control_center_is_registered_and_uses_public_android_handoffs(self):
        source = (ROOT.parent / "launcher/app/src/main/java/com/mana/launcher/ManaLifeControlCenterActivity.kt").read_text()
        manifest = (ROOT.parent / "launcher/app/src/main/AndroidManifest.xml").read_text()
        launcher = (ROOT.parent / "launcher/app/src/main/java/com/mana/launcher/MainActivity.kt").read_text()
        self.assertIn("ManaLifeControlCenterActivity", manifest)
        self.assertIn("ManaLifeControlCenterActivity", launcher)
        self.assertIn("MediaStore.ACTION_IMAGE_CAPTURE", source)
        self.assertIn("Intent.ACTION_SEND", source)
        self.assertIn("createConfirmDeviceCredentialIntent", source)
        self.assertIn("ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS", source)
        sleep = (ROOT.parent / "launcher/app/src/main/java/com/mana/launcher/ManaSleepSchedule.kt").read_text()
        self.assertIn("setExactAndAllowWhileIdle", sleep)
        self.assertIn("setAndAllowWhileIdle", sleep)
        self.assertIn("ManaSleepScheduler.schedule(context)", (ROOT.parent / "launcher/app/src/main/java/com/mana/launcher/ManaBootLifecycle.kt").read_text())
        self.assertNotIn("Runtime.exec", source)
        self.assertNotIn("su -c", source)

    def test_missing_micro_universe_is_rejected(self):
        bad = dict(self.data)
        bad["modules"] = [m for m in self.data["modules"] if m["id"] != "mana.microuniverse"]
        with self.assertRaises(ValueError):
            module.validate(bad, self.catalog)

    def test_unverified_kernel_must_not_be_allowed_to_replace(self):
        bad = dict(self.data)
        bad["target"] = dict(self.data["target"], kernel_replacement_allowed=True)
        with self.assertRaises(ValueError):
            module.validate(bad, self.catalog)

    def test_future_updates_pipeline_and_selinux_are_required(self):
        bad = dict(self.data)
        bad["future_updates"] = dict(self.data["future_updates"], pipeline=["idea", "install"])
        with self.assertRaises(ValueError):
            module.validate(bad, self.catalog)
        bad = dict(self.data)
        bad["security"] = dict(self.data["security"], selinux_enforcing=False)
        with self.assertRaises(ValueError):
            module.validate(bad, self.catalog)

    def test_receptionist_settings_are_preserved(self):
        bad = dict(self.data)
        bad["receptionist"] = dict(self.data["receptionist"], auto_answer_delay_seconds=0)
        with self.assertRaises(ValueError):
            module.validate(bad, self.catalog)


if __name__ == "__main__":
    unittest.main()
