#!/usr/bin/env bash
set -Eeuo pipefail

# Creates an AOSP Android 16 source checkout and attaches the pinned MANA tree.
# This prepares a build workspace only; it does not create a device ROM or flash images.

AOSP_URL="https://android.googlesource.com/platform/manifest"
AOSP_BRANCH="android16-release"
MANA_MANIFEST_URL="https://raw.githubusercontent.com/korumart13-commits/MANA-Genesis-4.0./main/platform/local_manifests/mana.xml"
MIN_FREE_GIB="${MANA_MIN_FREE_GIB:-250}"
JOBS="${MANA_SYNC_JOBS:-8}"
TOP="${1:-$PWD/aosp-mana}"

die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"; }

[[ "$(uname -s)" == Linux ]] || die "AOSP build workspace must be prepared on Linux."
need repo
need git
need curl
need python3
need df
[[ "$JOBS" =~ ^[1-9][0-9]*$ ]] || die "MANA_SYNC_JOBS must be a positive integer."
[[ "$MIN_FREE_GIB" =~ ^[1-9][0-9]*$ ]] || die "MANA_MIN_FREE_GIB must be a positive integer."

mkdir -p "$TOP"
TOP="$(cd "$TOP" && pwd)"
cd "$TOP"
FREE_KIB="$(df -Pk "$TOP" | awk 'NR==2 {print $4}')"
MIN_KIB=$((MIN_FREE_GIB * 1024 * 1024))
(( FREE_KIB >= MIN_KIB )) || die "Need at least ${MIN_FREE_GIB} GiB free at $TOP; currently $((FREE_KIB / 1024 / 1024)) GiB."

if [[ ! -d "$TOP/.repo" ]]; then
  repo init -u "$AOSP_URL" -b "$AOSP_BRANCH" --partial-clone --clone-filter=blob:limit=10M -g default,-mips
fi

mkdir -p "$TOP/.repo/local_manifests"
TMP_MANIFEST="$TOP/.repo/local_manifests/mana.xml.tmp"
curl --fail --location --silent --show-error "$MANA_MANIFEST_URL" -o "$TMP_MANIFEST"
grep -q 'revision="071ec83e"' "$TMP_MANIFEST" || die "MANA manifest does not contain the expected tested source pin."
mv "$TMP_MANIFEST" "$TOP/.repo/local_manifests/mana.xml"

repo sync -c -j"$JOBS" --no-tags

MANIFEST_COMMIT="$(git -C .repo/manifests rev-parse HEAD)"
MANA_COMMIT="$(git -C vendor/mana/genesis rev-parse HEAD)"
[[ "$MANA_COMMIT" == 071ec83e* ]] || die "Synced MANA tree is not the expected immutable source snapshot."

python3 - "$TOP" "$AOSP_BRANCH" "$MANIFEST_COMMIT" "$MANA_COMMIT" <<'PY'
import json, pathlib, sys
top, branch, aosp_commit, mana_commit = sys.argv[1:]
lock = {
    "aosp_manifest_url": "https://android.googlesource.com/platform/manifest",
    "aosp_branch": branch,
    "aosp_manifest_commit": aosp_commit,
    "mana_repository": "https://github.com/korumart13-commits/MANA-Genesis-4.0.",
    "mana_commit": mana_commit,
    "device_product": None,
    "device_images_built": False,
    "flashable": False,
}
path = pathlib.Path(top) / "mana-build-lock.json"
path.write_text(json.dumps(lock, indent=2) + "\n", encoding="utf-8")
print(f"Wrote reproducibility lock: {path}")
PY

printf '\nAOSP Android 16 sources synced.\n'
printf 'AOSP manifest: %s\nMANA source:   %s\n' "$MANIFEST_COMMIT" "$MANA_COMMIT"
printf 'Next: inspect available lunch targets and device integration prerequisites. No ROM image has been built.\n'
