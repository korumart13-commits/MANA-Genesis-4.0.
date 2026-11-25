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
