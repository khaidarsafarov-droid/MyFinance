# Google Play listing assets

Ready-to-upload images for the TruckoRig Play Console listing.

Regenerate after UI copy or palette changes:

```bash
cd truckerload/store/google-play
python3 render_screenshots.py
```

Requires Google Chrome (headless) and Pillow. Fonts and the launcher mark are copied from `app/src/main/res`.

Phone, 7", and 10" frames use the Midnight & Gold caption plate (`#1E2150` → `#0F1230`, gold `#C8A867`). The device shows a Robolectric render of the current Compose screens (and the live Glance split widget) with fictional demo loads. Captures live in `captures/{en,ru}/`. Regenerate with `:app:testDebugUnitTest --tests com.truckerload.presentation.store.PlayListingCaptureTest`, then `python3 install_captures.py /tmp/play-captures` and `python3 render_screenshots.py`. This VM has no emulator, so the shots are not `adb screencap`.

## Specs (Play Console)

| Asset | Size | Format | Count |
| --- | --- | --- | --- |
| Phone screenshots | 1080×1920 (9:16) | 24-bit PNG, no alpha | 7 English, 8 Russian |
| 10" tablet | 1920×1200 (16:9) | 24-bit PNG, no alpha | 1 |
| 7" tablet | 1200×1920 | 24-bit PNG, no alpha | 1 |
| Feature graphic | 1024×500 | 24-bit PNG, no alpha | 1 (required) |
| Feature graphic, 2× | 2048×1000 | 24-bit PNG, no alpha | `feature-graphic-*-2048x1000.png` |

Each file is under 8 MB. Long side / short side stays within the 2:1 Play limit.

## Upload map

### Default listing (English)

Play Console → **Grow** → **Store presence** → **Main store listing**

1. **App icon** — 512×512 32-bit PNG: `high-res-icon.png` (same plate as the adaptive launcher).
2. **Feature graphic** — `feature-graphic-en.png`
3. **Phone screenshots** (order matters; first is the store card) — `phone-en/01_dashboard.png` … `07_analytics.png`
4. **10" tablet screenshots** — `tablet-10-en/01_journal_landscape.png`
5. **7" tablet screenshots** — `tablet-7-en/01_journal.png`

### Russian listing

Same console page → language **Русский** (add a localized listing if it is missing):

- Feature graphic: `feature-graphic-ru.png`
- Phone: `phone-ru/01_journal.png` … `08_telegram.png`
- Tablet 10": `tablet-10-ru/01_journal_landscape.png`
- Tablet 7": `tablet-7-ru/01_journal.png`

## English phone set

1. Dashboard — week hero, load cards, shortcuts, bottom bar
2. Home-screen widget — split header on a wallpaper, two sizes
3. Location photo — trip photo with the app’s city, time, and GPS stamp
4. Document scan — rate confirmation with the recognized text open
5. Maintenance — mileage and date service reminders
6. Premium — paywall, first month free, then $2.99/month
7. Analytics — paycheck, diesel, and routes, with the Premium badge

## Russian phone set

1. Journal — all data on the phone, any load type
2. Home-screen widget — camera, scanner, diesel in one tap
3. Add load — paste, manual, or photo (not Relay-only)
4. Camera with GPS watermark
5. File / document scanner
6. Diesel quick-add with GPS location
7. Weekly goal
8. Telegram bot — forward a load, import HTML/text export, restore history

## Notes

- Do not upload PNGs with an alpha channel; the renderer flattens to RGB.
- 7" and 10" frames are both generated. The 7" shot is the phone journal inside the same caption plate.
- High-res icon: Play wants 512×512 32-bit PNG. Export `assets/ic_launcher_image.png` (or the Play App Signing generated icon) separately if the current file is not 512px.
