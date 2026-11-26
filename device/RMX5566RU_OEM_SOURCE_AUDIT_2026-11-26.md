# RMX5566RU OEM source audit — 2026-11-26

Target runtime identity recorded for MANA:
- Model/product: realme RMX5566RU / RE645F
- Product family: realme C85 4G, codename reported as `moscow-b`
- SoC/platform: Qualcomm SM6225 / `bengal` / `qcom`
- Android: 16 / API 36
- Runtime kernel: `5.15.197-android13-8-o-01074-gdfa4acfa411e`
- Runtime build fingerprint: `realme/RMX5566RU/RE645F:16/BP2A.250605.015/V.2f025ac-8895bb-9a8318:user/release-keys`

## New OEM source lead found

The official realme-kernel-opensource GitHub organization publishes these C85-family Android B source repositories:

1. Kernel source:
   - Repository: https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-kernel-source
   - Audited commit: `52ea05ec6849f7c3dd9a65864b5a52038deb9f9d`
   - Commit message identifies `RMX555525665_16.0.2.403(EX01)` and Qualcomm platform release `AU_LINUX_KERNEL.PLATFORM.2.0.R1.00.00.00.004.178`.
   - Root `Makefile` declares Linux `5.15.180`.
   - `build.config.msm.bengal` sets `MSM_ARCH=bengal`, variants `consolidate gki`, boot header v4, init_boot, vendor_dlkm, system_dlkm and DT overlay support.

2. Matching family vendor/kernel-platform source:
   - Repository: https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-vendor-source
   - Audited commit: `2ab37fb9ceda42cd8679dec8585d4ff5209c4e90`
   - Commit message identifies the same `RMX555525665_16.0.2.403(EX01)` release and lists Qualcomm/Oplus kernel-platform component tags.
   - Contains `kernel_platform/qcom/proprietary/devicetree/oplus/Moscow-25665-khaje-qrd-nopmi-overlay.dts`, `Moscow-25666-khaje-qrd-nopmi-overlay.dts`, further Moscow project overlays, and the common Moscow overlay.
   - The 25665 DTS sets `model = "Qualcomm Technologies, Inc. KHAJE QRD nopmi overlay, Moscow"` and `oplus,project-id = <25665>`.
   - The common Moscow DTSI includes the SM6225 fingerprint data and Moscow touchscreen, thermal and LED fragments.
   - The vendor-source tree contains Qualcomm/Oplus kernel and device-tree source components. This is not evidence that the Android proprietary vendor partition, all HAL binaries, modem firmware or exact VINTF manifests are available.

## Compatibility conclusion

This is a materially better lead than the previously found SM6225 community kernels: it is an OEM-published C85/C85 Pro Android B source family, declares the correct `bengal` platform, and includes Moscow product overlays.

It is **not yet accepted as the exact RMX5566RU build input**:
- The audited source commit names `RMX555525665_16.0.2.403(EX01)`, not the target's recorded `RMX5566RU` fingerprint.
- Its kernel Makefile is `5.15.180`; the target runtime reports `5.15.197`.
- The Moscow overlays include project IDs 25665, 25666 and others, but the target phone's own Oplus project ID has not yet been captured and matched to one overlay.
- No complete target Android product/device tree, proprietary Android vendor blobs, HAL/VINTF set, partition map, AVB signing metadata or stock restore proof has been established from these repositories.

Therefore:
- Keep the C85/C85 Pro OEM source pinned as a candidate for a device-specific source audit.
- Do not substitute project 25665 or 25666 based only on family/codename similarity.
- Do not build or flash a kernel/image until the target project ID, exact source branch/version, defconfig, DTBO/DTS, module ABI and vendor interface compatibility are proven against the device.
- Continue to keep the ROM release gate blocked.

## Read-only target-side identity needed next

When convenient, collect these properties in Termux; they are read-only and do not require root:

```sh
{
  for key in \
    ro.product.model ro.product.device ro.product.name \
    ro.product.vendor.device ro.build.fingerprint \
    ro.build.version.incremental ro.board.platform ro.boot.hardware \
    ro.soc.model ro.boot.project_name ro.boot.project \
    ro.oplus.regionmark ro.separate.soft ro.build.version.oplusrom;
  do
    printf '%s=' "$key"
    getprop "$key"
  done
} | tee "$HOME/mana_rmx5566_audit/oem_project_identity.txt"
```

If project properties are blank, that is acceptable; do not use root or read/write block devices to force access. The source candidate still requires an authenticated mapping to the target.

## Other candidate rejection

- `cd-Crypton/android_kernel_realme_sm6225` (`android13-bengal`) declares Linux 4.19.157, not target 5.15.197.
- `cdpkp/android_kernel_tree_realme_sm6225` (`lineage-20`) also declares Linux 4.19.157.
- The C85 5G RMX5253 / MediaTek source is not applicable to this Qualcomm C85 4G target.


## Additional project-ID match discovered — 2026-11-26

A further public firmware-catalog cross-check identifies the RMX5566 C85 4G firmware family with Oplus project/PCB ID **25697**. This is external catalog metadata, not a property read directly from the handset in this audit, so it is treated as a strong candidate mapping rather than sole proof of the phone's live project ID.

Crucially, the official realme-kernel-opensource C85/C85 Pro Android B vendor-source tree at commit `2ab37fb9ceda42cd8679dec8585d4ff5209c4e90` contains the exact matching overlay:

- `kernel_platform/qcom/proprietary/devicetree/oplus/Moscow-25697-khaje-qrd-nopmi-overlay.dts`
- It explicitly declares `oplus,dtsi_no = <25697>` and `oplus,project-id = <25697>`.
- It includes the Moscow common overlay, NFC, 25695 sensor profile, and the shared charging DTSI for 25695/25696/25697.
- The same source tree contains the matching common Moscow SM6225 fingerprint, touchscreen, thermal and LED fragments.

This is a substantial improvement over the previously noted 25665/25666-only candidates: **25697 is now the leading board-overlay candidate for RMX5566**. Do not select 25695 merely because some shared fragments use that filename; the top-level 25697 overlay is the candidate that sets the project ID.

Remaining release blockers are still real:
- The public source kernel tree declares Linux 5.15.180, while the handset audit reports 5.15.197. The source branch/commit and vendor module ABI must be reconciled to the exact installed build before building or using a kernel.
- This overlay is not a complete `device/realme/RE645F` product tree and does not supply all proprietary Android vendor blobs, modem firmware, HAL/VINTF manifests, partition sizes, AVB metadata, signing keys or a verified stock restore package.
- The device's exact live project property should be reconciled against 25697 if it is available in the previously captured audit output; do not ask the owner to repeat an audit already supplied.

Sources:
- Public RMX5566 firmware catalog lists project ID 25697: https://solution-firmware.com/index.php?a=downloads&b=folder&id=2894
- OEM source overlay at the pinned vendor-source commit: https://github.com/realme-kernel-opensource/realme_C85_C85pro-AndroidB-vendor-source/blob/2ab37fb9ceda42cd8679dec8585d4ff5209c4e90/kernel_platform/qcom/proprietary/devicetree/oplus/Moscow-25697-khaje-qrd-nopmi-overlay.dts
