# MANA OS — lock screen, boot identity and device integration

Target: realme RMX5566RU / RE645F, Android 16, Qualcomm SM6225 (bengal).
This is an implementation contract for an OEM-preserving firmware update, not a claim that a flashable image already exists.

## 1. Lock screen — required visual design

The lock screen must remain the real Android/Oplus Keyguard. Do not draw a fake lock screen over the secure one and do not intercept PIN, password, fingerprint, face unlock, emergency calling or trusted-device flows.

MANA visual specification:
- Full-bleed wallpaper with a dark navy readability gradient; wallpaper remains user-changeable.
- Large adaptive clock in the upper-middle area; date and optional weather below it (weather only when a configured provider and permission exist).
- MANA cyan accent (#54C8F5), navy surfaces (#071426 / #102B50), white-blue text (#EAF4FF), muted secondary text (#91A9C5).
- Notification cards use soft translucent/matte surfaces, clear app identity, grouped alerts and readable contrast. Respect private-notification settings and hide sensitive content when Android says so.
- Bottom shortcuts remain the platform-configured camera/phone/emergency affordances; do not hardcode a shortcut that breaks OEM emergency access.
- Media controls use the same MANA card style. Biometrics, privacy indicators, charging status and accessibility affordances remain visible.
- Always-on display / ambient mode must use a separate low-power layout and obey proximity, burn-in and OEM power policies.
- Animation: short fade/slide only; no continuous GPU animation while idle.
- Layout must support portrait, landscape where OEM Keyguard supports it, font scaling, display cutouts and accessibility.

Implementation route:
1. Extract and inspect the Keyguard/SystemUI APKs from the exact installed RMX5566RU build.
2. Identify overlayable resources and current Oplus overlay priorities.
3. Build a target-specific RRO for colors, dimensions, typography and supported layout resources.
4. If a desired layout is not overlayable, use a maintained source integration; never replace the credential verifier.
5. Verify PIN, password, fingerprint, face, emergency, notification privacy, AOD, charging and reboot flows on-device.

## 2. Power-on identity and boot animation

There are two visually distinct boot stages:
- **Bootloader / verified-boot splash:** often drawn by the bootloader before Android starts. On a locked, AVB-verified Realme device it is not safely replaceable by an ordinary Android overlay. Keep the OEM splash unless an official, device-specific and correctly signed mechanism is verified. Do not unlock or patch the bootloader just to change a logo.
- **Android boot animation:** after the kernel and Android userspace start, replace the standard animation with an original MANA OS animation, installed through the correct product media location for this exact build.

Animation art direction:
- 2–3 seconds, dark navy background (#071426).
- Centered original MANA wordmark, subtle cyan ring / light sweep, restrained particles.
- Text: “MANA OS” and optional small “Android 16 platform” only in engineering builds; no Realme branding in the Android animation.
- No fake progress percentage. Respect boot animation service timing and use a small, optimized asset.
- Keep a static MANA shutdown animation optional only if the OEM supports it safely.

Do not claim the Realme bootloader splash is replaced merely because the Android boot animation is MANA-branded.

## 3. Preserve the existing firmware

The update must be based on the exact compatible RMX5566RU Android 16 firmware, not a generic ROM or a different RMX model. Preserve:
- Qualcomm SM6225/bengal kernel, GPU, DSP, camera, audio, modem, Wi-Fi/Bluetooth, GNSS, NFC (if present), sensors, fingerprint and charging stack;
- Realme/Oplus HALs, vendor services, init configuration, firmware partitions, VINTF compatibility and dynamic-partition layout;
- all stock Realme system apps, Play/Google components if present, OEM services and user-installed apps/data.

MANA packages are additive. No broad PRODUCT_PACKAGES removal, no data wipe, no replacement of vendor blobs with guessed binaries.

## 4. Device integration for the standalone MANA app

The AI remains exclusively in standalone package `com.mana.genesis`. Firmware contributes a narrow, auditable integration layer; it does not duplicate the AI runtime.

### Integration layers
- **Public Android APIs first:** default assistant role / VoiceInteractionService where supported, accessibility only with explicit user enablement, notification listener only with explicit consent, media/session APIs, intents, app links, role managers and standard runtime permissions.
- **MANA Device Bridge:** authenticated Binder/AIDL service exposing purpose-specific operations, not unrestricted shell/root access. Each method checks caller UID, package name, signing certificate, user/profile, permission and current user consent.
- **HAL/vendor compatibility adapters:** use existing Realme/Oplus and Qualcomm HALs. Add adapter code only where a documented Android HAL interface and the exact device implementation are known. Do not invent proprietary drivers or replace vendor binaries.
- **Optional privileged permissions:** only for a separately signed, explicitly allowlisted system integration package on the correct partition. Signature permissions, privapp-permissions allowlists, SELinux domains and AppOps must be reviewed together.
- **Driver/kernel work:** no new kernel driver is justified for ordinary app, notification, media, launcher or settings control. A new driver is considered only for a demonstrated missing hardware capability, with matching kernel source, device tree, vendor interface, firmware, ABI and test evidence.

### Minimum bridge capabilities
- Read device/build/capability state without exposing secrets.
- Launch apps and open supported system panels.
- Read permitted notification metadata and control media sessions after user opt-in.
- Request supported toggles through public APIs or QS tile APIs; report unsupported actions instead of pretending success.
- Manage user-approved files through Storage Access Framework / MediaStore.
- Expose hardware capability descriptors for camera, audio, sensors, Bluetooth, Wi-Fi and location without bypassing Android permission checks.
- Provide versioned capability discovery, structured errors, audit events, rate limits and revocation.

### Security invariants
- Keep SELinux enforcing, AVB/Verified Boot and platform permission checks.
- No hidden root shell, no universal permission grant, no credential capture, no silent microphone/camera/location access.
- User can revoke each sensitive capability. Every privileged call is attributable to the calling package and logged.
- Fail closed if package signature, UID, device build or permission state is not verified.

## 5. Acceptance checklist

- [ ] Android boot animation displays MANA OS on the exact stock build.
- [ ] Bootloader splash limitation is documented; no unsafe bootloader modification.
- [ ] Real Keyguard visually matches MANA theme and still passes all credential/biometric/emergency tests.
- [ ] Quick Settings, notifications, volume, recents, Settings and Files retain OEM behavior and all existing entries.
- [ ] Every stock and user app remains installed; no user data is removed.
- [ ] MANA Device Bridge authenticates only the intended signed client and enforces per-capability consent.
- [ ] VINTF, SELinux, AVB, OTA and rollback checks pass on the target device.
- [ ] A full target-specific build and on-device test pass before any update package is labelled installable.

## 6. Current boundary

This repository currently contains source modules and design/integration plans. It does not yet contain the exact stock RMX5566RU Android 16 proprietary package set, complete device tree/vendor build inputs, a validated Keyguard/SystemUI overlay, a finished bootanimation.zip or a flashable OTA. Do not publish or flash an artifact until all acceptance gates pass.
