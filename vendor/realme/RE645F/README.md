# RE645F vendor integration

Status: **BLOCKED / DOCUMENTATION ONLY**. No vendor blob set is included or represented as complete.

## Confirmed context

The target reports RMX5566RU / RE645F, Qualcomm SM6225 / bengal / qcom, Android 16 (API 36), runtime kernel 5.15.197, and OPlus/Qualcomm platform components. Prior owner-supplied audit summaries identify Qualcomm and OPlus services/HAL families, but the exact complete manifest, versions, instances, transport, SELinux labels and blob inventory have not been preserved as machine-readable source evidence in this repository.

## Required inputs

- Matching RMX5566RU firmware package and exact build fingerprint.
- Vendor and odm partition contents obtained through an owner-authorized, lawful extraction workflow.
- `/vendor/etc/vintf/manifest.xml`, vendor/odm compatibility matrices and framework compatibility requirements.
- HAL implementation libraries, service rc files, init rc fragments, permissions XML, firmware files and associated symlinks.
- Module dependency and symbol-version information for kernel/vendor modules.
- Provenance record: source package, firmware build, file hash, license/redistribution status and extraction date.

## Integration rules

1. Do not borrow blobs from C85 5G / RMX5253 or another SM6225 phone.
2. Do not invent HAL versions or instances from a partial service listing.
3. Keep proprietary blobs outside the public repository unless redistribution is explicitly permitted. Prefer a generated, owner-local extraction list and hash manifest.
4. Preserve original file names, symlinks, permissions and SELinux labels during extraction.
5. Compare vendor VINTF declarations with the Android 16 framework compatibility matrix; do not lower compatibility requirements to silence failures.
6. Keep radio, modem, trust, camera and biometric firmware unchanged until their exact dependencies and licensing are established.

## Proposed generated artifacts (not present yet)

- `proprietary-files.txt` — verified extraction paths only.
- `vendor_manifest.snapshot.xml` — byte-for-byte source snapshot.
- `vendor_blob_hashes.sha256` — SHA-256 and provenance.
- `vintf_diff.md` — framework/vendor compatibility findings.
- `vendor_modules.txt` — module names, vermagic and dependencies.

## State

- [CONFIRMED] Qualcomm/OPlus platform family is present on the target.
- [PARTIAL] Previous audit summaries mention audio, camera, Bluetooth, IMS/DSP and OPlus services; exact versions and full inventory are not established here.
- [MISSING] Complete vendor/odm snapshots, verified VINTF XML, blob list, hashes, firmware provenance and redistribution review.
- [BLOCKED] Product vendor makefiles and proprietary module packaging.

No vendor image or OTA package may be generated from this placeholder.
