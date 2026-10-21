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

PHONE_SLIDES = {
    "en": ["dashboard", "widget", "photo", "scanner", "maintenance", "premium", "analytics"],
    "ru": [
        "journal",
        "widget",
        "add_load",
        "camera",
        "scanner",
        "diesel",
        "weekly_goal",
        "telegram",
    ],
}
PHONE_SIZE = (1080, 1920)
TABLET_SIZE = (1920, 1200)
FEATURE_SIZE = (1024, 500)


def chrome_screenshot(url: str, dest: Path, size: tuple[int, int], scale: int = 1) -> None:
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
            f"--force-device-scale-factor={scale}",
            "--allow-file-access-from-files",
            f"--user-data-dir={profile}",
            f"--window-size={w},{h}",
            f"--screenshot={raw}",
            url,
        ]
        subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        img = Image.open(raw).convert("RGB")
        target = (size[0] * scale, size[1] * scale)
        if img.size != target:
            img = img.resize(target, Image.Resampling.LANCZOS)
        dest.parent.mkdir(parents=True, exist_ok=True)
        img.save(dest, "PNG", optimize=True)


def file_url(kind: str, lang: str, slide: str | None = None) -> str:
    q = f"kind={kind}&lang={lang}"
    if slide:
        q += f"&slide={slide}"
    return TEMPLATE.resolve().as_uri() + "?" + q


def main() -> int:
    only = set(sys.argv[1:])
    kinds = {"all", "phone", "tablet", "feature", "en-phone", "ru-phone"}
    slides = only - kinds

    def wants_phone(lang: str) -> bool:
        if not only:
            return True
        if f"{lang}-phone" in only or "phone" in only or "all" in only:
            return True
        # `python3 render_screenshots.py premium` renders that slide only.
        return bool(slides) and only.isdisjoint(kinds)

    jobs: list[tuple[str, Path, tuple[int, int], int]] = []
    for lang, folder in (("ru", "phone-ru"), ("en", "phone-en")):
        if not wants_phone(lang):
            continue
        dest_dir = ROOT / folder
        keep = set()
        for i, slide in enumerate(PHONE_SLIDES[lang], start=1):
            if slides and slide not in slides:
                continue
            name = f"{i:02d}_{slide}.png"
            keep.add(name)
            # English frames are shot at 2× and downsampled so captions stay crisp.
            scale = 2 if lang == "en" else 1
            jobs.append((file_url("phone", lang, slide), dest_dir / name, PHONE_SIZE, scale))
        # A named slide (python3 render_screenshots.py premium) must not delete the rest.
        if not slides and dest_dir.is_dir():
            for stale in dest_dir.glob("*.png"):
                if stale.name not in keep:
                    stale.unlink()
                    print(f"remove {stale.relative_to(ROOT)}", flush=True)
    if not only or "tablet" in only or "all" in only:
        for lang, folder in (("ru", "tablet-10-ru"), ("en", "tablet-10-en")):
            jobs.append((file_url("tablet", lang), ROOT / folder / "01_journal_landscape.png", TABLET_SIZE, 1))
        tablet7 = (1200, 1920)
        for lang, folder in (("ru", "tablet-7-ru"), ("en", "tablet-7-en")):
            jobs.append((file_url("tablet7", lang), ROOT / folder / "01_journal.png", tablet7, 1))
    if not only or "feature" in only or "all" in only:
        jobs.append((file_url("feature", "ru"), ROOT / "feature-graphic-ru.png", FEATURE_SIZE, 1))
        jobs.append((file_url("feature", "en"), ROOT / "feature-graphic-en.png", FEATURE_SIZE, 1))
        jobs.append((file_url("feature", "ru"), ROOT / "feature-graphic-ru-2048x1000.png", FEATURE_SIZE, 2))
        jobs.append((file_url("feature", "en"), ROOT / "feature-graphic-en-2048x1000.png", FEATURE_SIZE, 2))

    for url, dest, size, scale in jobs:
        print(f"render {dest.relative_to(ROOT)}  {size[0]}x{size[1]}", flush=True)
        chrome_screenshot(url, dest, size, scale)
        if scale > 1 and size == PHONE_SIZE:
            sharp = Image.open(dest).convert("RGB")
            if sharp.size != PHONE_SIZE:
                sharp = sharp.resize(PHONE_SIZE, Image.Resampling.LANCZOS)
                sharp.save(dest, "PNG", optimize=True)

    print(f"done: {len(jobs)} images")
    return 0


if __name__ == "__main__":
    sys.exit(main())
