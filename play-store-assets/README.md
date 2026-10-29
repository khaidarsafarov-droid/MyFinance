# Play Console graphics — TruckoRig

Upload these in Play Console → **Grow** → **Store presence** → **Main store listing** (default language, English).

Screens are Robolectric renders of the current Compose UI (light theme, Midnight & Gold) with fictional demo data: driver Alex, loads such as Garner, NC → Dallas, TX, and a sample rate confirmation for Northline Freight. No personal data.

| File | Play Console slot | Spec |
| --- | --- | --- |
| `icon-512.png` | App icon | 512×512 PNG, 32-bit RGBA, 123 KB |
| `feature-graphic-1024x500.png` | Feature graphic | 1024×500 PNG, 24-bit RGB, no alpha, 197 KB |
| `phone/01-week.png` | Phone screenshot 1 | 1080×1920 PNG, 24-bit RGB |
| `phone/02-widget.png` | Phone screenshot 2 | 1080×1920 PNG, 24-bit RGB |
| `phone/03-photo.png` | Phone screenshot 3 | 1080×1920 PNG, 24-bit RGB |
| `phone/04-scanner.png` | Phone screenshot 4 | 1080×1920 PNG, 24-bit RGB |
| `phone/05-maintenance.png` | Phone screenshot 5 | 1080×1920 PNG, 24-bit RGB |
| `phone/06-premium.png` | Phone screenshot 6 | 1080×1920 PNG, 24-bit RGB |
| `phone/07-analytics.png` | Phone screenshot 7 | 1080×1920 PNG, 24-bit RGB |
| `tablet-7/01-journal.png` | 7-inch tablet screenshot | 1200×1920 PNG, 24-bit RGB |

Upload phone screenshots in filename order. The first frame is the store card.

## What each phone frame shows

1. **The week on one screen** — week gross, goal, miles, and recent loads.
2. **Home-screen widget** — goal, RPM, and camera / scanner / diesel.
3. **Photos with location** — trip photo stamped with city, time, and GPS.
4. **Scan the paperwork** — rate confirmation and recognized text.
5. **Service, on time** — mileage and date reminders.
6. **First month free** — Premium paywall, then $2.99/month, with each gate listed.
7. **Premium numbers** — paycheck, diesel, and best-paying routes.

The 7-inch frame is the journal with the route card visible (Garner, NC → Dallas, TX).

The icon is the adaptive launcher mark: midnight plate `#1E2150` → `#0F1230`, gold ring `#C8A867`, and the shield glyph from `app/src/main/res/drawable-nodpi/ic_launcher_image.png`. Play applies its own mask, so the file is a full square with an alpha channel.

A 10-inch landscape frame is not included. On that layout the week total sits under the goal ring and the amount is clipped.
