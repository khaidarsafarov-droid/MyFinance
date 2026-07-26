# TruckerLoad Release Notes

## Mindwell Forest Design System Unification (2026)

This release completes the design-system migration to **Truck Log — Mindwell Forest**
across app UI, widgets, dark mode, typography, and forms.

## Highlights

- Canonical forest/sage palette end-to-end (`SoftUiColors` + Material 3 schemes).
- **DM Sans** bundled and applied as the app typography family.
- Home-screen widget recolored from purple/blue glass to Forest gradients
  (light + night resources).
- Dark mode system bars and semantic text/surfaces fixed (no more forced sage bars
  with light icons in dark theme).
- Forms unified on `AppTextFieldDefaults.outlined()`.
- Decorative emoji replaced with Material Icons in social/voice/gallery/camera flows.
- Legacy Gold / NeoGlass / DarkGlass APIs deprecated in favor of SoftCard / TlButton /
  ForestScreenTitle.

## Navigation & IA

Phone bottom navigation:

- Logbook
- Weekly Goal
- Community
- Profile

Tablet uses the navigation rail; drawer covers settings, scanner, camera, reports,
tax tracker, and advisor.

## Component canon

| Prefer | Instead of |
|--------|------------|
| `BentoGlassCard` / `SoftCard` | `GoldCard`, raw glass APIs |
| `TlButton` | `NeoGlassPrimaryButton`, `GoldButton` |
| `ForestScreenTitle` | `DarkGlassScreenTitle` |
| `AppTextFieldDefaults.outlined()` | local `OutlinedTextFieldDefaults.colors` |
| Material Icons | inline emoji prefixes |

## Design tokens

See `docs/design/tokens.json` (v1.1.0) and `docs/design/README.md`.

## QA checklist

- Light/dark theme: status bar contrast, bottom nav, Home top app bar.
- Widget light/night: forest gradient, readable white text, RPM colors.
- Add Load / Paycheck / Diesel / Edit Load fields in both themes.
- Social/voice/gallery: icons render (no emoji fallbacks).
- Unit tests: `:app:testDebugUnitTest`.
