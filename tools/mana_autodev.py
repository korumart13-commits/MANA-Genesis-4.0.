#!/usr/bin/env python3
"""Build and test isolated MANA code candidates; never install or flash them.

The runner accepts a unified diff, restricts changed paths by track, applies it
in a detached git worktree, runs a fixed validation command, and leaves a local
candidate branch only when every gate passes. It never pushes or merges.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

TRACKS = {
    "engine": ("engine/mana-brain/src/main/kotlin/", "engine/mana-brain/src/test/kotlin/"),
    "apps": (
        "launcher/app/src/",
        "system/mana/apps/",
        "os/core/src/",
        "game/",
    ),
    "kernel": ("kernel/mana-patches/",),
}


class AutoDevError(RuntimeError):
    pass


def run(args: list[str], cwd: Path, timeout: int = 1800) -> str:
    safe_keys = {
        "PATH", "HOME", "PREFIX", "JAVA_HOME", "ANDROID_HOME",
        "ANDROID_SDK_ROOT", "GRADLE_USER_HOME", "TMPDIR", "LANG",
        "LC_ALL", "USER", "SHELL", "TERM", "LD_LIBRARY_PATH",
    }
    safe_env = {key: value for key, value in os.environ.items() if key in safe_keys}
    safe_env["GIT_TERMINAL_PROMPT"] = "0"
    result = subprocess.run(
        args, cwd=cwd, text=True, stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT, timeout=timeout, check=False,
        env=safe_env,
    )
    if result.returncode:
        raise AutoDevError(
            f"Command failed ({result.returncode}): {' '.join(args)}\n{result.stdout[-12000:]}"
        )
    return result.stdout


def validate_patch_text(patch_text: str) -> None:
    if "GIT binary patch" in patch_text or "Binary files " in patch_text:
        raise AutoDevError("Binary patch content is forbidden")
    if re.search(r"^(?:old mode|new mode|deleted file mode) |^new file mode 120000(?:\s|$)", patch_text, re.MULTILINE):
        raise AutoDevError("File mode changes and symlinks are forbidden")


def validate_paths(track: str, paths: list[str]) -> None:
    if track not in TRACKS:
        raise AutoDevError(f"Unknown development track: {track}")
    if not paths:
        raise AutoDevError("Patch changes no tracked files")
    for raw in paths:
        path = raw.replace("\\", "/")
        if path.startswith("/") or ".." in Path(path).parts:
            raise AutoDevError(f"Unsafe path: {raw}")
        if not any(path.startswith(prefix) for prefix in TRACKS[track]):
            raise AutoDevError(f"{track} track cannot modify: {raw}")
        if track == "engine" and not path.endswith(".kt"):
            raise AutoDevError(f"Engine track accepts Kotlin source/tests only: {raw}")
        if track == "apps" and not (path.endswith((".kt", ".java", ".xml", ".json", ".gltf", ".js", ".md"))):
            raise AutoDevError(f"Apps track file type is not allowlisted: {raw}")
        if track == "kernel" and not path.endswith((".patch", ".md")):
            raise AutoDevError(f"Kernel track accepts reviewable patchsets/docs only: {raw}")


def candidate_commands(track: str, root: Path) -> list[list[str]]:
    if track == "engine":
        return [["gradle", "-p", "engine/mana-brain", "--no-daemon", "clean", "test"]]
    if track == "apps":
        return [["gradle", "-p", "launcher", "--no-daemon",
                 ":app:assembleDebug", ":app:testDebugUnitTest"]]
    return [[sys.executable, "tools/validate_mana_kernel_patch.py"]]


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--patch", required=True, type=Path,
                        help="Unified diff file, supplied by MANA's code-generation stage")
    parser.add_argument("--track", choices=tuple(TRACKS), required=True)
    parser.add_argument("--output-dir", type=Path,
                        default=Path.home() / ".mana" / "autodev-candidates")
    args = parser.parse_args()

    repo = args.repo.resolve()
    patch = args.patch.resolve()
    output = args.output_dir.resolve()
    if not (repo / ".git").exists():
        raise AutoDevError("--repo must be the root of a Git checkout")
    if not patch.is_file() or patch.stat().st_size == 0:
        raise AutoDevError("Patch file is missing or empty")
    if patch.stat().st_size > 2_000_000:
        raise AutoDevError("Patch exceeds the 2 MB safety limit")
    patch_text = patch.read_text(encoding="utf-8")
    validate_patch_text(patch_text)

    status = run(["git", "status", "--porcelain", "--untracked-files=all"], repo)
    if status.strip():
        raise AutoDevError("Repository must be clean before candidate creation; commit or back up local changes first")
    base = run(["git", "rev-parse", "HEAD"], repo).strip()
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%SZ")
    branch = f"mana-candidate-{args.track}-{stamp}"
    output.mkdir(parents=True, exist_ok=True)

    with tempfile.TemporaryDirectory(prefix="mana-autodev-") as temp:
        worktree = Path(temp) / "worktree"
        added = False
        try:
            run(["git", "worktree", "add", "--detach", str(worktree), base], repo)
            added = True
            run(["git", "apply", "--check", str(patch)], worktree)
            run(["git", "apply", "--whitespace=error", str(patch)], worktree)
            changed = run(["git", "diff", "--name-only", base], worktree).splitlines()
            untracked = run(["git", "ls-files", "--others", "--exclude-standard"], worktree).splitlines()
            changed = sorted(set(changed + untracked))
            validate_paths(args.track, changed)

            if args.track == "kernel":
                run([sys.executable, "tools/validate_mana_kernel_patch.py"], worktree)
            else:
                for command in candidate_commands(args.track, worktree):
                    run(command, worktree, timeout=3600)

            run(["git", "diff", "--check", base], worktree)
            run(["git", "switch", "-c", branch], worktree)
            run(["git", "add", "--"] + changed, worktree)
            run(["git", "diff", "--cached", "--check", base], worktree)
            run(["git", "-c", "user.name=MANA AutoDev",
                 "-c", "user.email=autodev@mana.local",
                 "commit", "-m", f"AutoDev candidate: {args.track} {stamp}"], worktree)
            commit = run(["git", "rev-parse", "HEAD"], worktree).strip()
            candidate_patch = run(["git", "format-patch", "--stdout", f"{base}..{commit}"], worktree)
            patch_out = output / f"{branch}.patch"
            patch_out.write_text(candidate_patch, encoding="utf-8")
            print(f"CANDIDATE_READY track={args.track} branch={branch} commit={commit}")
            print(f"PATCH={patch_out}")
            if args.track == "kernel":
                print("KERNEL_STATUS=REVIEW_ONLY; no matching RMX5566RU kernel source is approved")
            print("No push, merge, APK installation, OTA creation, or device flashing was performed.")
        except Exception:
            if added:
                run(["git", "worktree", "remove", "--force", str(worktree)], repo)
            run(["git", "worktree", "prune"], repo)
            # A branch is only created after all validation gates have passed.
            raise
        finally:
            if added and worktree.exists():
                run(["git", "worktree", "remove", "--force", str(worktree)], repo)
            run(["git", "worktree", "prune"], repo)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AutoDevError, OSError, subprocess.TimeoutExpired) as exc:
        print(f"MANA AUTODEV REJECTED: {exc}", file=sys.stderr)
        raise SystemExit(2)
