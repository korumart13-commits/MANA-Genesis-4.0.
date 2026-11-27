# RMX5566 exact OTA identity captured from owner — 2026-11-27

The owner ran the read-only Termux `getprop` query and supplied these exact values:

| Property | Value |
|---|---|
| `ro.product.name` | `RMX5566RU` |
| `ro.build.version.ota` | `RMX5566_11.F.03_2030_202610170441` |
| `ro.build.oplus_nv_id` | `00110111` |
| `ro.build.version.realmeui` | `V7.0` |
| `ro.build.version.oplusrom` | `V16.0.0` |
| `ro.build.display.id` | `RMX5566_16.0.5.1310(EX01)` |

These values now provide a much more precise OTA lookup tuple than model/build alone. They do not prove which OTA package the server will return.

## Canonical OTA query tuple

- model/product: RMX5566RU
- OTA version: RMX5566_11.F.03_2030_202610170441
- NV ID: 00110111
- realme UI: V7.0
- OPlus ROM: V16.0.0
- display build: RMX5566_16.0.5.1310(EX01)

Use the exact values above in an OTA metadata request. Do not normalize away leading zeros in NV ID. Treat any returned signed URL as temporary and record response metadata before it expires.

## Verification status

- This tuple was supplied directly by the owner from Termux `getprop`.
- OTA catalog/backend availability for this tuple has not yet been confirmed.
- No OTA URL, package size, MD5/SHA checksum, payload type, source-build constraint, partition manifest, rollback metadata or package signature has yet been obtained.
- A response matching RMX5566RU is only a candidate. It must still match the exact build/region and pass package verification.
- No firmware has been downloaded, flashed or installed as part of this step.

## Next operations

1. Query the public OTA catalog with model RMX5566 and preserve any exact matching result.
2. Use the documented OPlus OTA request client/protocol with the exact tuple above, without guessing additional region/NV values.
3. Compare the returned target build against the current build. Prefer a full package; identify incremental packages and their required source build.
4. Download only after the server response identifies the exact package; verify supplied checksum and package signature/manifest before extraction.
5. Extract read-only metadata first: payload manifest, partition names, sizes, rollback indexes, boot/vendor_boot/dtbo/vbmeta presence.
6. Continue to block flashing until a verified matching stock restore route and device-specific boot chain are available.

Privacy note: these are software build/channel properties, not IMEI, serial number, Android ID, or credentials. Keep all personal hardware identifiers out of OTA reports.
