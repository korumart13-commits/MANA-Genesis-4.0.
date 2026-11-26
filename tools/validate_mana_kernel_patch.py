#!/usr/bin/env python3
"""Validate review-only MANA kernel patchsets; never build or flash a kernel."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATCH_DIR = ROOT / "kernel" / "mana-patches"
ALLOWED_NEW_FILE = re.compile(r"^drivers/misc/mana/[A-Za-z0-9_.+-]+$")


def validate_patch(path: Path) -> None:
    text = path.read_text(encoding="utf-8")
    if "GIT binary patch" in text or "Binary files " in text:
        raise ValueError(f"{path.name}: binary patch is forbidden")
    old_paths = re.findall(r"^--- (?:a/)?(.+)$", text, flags=re.MULTILINE)
    new_paths = re.findall(r"^\+\+\+ (?:b/)?(.+)$", text, flags=re.MULTILINE)
    if not old_paths or len(old_paths) != len(new_paths):
        raise ValueError(f"{path.name}: malformed unified diff")
    for old, new in zip(old_paths, new_paths):
        if old != "/dev/null":
            raise ValueError(f"{path.name}: kernel patches may only add new files; existing files are immutable")
        if not ALLOWED_NEW_FILE.fullmatch(new):
            raise ValueError(f"{path.name}: target path is outside drivers/misc/mana/: {new}")


def main() -> int:
    if not PATCH_DIR.is_dir():
        print("No MANA kernel patch directory exists.", file=sys.stderr)
        return 2
    patches = sorted(PATCH_DIR.glob("*.patch"))
    if not patches:
        print("No kernel patchset supplied; kernel track remains review-only.", file=sys.stderr)
        return 2
    for patch in patches:
        validate_patch(patch)
    print(f"Validated {len(patches)} review-only MANA kernel patchset(s).")
    print("No kernel source match, compilation, image generation, or flashing is implied.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (OSError, ValueError) as exc:
        print(f"MANA KERNEL PATCH REJECTED: {exc}", file=sys.stderr)
        raise SystemExit(2)
