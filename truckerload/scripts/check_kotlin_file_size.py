#!/usr/bin/env python3
"""Fail CI when Kotlin production sources exceed the file-size soft limit.

Soft limit: 600 lines (Phase 3 god-file policy).
Ideal target: 350 lines (reported as warnings only).

Files listed in the baseline may exceed 600 up to their frozen cap; they must not
grow, and should be removed from the baseline once under the soft limit.
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

DEFAULT_SOFT_LIMIT = 600
DEFAULT_WARN_LIMIT = 350


def parse_baseline(path: Path) -> dict[str, int]:
    caps: dict[str, int] = {}
    if not path.is_file():
        return caps
    for raw in path.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        parts = line.split()
        if len(parts) != 2:
            raise SystemExit(f"Invalid baseline line (want '<path> <max-lines>'): {raw!r}")
        rel, max_s = parts
        try:
            max_lines = int(max_s)
        except ValueError as exc:
            raise SystemExit(f"Invalid max-lines in baseline: {raw!r}") from exc
        if max_lines < 1:
            raise SystemExit(f"Baseline max-lines must be >= 1: {raw!r}")
        caps[rel.replace("\\", "/")] = max_lines
    return caps


def count_lines(path: Path) -> int:
    # Match `wc -l` (count newline characters only).
    return path.read_bytes().count(b"\n")


def iter_kotlin_sources(root: Path) -> list[Path]:
    main = root / "src" / "main"
    if not main.is_dir():
        raise SystemExit(f"Missing source root: {main}")
    return sorted(p for p in main.rglob("*.kt") if p.is_file())


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--app-dir",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "app",
        help="Android :app module directory",
    )
    parser.add_argument(
        "--baseline",
        type=Path,
        default=Path(__file__).resolve().parents[1] / "config" / "kotlin-file-size-baseline.txt",
        help="Baseline allowlist for existing oversized files",
    )
    parser.add_argument("--soft-limit", type=int, default=DEFAULT_SOFT_LIMIT)
    parser.add_argument("--warn-limit", type=int, default=DEFAULT_WARN_LIMIT)
    args = parser.parse_args(argv)

    app_dir: Path = args.app_dir.resolve()
    baseline = parse_baseline(args.baseline.resolve())
    soft = args.soft_limit
    warn = args.warn_limit

    errors: list[str] = []
    warnings: list[str] = []
    stale_baseline: list[str] = []
    seen_baseline: set[str] = set()

    for path in iter_kotlin_sources(app_dir):
        rel = path.relative_to(app_dir).as_posix()
        lines = count_lines(path)
        cap = baseline.get(rel)

        if cap is not None:
            seen_baseline.add(rel)
            if lines <= soft:
                stale_baseline.append(f"{rel}: {lines} lines (under soft limit {soft}; remove from baseline)")
            elif lines > cap:
                errors.append(
                    f"{rel}: {lines} lines exceeds baseline cap {cap} "
                    f"(soft limit {soft}; split the file or raise the cap only with review)",
                )
            continue

        if lines > soft:
            errors.append(
                f"{rel}: {lines} lines exceeds soft limit {soft} "
                f"(split the file; do not add to baseline without review)",
            )
        elif lines > warn:
            warnings.append(f"{rel}: {lines} lines (ideal target ≤{warn})")

    for rel, cap in sorted(baseline.items()):
        if rel not in seen_baseline:
            errors.append(f"{rel}: listed in baseline (cap {cap}) but file is missing")

    if warnings:
        print(f"File-size warnings ({len(warnings)} files above ideal {warn} LOC):")
        for item in warnings:
            print(f"  WARN  {item}")
        print()

    if stale_baseline:
        print(f"Stale baseline entries ({len(stale_baseline)} — safe to delete):")
        for item in stale_baseline:
            print(f"  INFO  {item}")
        print()

    if errors:
        print(f"File-size gate FAILED ({len(errors)} violation(s); soft limit {soft} LOC):")
        for item in errors:
            print(f"  FAIL  {item}")
        return 1

    print(
        f"OK: Kotlin file-size gate passed "
        f"(soft limit {soft}, ideal {warn}, baseline entries {len(baseline)})",
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
