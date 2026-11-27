# RMX5566 exact OTA identity captured from owner — 2026-11-27

The owner supplied these values directly from Termux `getprop`:

| Property | Exact value |
|---|---|
| `ro.product.name` | `RMX5566RU` |
| `ro.build.version.ota` | `RMX5566_11.F.03_2030_202610170441` |
| `ro.build.oplus_nv_id` | `00110111` |
| `ro.build.version.realmeui` | `V7.0` |
| `ro.build.version.oplusrom` | `V16.0.0` |
| `ro.build.display.id` | `RMX5566_16.0.5.1310(EX01)` |

## OTA client compatibility and region

The current public `realme-ota` CLI accepts the exact model, OTA version, Realme UI major version, NV ID and region. Its current CLI choices include RUI 7; a public commit explicitly added RUI 7 support. Region codes documented by the project are GL=0, CN=1, IN=2, EU=3. Sources:
- https://github.com/R0rt1z2/realme-ota
- https://github.com/R0rt1z2/realme-ota/commit/d264167

Given the owner's European regulatory marking and EU use, EU (3) is the first region to test, but the regulatory mark alone does not prove the OTA sales channel. Do not silently switch to another region if the query fails.

Exact query profile:
- product: `RMX5566RU`
- current OTA: `RMX5566_11.F.03_2030_202610170441`
- RUI major: `7`
- NV ID: `00110111` (preserve leading zeros)
- first region attempt: `EU / 3`
- current display build: `RMX5566_16.0.5.1310(EX01)`

The public OPlus downloader project confirms these are the precise property inputs it reads from the device and explains that its realme-ota backend uses them to query OPPO's OTA endpoint, returning component packet download URL, size and MD5. It also documents the separate public catalog API:
`GET https://roms.danielspringer.at/api/ota.php?model=RMX5566&latest=1`
Source: https://github.com/suddenBook/OPlus_firmware_download

The catalog endpoint could not be opened through the current research fetcher, so no catalog response is claimed. No OTA package URL or package metadata has yet been obtained.

## Safe query-only Termux command

This command installs the open-source CLI and queries the EU endpoint. It does not intentionally invoke a flash command; do not add beta, IMEI, GUID, proxy or download options.

```sh
pkg update -y
pkg install -y python git
python -m pip install --upgrade git+https://github.com/R0rt1z2/realme-ota
realme-ota -r 3 -v 3 RMX5566RU RMX5566_11.F.03_2030_202610170441 7 00110111
```

The command may display temporary vendor download links or component metadata. Do not publish a live signed URL; share only the returned target version, component names, sizes, MD5 values and whether the tool labels the result full or incremental. If the CLI prompts to download, cancel and report the prompt first. Do not enter IMEI or GUID.

## Package validation gates

A returned result is only a lead. Before any archive is accepted:
1. exact model and region/channel must match;
2. target build must be understood and not be an unintended downgrade;
3. distinguish full package from incremental/component OTA and record source-build prerequisites;
4. verify every provided size and checksum;
5. inspect package signature and payload manifest;
6. enumerate partition names, sizes and rollback indexes;
7. establish an independent, tested stock recovery path.

No package has yet passed these gates. No download, flash, bootloader unlock, partition write or AVB bypass was performed.

## Device source status

Exact RMX5566 kernel source matching `5.15.197-android13-8-o-01074-gdfa4acfa411e`, defconfig, DTS/DTSI, DTBO, complete device tree and matching vendor blobs remain unverified. Similar SoC sources are not substitutes.
