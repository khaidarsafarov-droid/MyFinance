#!/usr/bin/env python3
"""Fail if string resource key sets differ across values / values-en / values-ru."""
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
FOLDERS = ("values", "values-en", "values-ru")
NAME_RE = re.compile(r'<string\s+name="([^"]+)"')


def keys(folder: str) -> set[str]:
    text = (ROOT / folder / "strings.xml").read_text(encoding="utf-8")
    return set(NAME_RE.findall(text))


def main() -> int:
    sets = {f: keys(f) for f in FOLDERS}
    all_keys = set().union(*sets.values())
    ok = True
    for f in FOLDERS:
        missing = sorted(all_keys - sets[f])
        if missing:
            ok = False
            print(f"{f}: missing {len(missing)} keys")
            for k in missing[:40]:
                print(f"  - {k}")
            if len(missing) > 40:
                print(f"  ... and {len(missing) - 40} more")
    if ok:
        print(f"OK: {len(all_keys)} keys present in all locales")
        return 0
    return 1


if __name__ == "__main__":
    sys.exit(main())
