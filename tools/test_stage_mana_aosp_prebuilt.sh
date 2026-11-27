#!/usr/bin/env bash
set -euo pipefail
SCRIPT="tools/stage_mana_aosp_prebuilt.sh"
bash -n "$SCRIPT"
grep -F 'android_app_import {' "$SCRIPT"
grep -F 'presigned: false' "$SCRIPT"
grep -F 'certificate: "platform"' "$SCRIPT"
grep -F 'privileged: false' "$SCRIPT"
! grep -F 'privileged: true' "$SCRIPT"
grep -F 'com.mana.launcher' "$SCRIPT"
grep -F 'ManaEngineBridgeService' "$SCRIPT"
grep -F 'com.mana.permission.BIND_BRAIN' "$SCRIPT"
grep -F 'mana-source-backup' "$SCRIPT"
echo "MANA AOSP prebuilt staging script static checks passed."
