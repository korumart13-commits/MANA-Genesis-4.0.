import importlib.util
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("mana_autodev", ROOT / "mana_autodev.py")
autodev = importlib.util.module_from_spec(spec)
spec.loader.exec_module(autodev)

kspec = importlib.util.spec_from_file_location(
    "validate_mana_kernel_patch", ROOT / "validate_mana_kernel_patch.py"
)
kernel_gate = importlib.util.module_from_spec(kspec)
kspec.loader.exec_module(kernel_gate)


class AutoDevPolicyTests(unittest.TestCase):
    def test_engine_source_and_tests_are_allowed(self):
        autodev.validate_paths("engine", [
            "engine/mana-brain/src/main/kotlin/com/mana/brain/NewFeature.kt",
            "engine/mana-brain/src/test/kotlin/com/mana/brain/NewFeatureTest.kt",
        ])

    def test_engine_cannot_change_build_scripts_or_workflows(self):
        for path in ("engine/mana-brain/build.gradle.kts", ".github/workflows/ci.yml"):
            with self.subTest(path=path), self.assertRaises(autodev.AutoDevError):
                autodev.validate_paths("engine", [path])

    def test_app_track_allows_mana_apps_but_not_firmware_or_workflows(self):
        autodev.validate_paths("apps", ["system/mana/apps/ManaSettings/src/New.java"])
        for path in ("kernel/boot.img", ".github/workflows/build.yml"):
            with self.subTest(path=path), self.assertRaises(autodev.AutoDevError):
                autodev.validate_paths("apps", [path])

    def test_kernel_track_only_accepts_patchsets_and_docs(self):
        autodev.validate_paths("kernel", ["kernel/mana-patches/0001-mana-driver.patch"])
        for path in ("kernel/realme/defconfig", "kernel/drivers/gpu/evil.c",
                     "kernel/mana-patches/boot.img"):
            with self.subTest(path=path), self.assertRaises(autodev.AutoDevError):
                autodev.validate_paths("kernel", [path])

    def test_traversal_and_absolute_paths_are_rejected(self):
        for path in ("engine/mana-brain/src/main/kotlin/../../../../x.kt", "/tmp/x.kt"):
            with self.subTest(path=path), self.assertRaises(autodev.AutoDevError):
                autodev.validate_paths("engine", [path])

    def test_binary_and_symlink_patches_are_rejected(self):
        for patch in (
            "GIT binary patch\\nliteral 10\\n",
            "diff --git a/foo.kt b/foo.kt\\nnew file mode 120000\\n",
            "diff --git a/foo.kt b/foo.kt\\nold mode 100644\\nnew mode 100755\\n",
        ):
            with self.subTest(patch=patch), self.assertRaises(autodev.AutoDevError):
                autodev.validate_patch_text(patch)

    def test_kernel_patch_may_only_add_new_mana_driver_files(self):
        good = """diff --git a/drivers/misc/mana/core.c b/drivers/misc/mana/core.c
new file mode 100644
--- /dev/null
+++ b/drivers/misc/mana/core.c
@@ -0,0 +1,1 @@
+/* MANA */
"""
        bad = """diff --git a/drivers/gpu/core.c b/drivers/gpu/core.c
--- a/drivers/gpu/core.c
+++ b/drivers/gpu/core.c
@@ -1,1 +1,1 @@
-old
+new
"""
        with tempfile.TemporaryDirectory() as tmp:
            good_path = Path(tmp) / "good.patch"
            good_path.write_text(good)
            kernel_gate.validate_patch(good_path)
            bad_path = Path(tmp) / "bad.patch"
            bad_path.write_text(bad)
            with self.assertRaises(ValueError):
                kernel_gate.validate_patch(bad_path)


if __name__ == "__main__":
    unittest.main()
