#!/usr/bin/env python3
"""Recolor shield logo PNGs to the Daily Task Tracker kit palette (SoftUiTheme.kt).

Kit chrome: #5B54E6 primary, #48C9B0 mint, #FFB74D orange, sage plate #EEEDFF.
"""
from __future__ import annotations

import colorsys
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
DESIGN = ROOT / "design"
RES = ROOT / "app" / "src" / "main" / "res"

# SoftUiColors / kit tokens
KIT_PRIMARY = (0x5B, 0x54, 0xE6)
KIT_PRIMARY_DARK = (0x48, 0x44, 0xC4)
KIT_MINT = (0x48, 0xC9, 0xB0)
KIT_ORANGE = (0xFF, 0xB7, 0x4D)
SAGE = (0xEE, 0xED, 0xFF)
WHITE = (0xFF, 0xFF, 0xFF)
OLD_NAVY = (0x14, 0x38, 0x82)


def _dist(a: tuple[int, int, int], b: tuple[int, int, int]) -> float:
    return sum((x - y) ** 2 for x, y in zip(a, b)) ** 0.5


def _set_luminance(rgb: tuple[int, int, int], target_v: float) -> tuple[int, int, int]:
    h, _s, v = colorsys.rgb_to_hsv(rgb[0] / 255, rgb[1] / 255, rgb[2] / 255)
    s = max(_s, 0.15)
    r, g, b = colorsys.hsv_to_rgb(h, s, target_v)
    return int(r * 255), int(g * 255), int(b * 255)


def recolor_pixel(r: int, g: int, b: int, a: int) -> tuple[int, int, int, int]:
    if a < 16:
        return r, g, b, a

    if _dist((r, g, b), OLD_NAVY) < 40:
        return *KIT_PRIMARY, a

    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    hue = h * 360

    if v > 0.82 and s < 0.12:
        return *WHITE, a
    if v > 0.68 and s < 0.25:
        return *_set_luminance(SAGE, v), a

    # Diesel nozzle — kit orange (before dark-green plate mapping)
    if 20 <= hue <= 55 and s > 0.30 and v > 0.30:
        return *_set_luminance(KIT_ORANGE, min(v + 0.02, 0.98)), a

    # Cargo / card — kit mint (saturation + brightness; skip dark forest plate)
    if 70 <= hue <= 200 and s > 0.35 and v >= 0.45:
        return *_set_luminance(KIT_MINT, v), a

    # Dark plate / shield (legacy forest green or navy) → kit purple
    if v < 0.52 and s > 0.06:
        if v < 0.22:
            return *KIT_PRIMARY_DARK, a
        return *_set_luminance(KIT_PRIMARY, max(v, 0.28)), a

    return r, g, b, a


def recolor_image(path: Path) -> Image.Image:
    im = Image.open(path).convert("RGBA")
    out = Image.new("RGBA", im.size)
    px_in = im.load()
    px_out = out.load()
    for y in range(im.height):
        for x in range(im.width):
            px_out[x, y] = recolor_pixel(*px_in[x, y])
    return out


def save_png(img: Image.Image, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path, format="PNG", optimize=True)


def write_mipmaps(source: Image.Image) -> None:
    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for folder, size in sizes.items():
        scaled = source.resize((size, size), Image.Resampling.LANCZOS)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            save_png(scaled, RES / folder / name)


def main() -> int:
    src = DESIGN / "app_logo_source.png"
    if not src.is_file():
        print(f"Missing source: {src}", file=sys.stderr)
        return 1

    logo = recolor_image(src)
    save_png(logo, DESIGN / "app_logo_source.png")
    save_png(logo, RES / "drawable-nodpi" / "app_logo.png")

    launcher = logo.resize((512, 512), Image.Resampling.LANCZOS)
    save_png(launcher, RES / "drawable-nodpi" / "ic_launcher_image.png")
    write_mipmaps(launcher)
    print("Recolored app icon assets to kit palette (#5B54E6).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
