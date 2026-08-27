#!/usr/bin/env python3
"""Собирает материалы для карточки Google Play из «сырых» скриншотов эмулятора.

Вход  : docs/play-store/screenshots/phone/*.png  (1080x1920, снятые через adb screencap)
Выход : docs/play-store/screenshots/phone-framed/*.png  (1080x1920, с подписями)
        docs/play-store/icon-512.png
        docs/play-store/feature-graphic-1024x500.png

Запуск: python3 scripts/build_play_store_assets.py
Зависимости: Pillow, шрифты Inter (или любой TTF, заданный через --font-dir).
"""
from __future__ import annotations

import argparse
import pathlib
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

REPO = pathlib.Path(__file__).resolve().parents[1]
STORE = REPO / "docs" / "play-store"
RAW = STORE / "screenshots" / "phone"
FRAMED = STORE / "screenshots" / "phone-framed"
ICON_SOURCE = REPO / "app" / "src" / "main" / "res" / "drawable-nodpi" / "ic_launcher_image.png"
LOGO_SOURCE = REPO / "app" / "src" / "main" / "res" / "drawable-nodpi" / "app_logo.png"

# Фирменные цвета из SoftUiColors (presentation/theme/SoftUiTheme.kt).
BRAND = (0x5B, 0x54, 0xE6)
BRAND_DEEP = (0x2E, 0x2A, 0x7A)
BRAND_INK = (0x16, 0x14, 0x1F)

# Подписи к скриншотам: имя файла -> (заголовок, подзаголовок).
CAPTIONS: dict[str, tuple[str, str]] = {
    "01-home": ("Вся неделя\nна одном экране", "Гросс, мили и средний RPM считаются сами"),
    "02-add-load": ("Вставил сообщение —\nгруз разобран", "Ставка, мили, адреса и RPM без ручного ввода"),
    "03-weekly-goal": ("Цель недели\nи ваш темп", "Видно, идёте вы с опережением или отстаёте"),
    "04-analytics": ("Мои цифры\nза 12 недель", "Гросс, пробег, средний RPM и лучшая неделя"),
    "05-weekly-chart": ("График заработка\nпо неделям", "Сразу видно сильные и провальные недели"),
    "06-finance": ("Зарплата и дизель\nрядом", "Галлоны, цена за галлон и экономия на скидке"),
    "07-journal": ("Журнал рейсов\nвсегда под рукой", "Поиск, фильтры по периодам и архив"),
    "08-profile": ("Профиль водителя\nсо статистикой", "Данные хранятся на телефоне, работает офлайн"),
}

FEATURE_TITLE = "TruckoRig"
FEATURE_SUBTITLE = "Журнал грузов и деньги в рейсе"
FEATURE_BULLETS = ["Разбор сообщений Relay и Telegram",
                   "Гросс, мили и RPM за неделю",
                   "Зарплата, дизель и цель недели"]


def load_font(font_dir: pathlib.Path | None, name: str, size: int) -> ImageFont.FreeTypeFont:
    candidates = []
    if font_dir:
        candidates.append(font_dir / name)
    candidates += [
        pathlib.Path("/usr/share/fonts/truetype/macos") / name,
        pathlib.Path("/usr/share/fonts/truetype/inter") / name,
        pathlib.Path("/usr/share/fonts/truetype/dejavu") / "DejaVuSans-Bold.ttf",
    ]
    for path in candidates:
        if path.exists():
            return ImageFont.truetype(str(path), size)
    raise SystemExit(f"Не найден шрифт {name}; укажите --font-dir")


