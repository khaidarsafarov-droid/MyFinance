#!/usr/bin/env python3
"""Render Google Play listing images from templates/screenshots.html.

Outputs 24-bit RGB PNGs (no alpha) at Play Console sizes:
  phone  1080×1920
  10" tablet landscape  1920×1200
  feature graphic  1024×500
"""
from __future__ import annotations

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent
TEMPLATE = ROOT / "templates" / "screenshots.html"
# Prefer the real binary. /usr/local/bin/google-chrome is a wrapper that
# reuses --user-data-dir and --remote-debugging-port, which hangs headless shots.
CHROME_CANDIDATES = [
    "/opt/google/chrome/google-chrome",
    "/usr/bin/google-chrome-stable",
    shutil.which("google-chrome-stable"),
    shutil.which("chromium"),
]
CHROME = next((p for p in CHROME_CANDIDATES if p and Path(p).exists()), None)

PHONE_SLIDES = [
    "journal",
    "widget",
    "add_load",
    "camera",
    "scanner",
    "diesel",
    "weekly_goal",
    "telegram",
]
PHONE_SIZE = (1080, 1920)
TABLET_SIZE = (1920, 1200)
FEATURE_SIZE = (1024, 500)


def chrome_screenshot(url: str, dest: Path, size: tuple[int, int]) -> None:
    if not CHROME:
        raise SystemExit("google-chrome is required to render Play screenshots")
    w, h = size
    with tempfile.TemporaryDirectory() as tmp:
        raw = Path(tmp) / "shot.png"
        profile = Path(tmp) / "profile"
        cmd = [
            CHROME,
            "--headless=new",
            "--no-sandbox",
            "--disable-gpu",
            "--disable-dev-shm-usage",
            "--hide-scrollbars",
            "--no-first-run",
            "--force-device-scale-factor=1",
            "--allow-file-access-from-files",
            f"--user-data-dir={profile}",
            f"--window-size={w},{h}",
            f"--screenshot={raw}",
            url,
        ]
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        img = Image.open(raw).convert("RGB")
        if img.size != size:
            img = img.resize(size, Image.Resampling.LANCZOS)
        dest.parent.mkdir(parents=True, exist_ok=True)
        img.save(dest, "PNG", optimize=True)


def file_url(kind: str, lang: str, slide: str | None = None) -> str:
    q = f"kind={kind}&lang={lang}"
    if slide:
        q += f"&slide={slide}"
    return TEMPLATE.resolve().as_uri() + "?" + q


def main() -> int:
    jobs: list[tuple[str, Path, tuple[int, int]]] = []
    for lang, folder in (("ru", "phone-ru"), ("en", "phone-en")):
        for i, slide in enumerate(PHONE_SLIDES, start=1):
            name = f"{i:02d}_{slide}.png"
            jobs.append((file_url("phone", lang, slide), ROOT / folder / name, PHONE_SIZE))
    for lang, folder in (("ru", "tablet-10-ru"), ("en", "tablet-10-en")):
        jobs.append((file_url("tablet", lang), ROOT / folder / "01_journal_landscape.png", TABLET_SIZE))
    jobs.append((file_url("feature", "ru"), ROOT / "feature-graphic-ru.png", FEATURE_SIZE))
    jobs.append((file_url("feature", "en"), ROOT / "feature-graphic-en.png", FEATURE_SIZE))

    for url, dest, size in jobs:
        print(f"render {dest.relative_to(ROOT)}  {size[0]}x{size[1]}", flush=True)
        chrome_screenshot(url, dest, size)

    print(f"done: {len(jobs)} images")
    return 0


if __name__ == "__main__":
    sys.exit(main())
