# Tablet adaptation (7–13″)

TruckerLoad is a **native Android / Jetpack Compose** app (not a web client).
Tablet behavior maps the usual web breakpoints onto Material width size classes.

## Size classes

| Width (dp) | Class | Typical devices | Chrome | Content |
|------------|-------|-----------------|--------|---------|
| &lt; 600 | Compact | Phones | Bottom navigation | Single pane |
| 600–839 | Medium | iPad / Android tablet portrait, split-screen | Navigation rail + swipe drawer | Single pane, max ~840dp |
| ≥ 840 | Expanded | Tablet landscape, iPad Pro, Surface | Navigation rail + swipe drawer | List \| detail journal, max ~1366dp, 3-col metrics |

Helpers live in `presentation/utils/DeviceUtils.kt`
(`windowSizeClassForWidth`, `isTablet`, `useTwoPaneLayout`, `adaptiveGridColumns`).

Touch targets: `UiDimens.TouchTarget` / toolbar icons = **48dp**.
Form helpers: `Modifier.appFormField()` / `Modifier.appFormFieldSpacing()`.

## What was adapted

1. **Breakpoints** aligned with Material (600 / 840) instead of treating every ≥600dp width as “Expanded”.
2. **Navigation** — Compact: bottom bar; Medium landscape / Expanded: wide soft forest sidebar
   (`TruckLogNavigationRail`) + modal tools drawer. Sidebar includes profile greeting and a
   **Backup** card that opens Settings (Telegram sync is automatic — not promoted on Home).
3. **Journal (tablet)** — soft dashboard: hero week summary, 4 stat cards, recent loads + weekly
   goal panel. Phone layout unchanged.
4. **Journal (expanded detail)** — `JournalListDetailHost` (list left, load detail right) when
   browsing into a load from other flows; Home dashboard is the landing composition.
5. **App-wide soft page chrome** — `SoftAppPageScaffold` / `SoftTabletPageHeader` /
   `SoftTabletTwoPane` (`TabletSoftChrome.kt`) on primary and tool destinations: Weekly Goal,
   Weekly Goal, Profile, Settings, Analytics, Advanced Stats, Map, Maintenance, Scan gallery,
   About. Tablet shows a large title (+ optional actions); phone keeps TopAppBar (menu/back).
6. **Analytics metrics** — `BentoGrid` uses 2 columns on phone/portrait tablet, 3 on landscape.
7. **Content width** — padded beside the sidebar; Medium/Expanded horizontal padding as before.

## DevTools / emulator checklist

Android Studio **Layout Validation** or a tablet AVD:

| Profile | Suggested AVD / size | Orientation |
|---------|----------------------|-------------|
| iPad-like | `7.6" Foldable` or custom **768×1024** mdpi/hdpi | Portrait → Medium |
| iPad landscape | rotate to **1024×768** | Expanded, two-pane journal |
| iPad Pro 12.9 | **1024×1366** / **1366×1024** | Medium / Expanded |
| Surface Pro | **912×1368** / **1368×912** | Medium / Expanded |
| Split-screen | Multi-window ~50% width on a tablet AVD | Should drop to Compact or Medium and keep usable chrome |

### Manual steps

1. Install debug APK, AOT optional: `adb shell cmd package compile -m speed -f com.truckorig`
2. Disable animations (recommended on slow emulators).
3. Open **Logbook**:
   - Portrait tablet: rail visible; tapping a load opens full-screen detail (stack).
   - Landscape (≥840dp): list \| detail; empty pane copy “Select a load”; back clears detail.
4. Open **Reports / Analytics**: metric cells should reflow to 3 columns in landscape.
5. Rotate portrait ↔ landscape: no horizontal page scroll; rail stays; two-pane toggles with Expanded.
6. Open drawer from menu / edge swipe on main tabs; camera/scanner still hide primary chrome.
7. Confirm nav items and FAB remain ≥ 48×48dp.

### Compose preview widths (optional)

```text
widthDp = 411   // Compact phone
widthDp = 768   // Medium tablet portrait
widthDp = 1024  // Expanded landscape
widthDp = 600   // Compact/Medium boundary (rail on)
widthDp = 840   // Medium/Expanded boundary (two-pane on)
```

## Unit tests

`presentation/utils/WindowSizeClassTest` covers width buckets, tablet chrome gate, two-pane gate, and grid columns.
