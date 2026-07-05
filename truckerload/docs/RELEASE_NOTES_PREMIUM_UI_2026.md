# TruckerLoad Release Notes

## Premium UI & Date Engine (2026)

This release completes a major UX and architecture refresh focused on driver ergonomics, visual consistency, and date correctness.

## Highlights

- Fixed legacy date behavior and removed hardcoded 2024 logic from Finance/Stats flows.
- Implemented dynamic date handling based on current device date.
- Defaulted Finance/Stats to March of the current year and synced week selection logic.
- Replaced heavy date controls with a modern horizontal week strip.
- Introduced active-week highlighting (Indigo-600) and improved visual state clarity.
- Rolled out premium light design language across core screens.

## Finance & Stats Improvements

- Added cockpit-style KPI hierarchy with Net Profit as centerpiece.
- Added gradient emphasis for Net Profit values (Emerald -> Teal).
- Added glass card styling and layered depth effects.
- Implemented speed-dial FAB with backdrop for quick one-hand actions:
  - Add Diesel Entry
  - Add Salary Entry
- Added FAB micro-interactions:
  - plus-to-close rotation
  - soft glow pulse
- Added subtle shimmer on active week chip for visual feedback.

## Navigation & Ergonomics

- Updated bottom navigation to four key destinations:
  - Finance
  - Logs
  - Profile
  - Statistics
- Improved selected tab visibility and premium indicator styling.
- Standardized touch target sizing across key controls.

## Form & Detail Screen Consistency

Applied premium styling and typography consistency to:

- Add Load
- Add Diesel
- Add Paycheck
- Edit Load
- Load Detail
- Settings
- Tax Tracker
- Financial Advisor

Includes:

- unified backgrounds and app bars
- consistent outlined text field colors
- consistent CTA sizing for major actions
- consistent error and supporting text color hierarchy
- dialog styling alignment (AlertDialog / DatePickerDialog)

## Technical Quality

- Build status: successful (`:truckerload:assembleDebug`).
- Lint status: no new lint issues on modified files.
- Deprecated icon usages migrated where updated in this cycle (AutoMirrored variants).

## QA Checklist (Recommended Final Pass)

- Verify readability under bright sunlight and night mode conditions.
- Verify week/month switching updates all Finance aggregates immediately.
- Verify speed-dial open/close flow on smaller devices.
- Verify bottom bar labels and icon alignment on low-density screens.
- Verify dialogs and text input focus states across Add/Edit flows.

