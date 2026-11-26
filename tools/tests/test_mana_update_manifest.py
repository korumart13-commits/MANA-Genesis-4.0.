import unittest
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization

from mana_update_manifest import (
    CHANNEL, ManifestError, canonical_payload, validate_manifest,
)


class ManaUpdateManifestTests(unittest.TestCase):
    def setUp(self):
        self.private = Ed25519PrivateKey.generate()
        self.public = self.private.public_key()
        self.manifest = {
            "schema_version": 1,
            "channel": CHANNEL,
            "release_id": "genesis-rmx5566ru-0001",
            "version_code": 1,
            "version_name": "MANA +1",
            "target": {"model": "RMX5566RU", "device": "RE645F"},
            "base": {
                "android_api": 36,
                "build_fingerprint":
                    "realme/RMX5566RU/RE645F:16/BP2A.250605.015/"
                    "V.2f025ac-8895bb-9a8318:user/release-keys",
            },
            "artifacts": [{
                "kind": "module_bundle",
                "url": "https://example.invalid/mana/modules-1.zip",
                "size": 1234,
                "sha256": "a" * 64,
            }],
        }
        self.manifest["signature"] = {
            "algorithm": "Ed25519",
            "value_hex": self.private.sign(canonical_payload(self.manifest)).hex(),
        }

    def test_accepts_separately_signed_mana_channel(self):
        validate_manifest(self.manifest, self.public)

    def test_rejects_oem_channel_or_fake_android_version(self):
        bad = dict(self.manifest)
        bad["channel"] = "realme.ota"
        with self.assertRaises(ManifestError):
            validate_manifest(bad, self.public)

    def test_rejects_wrong_device(self):
        bad = dict(self.manifest)
        bad["target"] = {"model": "RMX5566", "device": "other"}
        with self.assertRaises(ManifestError):
            validate_manifest(bad, self.public)

    def test_rejects_wrong_base_fingerprint(self):
        with self.assertRaises(ManifestError):
            validate_manifest(self.manifest, self.public, "different-build")

    def test_rejects_modified_manifest(self):
        bad = dict(self.manifest)
        bad["version_code"] = 2
        with self.assertRaises(ManifestError):
            validate_manifest(bad, self.public)

    def test_rejects_firmware_partition_artifact(self):
        bad = dict(self.manifest)
        bad["artifacts"] = [{
            "kind": "boot_image",
            "url": "https://example.invalid/boot.img",
            "size": 1,
            "sha256": "b" * 64,
        }]
        bad["signature"] = {
            "algorithm": "Ed25519",
            "value_hex": self.private.sign(canonical_payload(bad)).hex(),
        }
        with self.assertRaises(ManifestError):
            validate_manifest(bad, self.public)


if __name__ == "__main__":
    unittest.main()
