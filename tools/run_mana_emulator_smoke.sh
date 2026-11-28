#!/usr/bin/env bash
set -euo pipefail

WORKSPACE="${GITHUB_WORKSPACE:?GITHUB_WORKSPACE is required}"
APK="$WORKSPACE/launcher/app/build/outputs/apk/debug/app-debug.apk"

gradle --no-daemon -p "$WORKSPACE/launcher" :app:connectedDebugAndroidTest --stacktrace
test -s "$APK"
adb wait-for-device
for attempt in $(seq 1 120); do
  BOOT_COMPLETED="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [ "$BOOT_COMPLETED" = "1" ]; then break; fi
  sleep 5
done
test "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1"
adb install -r "$APK"
adb shell pm path com.mana.launcher
adb shell monkey -p com.mana.launcher -c android.intent.category.LAUNCHER 1
for attempt in $(seq 1 30); do
  PID="$(adb shell pidof com.mana.launcher 2>/dev/null | tr -d '\r' || true)"
  if [ -n "$PID" ]; then break; fi
  sleep 2
done
test -n "${PID:-}"
LOG="$(adb logcat -d -t 12000)"
if printf '%s\n' "$LOG" | grep -A 8 -B 2 -E "FATAL EXCEPTION|Fatal signal"; then
  if printf '%s\n' "$LOG" | grep -A 8 -B 2 -E "FATAL EXCEPTION|Fatal signal" | grep -q "com.mana.launcher"; then
    echo "MANA process fatal crash detected." >&2
    exit 1
  fi
fi
adb shell dumpsys activity activities | grep -E 'com.mana.launcher|mResumedActivity'
echo "PASS: emulator booted, four instrumentation tests passed, APK installed, MANA process observed, no MANA fatal crash signature found."