def vertical_gradient(size: tuple[int, int], top: tuple[int, int, int],
                      bottom: tuple[int, int, int]) -> Image.Image:
    width, height = size
    base = Image.new("RGB", (1, height))
    pixels = base.load()
    for y in range(height):
        t = y / max(height - 1, 1)
        pixels[0, y] = tuple(round(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
    return base.resize(size, Image.BICUBIC)


def rounded(image: Image.Image, radius: int) -> Image.Image:
    mask = Image.new("L", image.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, image.width - 1, image.height - 1],
                                           radius=radius, fill=255)
    out = image.convert("RGBA")
    out.putalpha(mask)
    return out


def drop_shadow(canvas: Image.Image, box: tuple[int, int, int, int], radius: int,
                blur: int = 34, alpha: int = 110, offset: tuple[int, int] = (0, 18)) -> None:
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    x0, y0, x1, y1 = box
    ImageDraw.Draw(layer).rounded_rectangle(
        [x0 + offset[0], y0 + offset[1], x1 + offset[0], y1 + offset[1]],
        radius=radius, fill=(0, 0, 0, alpha))
    canvas.alpha_composite(layer.filter(ImageFilter.GaussianBlur(blur)))


def frame_screenshot(shot: Image.Image, title: str, subtitle: str,
                     bold: ImageFont.FreeTypeFont, regular: ImageFont.FreeTypeFont) -> Image.Image:
    width, height = 1080, 1920
    canvas = vertical_gradient((width, height), BRAND, BRAND_DEEP).convert("RGBA")

    # Мягкое световое пятно за «телефоном», чтобы фон не был плоским.
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([-260, 420, width + 260, 1720], fill=(255, 255, 255, 34))
    canvas.alpha_composite(glow.filter(ImageFilter.GaussianBlur(120)))

    draw = ImageDraw.Draw(canvas)
    y = 96
    for line in title.split("\n"):
        draw.text((72, y), line, font=bold, fill=(255, 255, 255))
        y += bold.size + 12
    draw.text((72, y + 10), subtitle, font=regular, fill=(255, 255, 255, 216))

    shot_w = 820
    scaled = shot.convert("RGB").resize(
        (shot_w, round(shot.height * shot_w / shot.width)), Image.LANCZOS)
    x0 = (width - shot_w) // 2
    y0 = height - scaled.height - 20
    drop_shadow(canvas, (x0, y0, x0 + scaled.width, y0 + scaled.height), radius=44)
    canvas.alpha_composite(rounded(scaled, 44), (x0, y0))
    return canvas.convert("RGB")


def build_icon(out: pathlib.Path) -> None:
    """512x512 иконка для Play — тот же кадр, что видно на лаунчере.

    Adaptive-icon: холст 108, foreground с инсетом 21dp (safe zone 66), маска
    показывает центральные 72. Play рисует полный квадрат, поэтому повторяем
    кадрирование маской: foreground занимает 66/72 итогового размера.
    """
    size = 512
    icon = Image.new("RGB", (size, size), BRAND)
    foreground = Image.open(ICON_SOURCE).convert("RGBA")
    inner = round(size * 66 / 72)
    foreground = foreground.resize((inner, inner), Image.LANCZOS)
    offset = (size - inner) // 2
    icon.paste(foreground, (offset, offset), foreground)
    icon.save(out, "PNG")


def build_feature_graphic(out: pathlib.Path, font_dir: pathlib.Path | None) -> None:
    width, height = 1024, 500
    canvas = vertical_gradient((width, height), BRAND, BRAND_INK).convert("RGBA")

    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([620, -160, 1180, 500], fill=(255, 255, 255, 52))
    canvas.alpha_composite(glow.filter(ImageFilter.GaussianBlur(90)))

    logo_size = 252
    logo = Image.open(LOGO_SOURCE).convert("RGBA").resize((logo_size, logo_size), Image.LANCZOS)
    canvas.alpha_composite(rounded(logo, logo_size // 4), (742, (height - logo_size) // 2))

    title = load_font(font_dir, "Inter-Bold.ttf", 78)
    subtitle = load_font(font_dir, "Inter-Medium.ttf", 33)
    bullet = load_font(font_dir, "Inter-Regular.ttf", 26)

    draw = ImageDraw.Draw(canvas)
    draw.text((64, 96), FEATURE_TITLE, font=title, fill=(255, 255, 255))
    draw.text((66, 196), FEATURE_SUBTITLE, font=subtitle, fill=(255, 255, 255, 226))
    y = 272
    for line in FEATURE_BULLETS:
        draw.ellipse([70, y + 10, 82, y + 22], fill=(0xA2, 0x9B, 0xFE))
        draw.text((100, y), line, font=bullet, fill=(255, 255, 255, 210))
        y += 46
    canvas.convert("RGB").save(out, "PNG")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--font-dir", type=pathlib.Path, default=None,
                        help="каталог с Inter-Bold.ttf / Inter-Medium.ttf / Inter-Regular.ttf")
    args = parser.parse_args()

    if not RAW.is_dir():
        raise SystemExit(f"Нет каталога с исходными скриншотами: {RAW}")

    bold = load_font(args.font_dir, "Inter-Bold.ttf", 62)
    regular = load_font(args.font_dir, "Inter-Regular.ttf", 34)

    FRAMED.mkdir(parents=True, exist_ok=True)
    made = 0
    for path in sorted(RAW.glob("*.png")):
        caption = CAPTIONS.get(path.stem)
        if caption is None:
            print(f"пропуск (нет подписи): {path.name}")
            continue
        shot = Image.open(path)
        if shot.size != (1080, 1920):
            raise SystemExit(f"{path.name}: ожидается 1080x1920, получено {shot.size}")
        frame_screenshot(shot, caption[0], caption[1], bold, regular).save(
            FRAMED / path.name, "PNG")
        made += 1
        print(f"собран {FRAMED.relative_to(REPO)}/{path.name}")

    build_icon(STORE / "icon-512.png")
    print(f"собран {(STORE / 'icon-512.png').relative_to(REPO)}")
    build_feature_graphic(STORE / "feature-graphic-1024x500.png", args.font_dir)
    print(f"собран {(STORE / 'feature-graphic-1024x500.png').relative_to(REPO)}")
    print(f"готово: {made} скриншотов")
    return 0


if __name__ == "__main__":
    sys.exit(main())
