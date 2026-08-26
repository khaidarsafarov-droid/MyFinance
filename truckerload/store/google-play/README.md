# Google Play listing assets

Ready-to-upload images for the TruckoRig Play Console listing.

Regenerate after UI copy or palette changes:

```bash
cd truckerload/store/google-play
python3 render_screenshots.py
```

Requires Google Chrome (headless) and Pillow. Fonts and the launcher mark are copied from `app/src/main/res`.

These are **marketing frames** around UI that matches the current kit palette (`SoftUiColors`, purple `#5B54E6`) and the real 3-tab phone nav (Loads / Goal / Profile). Demo numbers are illustrative, not a live device dump.

## Specs (Play Console)

| Asset | Size | Format | Count |
| --- | --- | --- | --- |
| Phone screenshots | 1080×1920 (9:16) | 24-bit PNG, no alpha | 8 (max) |
| 10" tablet | 1920×1200 (16:9) | 24-bit PNG, no alpha | 1 |
| Feature graphic | 1024×500 | 24-bit PNG, no alpha | 1 (required) |

Each file is under 8 MB. Long side / short side stays within the 2:1 Play limit.

## Upload map

### Default listing (English)

Play Console → **Grow** → **Store presence** → **Main store listing**

1. **App icon** — 512×512 32-bit PNG: `high-res-icon.png` (same plate as the adaptive launcher).
2. **Feature graphic** — `feature-graphic-en.png`
3. **Phone screenshots** (order matters; first is the store card) — `phone-en/01_journal.png` … `08_analytics.png`
4. **10" tablet screenshots** — `tablet-10-en/01_journal_landscape.png`

### Russian listing

Same console page → language **Русский** (add a localized listing if it is missing):

- Feature graphic: `feature-graphic-ru.png`
- Phone: `phone-ru/01_journal.png` … `08_analytics.png`
- Tablet: `tablet-10-ru/01_journal_landscape.png`

## Phone set (same order in RU and EN)

1. Journal — all data on the phone, any load type
2. Home-screen widget — camera, scanner, diesel in one tap
3. Add load — paste, manual, or photo (not Relay-only)
4. Camera with GPS watermark
5. File / document scanner
6. Diesel quick-add with GPS location
7. Weekly goal
8. My numbers

## Notes

- Do not upload PNGs with an alpha channel; the renderer flattens to RGB.
- 7-inch tablet is optional; the 10" landscape shot also satisfies the large-screen slot if you only add one tablet type.
- High-res icon: Play wants 512×512 32-bit PNG. Export `assets/ic_launcher_image.png` (or the Play App Signing generated icon) separately if the current file is not 512px.
