#!/usr/bin/env bash
set -euo pipefail

ROOT="${GITHUB_WORKSPACE:-$(git rev-parse --show-toplevel)}"
cd "$ROOT"

APK="$ROOT/launcher/app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"

adb wait-for-device
BOOT_COMPLETED="$(adb shell getprop sys.boot_completed | tr -d '\r')"
test "$BOOT_COMPLETED" = "1"

adb install -r "$APK"
adb shell pm path com.mana.launcher
adb shell monkey -p com.mana.launcher -c android.intent.category.LAUNCHER 1
sleep 20
adb shell pidof com.mana.launcher

if adb logcat -d -t 12000 | grep -E 'FATAL EXCEPTION|Fatal signal [0-9]+|Process: com.mana.launcher'; then
  echo "A fatal application crash was detected in the emulator log." >&2
  exit 1
fi

adb shell dumpsys activity activities | grep -E 'com.mana.launcher|mResumedActivity'
echo "PASS: emulator booted, APK installed, MANA launch process observed, no fatal crash signature found."
