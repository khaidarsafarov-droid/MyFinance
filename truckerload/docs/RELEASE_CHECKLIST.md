# TruckerLoad Release Checklist

Use this checklist for final manual verification before publishing.

## 1) Build & Stability

- [ ] `:truckerload:assembleDebug` completes successfully.
- [ ] App installs and launches without crash on a clean start.
- [ ] No visual freezes when switching between Finance, Logs, Profile, Statistics.

## 2) Date Engine (Critical)

- [ ] Finance opens with dynamic current-year context (no hardcoded 2024 behavior).
- [ ] Stats opens with dynamic current-year context (no hardcoded 2024 behavior).
- [ ] Default month logic is correct for current operational flow.
- [ ] Switching month/year updates selected week correctly.
- [ ] Aggregate values refresh after period change:
  - [ ] Gross
  - [ ] Diesel
  - [ ] Net Profit
- [ ] Previous/Next week navigation recalculates values immediately.

## 3) Finance UX (Cockpit)

- [ ] Horizontal week strip scrolls smoothly and remains responsive.
- [ ] Active week has clear highlight and inactive weeks remain readable.
- [ ] Net Profit card is visually dominant and amount styling is correct.
- [ ] Diesel and Salary accents are correct and consistent.
- [ ] Empty state appears cleanly when no data is available.
- [ ] Empty state add action is visible and clickable.

## 4) FAB Speed Dial

- [ ] FAB has a clear 44x44+ touch area.
- [ ] Tap FAB opens speed dial with backdrop.
- [ ] Tap outside closes speed dial.
- [ ] Icon rotation/open-close animation is smooth.
- [ ] "Add Diesel" opens expected flow.
- [ ] "Add Salary" opens expected flow.

## 5) Navigation & Touch Targets

- [ ] Bottom navigation shows all required items.
- [ ] Selected tab state is always clear.
- [ ] Top bar icon buttons are easy to tap (44x44+).
- [ ] Week controls, add buttons, and form actions meet touch size expectations.

## 6) Forms & Dialogs

- [ ] Add Load form: field states, validation, and submit action work.
- [ ] Add Diesel form: date/time pickers and save flow work.
- [ ] Add Paycheck form: date/time pickers and save flow work.
- [ ] Settings form: validation and save behavior work.
- [ ] Dialog colors/contrast are readable on target devices.

## 7) Data Consistency

- [ ] Finance and Stats show consistent totals for the same period.
- [ ] No stale values remain after period switches.
- [ ] Empty/non-empty transitions do not show overlapping UI layers.

## 8) Performance & Perceived Quality

- [ ] Screen transitions feel smooth on mid-range device hardware.
- [ ] No obvious jank when opening month picker or speed dial.
- [ ] No clipped text, icon overlap, or card overflow on small screens.

## 9) Accessibility & Readability

- [ ] Primary values are readable in outdoor/high-brightness usage.
- [ ] Label/value hierarchy is clear at a glance.
- [ ] Contrast is sufficient for all action controls.

## 10) Final Go/No-Go

- [ ] All critical items above pass.
- [ ] Known non-critical issues are documented.
- [ ] Release notes are updated and attached.

