# RMX5566RU official firmware source check — 2026-11-26

## Result

No publicly indexed, independently verifiable full OTA matching the exact user-reported build `RMX5566_16.0.5.1310(EX01)` and fingerprint `realme/RMX5566RU/RE645F:16/...` was found in this research pass. Do not substitute a different regional/model package.

## Findings

1. The device information source identifies RE645F / RMX5566 as Qualcomm SM6225pro (Snapdragon 685), board `bengal`, Android 16, kernel 5.15.197, A/B and dynamic partitions. It reports October 2026 as the newest security patch observed among user-shared samples; this is not an OEM rollout statement:
   https://www.deviceinfo.app/devices/realme/c85/

2. The official Realme EU security/Android Enterprise list currently labels `RMX5566` as `realme C85 5G`, while the device information source above associates RE645F / RMX5566 with Snapdragon 685 and C85 4G. This public catalog naming conflict means the marketing name alone must not select firmware. The exact device fingerprint, board, SoC and OEM OTA response must agree:
   https://www.realme.com/eu/legal/AndroidEnterpriseRecommended

3. A third-party firmware index lists `RMX5566export_11_15.0.2.900(EX01)_202601231432250000.zip`, dated 2026-08-20, around 11.70 GB. This is an Android 15-era package and does not match the user's reported Android 16 / 16.0.5.1310 build. It is not a valid downgrade/base package for this task:
   https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894

4. The public `R0rt1z2/realme-ota` project documents the request parameters needed to query BBK OTA services (product model, current OTA version, Realme UI major, NV identifier, region). It also states that since Android 11 / Realme UI 2, Realme uses component updates and a complete OTA URL may not be returned:
   https://github.com/R0rt1z2/realme-ota

5. A separate public tool, OTA Pulse, provides a well-structured Android implementation reference for OTA discovery, metadata, downloads, payload extraction, ARB inspection and history. Its published Realme device catalog was inspected and does not currently include RMX5566 / RE645F:
   https://github.com/RemuruSama/OTA-Pulse

## Consequence for MANA

- Do not download or flash the older Android 15 RMX5566 package as a base for the Android 16 target.
- Do not assume that the official Realme web catalog's marketing label is the exact hardware variant.
- The next source acquisition step is a sanitized live OTA component query using the already recorded device baseline (model, OTA profile, RUI version and EU region), without IMEI/GUID. Record component IDs, version strings, hashes, sizes and URLs. A component response is not automatically a full firmware package.
- If no full package is returned, obtain the exact installed stock images through a lawful owner-controlled backup/export route or vendor-provided full package. Do not use a random mirror as a substitute for matching stock firmware.
- Only after exact source images and matching device tree/vendor files are verified can MANA target-files and a signed MANA OTA be built.
