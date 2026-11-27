# OTA discovery inputs for RMX5566 — 2026-11-27

This document records the concrete OTA lookup inputs and a safe collection procedure. It is not a firmware installer.

## Device identity already confirmed

- Model: RMX5566
- Product name: RMX5566RU
- Device: RE645F
- Board: bengal
- Build: RMX5566_16.0.5.1310(EX01)
- Android: 16 / API 36
- realme UI: 7.0
- Kernel: 5.15.197-android13-8-o-01074-gdfa4acfa411e
- Region label shown on regulatory page: Europe; regulatory label also contains Indonesia certification. These labels do not establish the OTA NV ID or sales-region package channel.

## Exact OTA query fields

The public OPlus Firmware Download implementation documents these fields for the OPPO/realme OTA endpoint:
- `ro.product.name`
- `ro.build.version.ota`
- `ro.build.oplus_nv_id`
- `ro.build.version.realmeui`
- `ro.build.version.oplusrom`
- `ro.build.display.id`

The owner's screenshot/audit already gives model/build/UI version but not a verified OTA version string or NV ID. Do not guess NV ID or substitute a guessed region.

## Read-only Termux collector

Run these commands in Termux. They print only the six OTA lookup fields and do not read identifiers such as IMEI, serial number, Android ID, Wi-Fi MAC, or phone number.

```sh
for p in ro.product.name ro.build.version.ota ro.build.oplus_nv_id ro.build.version.realmeui ro.build.version.oplusrom ro.build.display.id; do
  printf '%s=' "$p"
  getprop "$p"
done
```

If `getprop` returns blank for one or more values, leave it blank; do not infer a replacement. Do not share full `getprop` output because it can contain device identifiers.

## Public catalog query

The OPlus downloader documents the public catalog request:
`GET https://roms.danielspringer.at/api/ota.php?model=RMX5566&latest=1`

A research-environment request did not return a readable response, so availability for RMX5566 remains unconfirmed. If the catalog returns a record, preserve its original source URL, target build, file size, MD5, and model metadata. A catalog result is a lead, not proof of package suitability.

## OPPO OTA endpoint

The OPlus downloader documents that the direct backend builds an encrypted request from model, current OTA version, ColorOS/realme UI version, region and NV ID; the response includes a download URL, size and MD5. Signed URLs expire. The protocol is implemented in:
- https://github.com/R0rt1z2/realme-ota
- https://github.com/suddenBook/OPlus_firmware_download

The app supports Android 13+ and arm64-v8a. It is an independent open-source client, not an OEM-certified recovery tool. A successful response must still be checked for exact target/source build, package type, checksum and partition coverage.

## Package acceptance checklist

Reject a candidate unless all available evidence is consistent:
1. exact model RMX5566 and correct product/region channel;
2. target build equals RMX5566_16.0.5.1310(EX01), or a documented newer compatible build;
3. full OTA vs incremental is explicitly known;
4. source build requirement is known for incremental packages;
5. size and checksum verify after download;
6. package signature and payload manifest are inspectable;
7. partition list and rollback metadata are understood;
8. an independent, tested stock recovery path exists.

No download URL or filename alone passes this checklist. Never flash a package solely because it says RMX5566.
