# MANA Android action bridge

Status: implementation added; Android API bridge is available to the launcher process, but the conversational action planner and owner-facing capability-grant UI are not yet wired end-to-end.

## What this adapter actually does

`ManaAndroidActionAdapter` maps the typed requests accepted by `ManaAutonomyController` to documented public Android APIs:

| MANA action | Android API | Additional gate |
| --- | --- | --- |
| `launch_app` | `PackageManager.getLaunchIntentForPackage` + `startActivity` | Launchable/visible package |
| `set_torch` | `CameraManager.setTorchMode` | CAMERA runtime permission; flash hardware |
| `set_brightness` | `Settings.System.putInt` | User-granted Modify system settings special access |
| `set_volume` | `AudioManager.setStreamVolume` | Allowlisted audio streams and per-stream bounds |
| `set_do_not_disturb` | `NotificationManager.setInterruptionFilter` | User-granted Notification Policy Access; controller owner-confirmation gate |

The adapter only registers capabilities explicitly passed by the caller to `createController(ownerEnabledResources)`. An empty set grants no device actions. Unknown capabilities are rejected. The controller continues to enforce typed allowlisted parameters, principal identity, risk confirmation and default-deny security grants.

## Integration contract

The launcher must obtain the owner's explicit choice for each capability before calling:

```kotlin
val adapter = ManaAndroidActionAdapter(applicationContext)
val controller = adapter.createController(ownerEnabledResources = explicitlyEnabledResources)
```

The caller must separately request Android runtime/special permissions using the platform UI, and must handle `SecurityException`, missing hardware and denied special access. The adapter rechecks access immediately before each operation. Do not pre-grant all resources at first launch.

The next integration task is to add an owner-facing capability screen, persist only the owner's selections, reconcile them against current Android grants on every process start, then expose a constrained action proposal interface to MANA Brain. The LLM must produce typed proposals, never executable code. Disruptive actions must pass the existing owner-confirmation gate.

## Deliberate limitations

- This is an ordinary-APK adapter, not a privileged system service, Device Owner agent, AccessibilityService, root bridge or SELinux policy.
- It does not control cellular/network radios, arbitrary app internals, lock-screen/security settings, bootloader, partitions or firmware.
- Firmware installation remains exclusively in the separate signed, device-matched OTA admission path.
- Android 16 and the reported RMX5566RU product are runtime targets, not proof of hardware compatibility. Camera flash, OEM restrictions, special-access screens and background execution must be tested on the physical device.
- This code does not turn the APK into a replacement operating system. Privileged integration requires a correctly built AOSP product and device-specific vendor/kernel/AVB artifacts.

## Verification

The Android source must be compiled by the repository's Android launcher workflow. JVM policy tests do not validate Android framework calls or OEM behavior. Physical-device validation is still required for every listed API.


## Owner-facing controls screen

The Android launcher now exposes **Управление функциями телефона** from its home screen. The screen is implemented by `ManaDeviceControlsActivity` and routes each operation through `ManaAutonomyController` and `ManaAndroidActionAdapter`.

- All five capabilities start disabled and are stored as explicit owner choices.
- Camera permission is requested only when torch control is enabled.
- Modify-system-settings access is requested only when brightness control is enabled.
- Notification Policy Access is requested only when DND control is enabled.
- Launch-app and audio-volume controls require the owner to enable their capability switch first.
- DND activation presents a separate confirmation dialog.
- The activity rechecks Android grants when returning from system settings; revoked or absent access does not become an active MANA capability.
- This remains an ordinary app-level integration. It does not grant root, shell, Device Owner, privileged Binder access, or firmware control.

The UI currently executes explicit owner-button actions through the policy controller. It is not yet a natural-language-to-action pipeline from MANA Brain; that must be added as a typed proposal stage with independent owner confirmation where required.


## Direct owner command path

The launcher also routes a deliberately small set of exact owner phrases through `ManaOwnerCommandParser` before sending other text to MANA Brain. Current supported forms are torch on/off, DND on/off, brightness 0–255, and app launch by explicit package ID. Unrecognized text is not treated as a device action. Parsed requests are passed to the non-exported controls activity, shown in a confirmation dialog, and still require the matching capability to have been enabled. Model-generated prose is not accepted as an executable action request.
