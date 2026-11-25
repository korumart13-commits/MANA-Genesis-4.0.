# Google Play Data safety — implementation checklist (draft)

**Not ready for submission.** This is a source-code review aid, not a completed Play Console declaration. Recheck the final signed artifact, SDK behavior, all provider contracts and the current Play Console form before submitting.

| Data category | Current code path | Intended purpose | Current handling / declaration note |
|---|---|---|---|
| Contacts | READ_CONTACTS; local caller-number lookup | Identify known callers for Smart Receptionist | On-device lookup; no address-book upload implemented |
| Audio | RECORD_AUDIO; Android speech recognition intent | User-triggered voice input | Recognition service may process audio on-device or remotely; MANA stores transcript only when user saves it; verify actual recognizer behavior |
| Phone numbers and call history | CallScreeningService, ROLE_DIALER, READ_CALL_LOG | Screen unknown callers, optional call UI and user-requested call history | Call number is checked locally against contacts; call history is displayed locally only after user action; no call audio recording or server upload |
| Files and documents | Storage Access Framework selected tree | User-requested file inventory and reversible quarantine | Only the folder selected by the user; metadata and quarantine processing remain local; deletion requires separate confirmation |
| Nearby devices | BLUETOOTH_CONNECT; paired-device list | Show already paired watches, earbuds and car systems | Reads paired-device names/classes locally; no hidden scanning or remote control unless separately implemented |
| App activity / usage | PACKAGE_USAGE_STATS (user-granted special access) | Detect foreground game sessions | Local package/session detection; verify exact fields read and no upload |
| User-generated content | Diary, memory, future-update ideas | Local assistant features | App-private encrypted files; no cloud sync; user controls save/delete |
| App info and performance | Local model runtime and device compatibility checks | Run local inference and diagnostics | No analytics SDK observed in Gradle dependencies; verify final transitive SDKs and logs |
| Other user content and search queries | Prompt text for cloud inference; user-entered adaptive-learning topics | Answer requests and perform opt-in source discovery | Inference prompts require per-request confirmation; daily search topics are sent to Yandex only after adaptive-learning opt-in and on unmetered network; results are stored locally |
| Device or other IDs | No collection implemented intentionally | None | Verify final network traffic and third-party SDKs |

## Permissions and user-facing disclosures

- `SYSTEM_ALERT_WINDOW`: floating avatar; user explicitly enables special access.
- `RECORD_AUDIO`: voice input only after user action.
- `READ_CONTACTS`: caller name matching only; no upload.
- `READ_CALL_LOG`: only when the user opens call history; local display only.
- `CALL_PHONE`: only when the user presses the dial button to place a call.
- `BLUETOOTH_CONNECT`: list already paired devices; no hidden scanning.
- `POST_NOTIFICATIONS`: unknown caller / foreground service notifications; request runtime permission on Android 13+.
- `PACKAGE_USAGE_STATS`: optional game session detection via Settings special access.
- `INTERNET`: model downloads, user-enabled cloud inference and official Yandex/VK API calls.
- Call screening service: user must select MANA in Android's call-screening role flow.
- Bluetooth permissions must be listed only if/when the paired-device module is merged and runtime behavior is verified.

## Required release checks

1. Inspect the final merged manifest and all transitive SDK manifests.
2. Run a network capture for local-only mode, cloud opt-in, model download, Yandex/VK connectors, and voice input.
3. Verify no cloud call occurs without the per-request confirmation.
4. Verify no contacts, diary, memory or future-update content is included in cloud prompts.
5. Verify API keys are not present in logs, crash reports, analytics, or exported files.
6. Test delete/export flows and document provider-side deletion limitations.
7. Complete Play Console Data safety answers based on the actual release artifact, not this draft.
8. Publish a stable public privacy-policy URL and fill the operator contact details.
9. Review call-screening, accessibility, foreground-service and overlay policies for the actual release.
10. Obtain a legal review before publishing.
