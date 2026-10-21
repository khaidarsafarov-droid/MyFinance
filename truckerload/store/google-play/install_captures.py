#!/usr/bin/env python3
"""Copy Robolectric captures into the listing tree and the docs phone originals.

Source layout: <src>/{en,ru}/*.png  (default /tmp/play-captures)
Phone originals are a top crop to 1080×1920 with no upscaling.
"""
from __future__ import annotations

import shutil
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
DOCS_PHONE = REPO / "docs" / "play-store" / "screenshots" / "phone"

DOCS_FROM_RU = {
    "journal": "01-home.png",
    "add_load": "02-add-load.png",
    "weekly_goal": "03-weekly-goal.png",
    "analytics": "04-analytics.png",
    "weekly_chart": "05-weekly-chart.png",
    "finance": "06-finance.png",
    "journal_list": "07-journal.png",
    "profile": "08-profile.png",
}


def fit_phone(src: Path, dest: Path) -> None:
    im = Image.open(src).convert("RGB")
    if im.width < 1080 or im.height < 1920:
        raise SystemExit(f"{src} is {im.size}; refusing to upscale")
    left = (im.width - 1080) // 2
    im = im.crop((left, 0, left + 1080, 1920))
    dest.parent.mkdir(parents=True, exist_ok=True)
    im.save(dest, "PNG", optimize=True)


def main() -> int:
    src = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/play-captures")
    for lang in ("en", "ru"):
        folder = src / lang
        if not folder.is_dir():
            raise SystemExit(f"missing {folder}")
        dest = ROOT / "captures" / lang
        dest.mkdir(parents=True, exist_ok=True)
        for png in sorted(folder.glob("*.png")):
            shutil.copy2(png, dest / png.name)
            print(f"capture {lang}/{png.name}")
    for key, name in DOCS_FROM_RU.items():
        fit_phone(src / "ru" / f"{key}.png", DOCS_PHONE / name)
        print(f"docs phone {name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
