#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$ROOT/aosp/manifest.lock"
actual="$(git ls-remote "$AOSP_MANIFEST_URL" "refs/tags/$AOSP_MANIFEST_TAG^{}" | awk 'NR==1 {print $1}')"
if [[ -z "$actual" ]]; then
  echo "ERROR: upstream tag $AOSP_MANIFEST_TAG could not be resolved" >&2
  exit 2
fi
if [[ "$actual" != "$AOSP_MANIFEST_COMMIT" ]]; then
  echo "ERROR: AOSP manifest pin mismatch: expected $AOSP_MANIFEST_COMMIT, got $actual" >&2
  exit 3
fi
echo "AOSP manifest pin verified: $AOSP_MANIFEST_TAG ($actual)"
