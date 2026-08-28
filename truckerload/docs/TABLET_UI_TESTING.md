# Tablet UI testing (TruckerLoad Android)

TruckerLoad is a **native Jetpack Compose** app, not a web client. Tablet layout uses
width buckets (dp) analogous to common CSS breakpoints:

| Bucket | Width (dp) | CSS analog | Navigation |
|--------|------------|------------|------------|
| COMPACT | &lt; 600 | phone | Bottom bar |
| MEDIUM | 600–839 | `md` / portrait tablet | Bottom bar + swipe drawer |
| EXPANDED | ≥ 840 | `lg`+ / landscape tablet | Fixed navigation rail |

Implementation: `presentation/utils/DeviceUtils.kt`.

## Emulator AVDs

Create or use AVDs that match target devices:

| Device | Approx. size | Notes |
|--------|--------------|-------|
| Pixel Tablet | 1600×2560, 256dp | Portrait MEDIUM, landscape EXPANDED |
| Nexus 9 / 10" WXGA | 800×1280 | Portrait MEDIUM |
| Pixel C | 900×1280 | Portrait MEDIUM |
| iPad class | 768×1024 @ 2x → ~384–768dp | Use custom AVD 768×1024 |

```bash
source ~/.bashrc
emulator -avd tl_test -no-accel -no-snapshot -no-window -no-audio \
  -memory 4096 -cores 4 -gpu swiftshader_indirect -no-boot-anim
```

After install, AOT-compile for responsiveness on software GPU:

```bash
adb shell cmd package compile -m speed -f com.truckorig
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

## Orientation checks

1. **Portrait tablet (MEDIUM)** — bottom navigation visible; hamburger opens drawer; no side rail.
2. **Landscape tablet (EXPANDED or MEDIUM+landscape)** — navigation rail on the left; no bottom bar.
3. Rotate on home screen and confirm navigation mode switches without crash.

```bash
adb shell wm size reset
adb shell wm density reset
# Force orientation (varies by API):
adb shell settings put system user_rotation 0   # portrait
adb shell settings put system user_rotation 1   # landscape
```

## Split-screen (50% width)

1. Open two apps in split-screen on a tablet AVD (or resize with `wm size`).
2. Each half should re-classify width — expect COMPACT or MEDIUM, bottom nav, no horizontal page scroll.

```bash
# Simulate narrow split: ~400dp width on a 800dp tablet
adb shell wm size 400x1280
```

Reset:

```bash
adb shell wm size reset
```

## Screen checklist

- [ ] Home journal: pull-to-refresh works; **2-column load cards** on tablet (≥600dp); 1 column on phone.
- [ ] Multi-column: swipe-to-delete disabled (tap into detail to manage); phone keeps swipe.
- [ ] Photo gallery: adaptive grid (2 cols portrait, 3 landscape).
- [ ] Forms / search: inputs ≥ 48dp touch height.
- [ ] Dialogs: centered, max width ~672dp, dismiss on outside tap.
- [ ] Bottom nav / toolbar icons: ≥ 48dp tap targets.
- [ ] Portrait tablet: bottom navigation (no rail).
- [ ] Landscape tablet: navigation rail on the left.
- [ ] No full-page horizontal scroll at 768–1366dp effective width.

## Chrome DevTools (web)

Not applicable — there is no web UI in this repository. Use Android Studio Layout Inspector
or `adb exec-out screencap -p` for visual checks.

## Unit tests

```bash
cd truckerload
sh ./gradlew :app:testDebugUnitTest --tests com.truckerload.presentation.utils.WindowSizeClassTest
sh ./gradlew :app:testDebugUnitTest --tests com.truckerload.presentation.screens.home.HomeLoadGridTest
```
