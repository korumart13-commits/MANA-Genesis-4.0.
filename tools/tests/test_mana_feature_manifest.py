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
