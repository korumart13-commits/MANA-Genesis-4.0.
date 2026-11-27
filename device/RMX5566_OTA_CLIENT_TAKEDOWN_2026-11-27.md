# RMX5566 OTA client availability update — 2026-11-27

## Verified result from the owner's Termux session

The owner ran the documented install attempt for `R0rt1z2/realme-ota`. GitHub returned:

```
remote: Repository unavailable due to DMCA takedown.
remote: See the takedown notice for more details:
remote: https://github.com/github/dmca/blob/master/2026/09/2026-09-01-coloros.md.
fatal: Authentication failed
```

The subsequent codeload URL for the `master` branch returned HTTP 404. Therefore the previously documented installation path is no longer available and must not be recommended as a working path. The authentication prompt is not a request for the owner's credentials; do not enter a GitHub username, password, token, IMEI or GUID.

## Current implications

- `realme-ota` was not installed; `realme-ota` command returned "command not found".
- Installing Python dependencies alone does not provide the missing CLI.
- No OTA query was executed and no package URL, target build, component list, size or checksum was obtained.
- Do not use unverified mirrors, repackaged copies or forks of the removed client.
- Do not claim that a full stock package exists for `RMX5566RU` / `RMX5566_16.0.5.1310(EX01)`.

## Exact owner-provided query identity (retained)

- Product: `RMX5566RU`
- Current OTA version: `RMX5566_11.F.03_2030_202610170441`
- NV ID: `00110111`
- realme UI: `V7.0`
- Oplus ROM: `V16.0.0`
- Display build: `RMX5566_16.0.5.1310(EX01)`

The earlier EU region value was only a first research hypothesis, not a confirmed sales-channel identity.

## Next safe research route

1. Search for direct, current official realme support / service recovery options for the exact regional model and build.
2. Continue checking public catalog metadata only where the endpoint is reachable and its provenance can be established. A model-only match is not sufficient.
3. If an official OTA metadata endpoint or package is found, preserve the exact response provenance and validate model, region/NV ID, source/target build, package type, size, checksums, signature, payload manifest and rollback metadata.
4. Distinguish incremental/component OTA from a complete factory restore package.
5. Keep device image creation blocked until exact device tree, kernel source/configuration, DTBO and matching vendor inputs are verified, and a tested stock recovery path exists.

No download, installation, bootloader unlock, partition write, firmware downgrade, AVB bypass or flash operation was performed.

## Correction to prior project notes

The install command referencing `pip install git+https://github.com/R0rt1z2/realme-ota` and the corresponding codeload ZIP must be treated as obsolete after the reported DMCA takedown. This note supersedes those operational instructions.
