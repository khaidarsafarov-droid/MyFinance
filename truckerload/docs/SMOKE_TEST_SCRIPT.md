# TruckerLoad Smoke Test Script (5-7 min)

Goal: quickly detect critical regressions after UI/date-engine changes.

## Preconditions

- App is installed on a test device.
- Test account/data is available (at least one load, one diesel, one salary entry).
- Network is on (if sync-related behavior is expected).

## Pass Criteria

- No crashes.
- No blocked navigation.
- Finance and Stats values update when the period changes.

## Step-by-Step Script

### 1) Launch & Navigation (1 min)

1. Open app from cold start.
2. Confirm app launches without crash.
3. Tap bottom tabs in order:
   - Finance -> Logs -> Profile -> Statistics -> Finance
4. Confirm each screen opens and returns quickly.

Expected:
- No freeze, no overlapping layers, no broken top/bottom bars.

### 2) Finance Date Engine (1-2 min)

1. On Finance, open period selector (week strip/month picker path).
2. Switch to another month/week, then back.
3. Use previous/next week arrows.

Expected:
- Period changes immediately.
- Gross, Diesel, Net Profit recalculate without stale values.
- No hardcoded 2024 behavior appears.

### 3) Finance UI Core (1 min)

1. Verify Net Profit card is visible and visually dominant.
2. Verify week strip active/inactive states are clear.
3. If period has no data, verify clean empty state with add action.

Expected:
- Card hierarchy remains readable.
- Empty state is actionable and not just plain text.

### 4) FAB Speed Dial (1 min)

1. Tap FAB to open quick actions.
2. Verify backdrop appears.
3. Tap "Add Diesel", back out.
4. Tap FAB again, tap outside backdrop to close.

Expected:
- Open/close animation is smooth.
- No accidental taps behind backdrop.

### 5) Add Flows Quick Check (1 min)

1. Open Add Diesel, enter minimal valid data, cancel/back.
2. Open Add Salary (Paycheck), enter minimal valid data, cancel/back.
3. Open Add Load, focus/unfocus key fields.

Expected:
- Forms render correctly.
- Dialog/picker colors are readable.
- Buttons are easy to tap (44x44+ target intent).

### 6) Stats Consistency (1 min)

1. Go to Statistics.
2. Select the same period used in Finance.
3. Compare major totals/trend direction.

Expected:
- No obvious mismatch with Finance for the same period.
- No stale values after changing period.

## Fast Failure Log Template

Use this template when a step fails:

- Step:
- Screen:
- Action:
- Expected:
- Actual:
- Repro rate (e.g. 3/3):
- Device/OS:
- Build:
- Screenshot/Video:

## Go/No-Go Rule

- GO: all critical flows pass (launch, navigation, Finance period updates, Stats consistency, FAB actions).
- NO-GO: any crash, blocked flow, or stale/incorrect period-based totals.

## 7) Journal / Loads QA (2–3 min) — added for QUALITY_100

1. Home: tap Search icon — search field appears; tap again with empty query — field hides.
2. Swipe a load left → confirm dialog → Cancel → card stays visible (not ghost-dismissed).
3. Swipe again → Confirm delete → load gone; linked photos/scans gone if any.
4. Switch language to EN → Home DISPUTE filter label is English («Dispute»), not Russian.
5. Add Load: paste Relay fixture with Total Rate + Pu-address → Save → appears in list immediately.
6. Open load camera from card → capture → share/save → returns to load detail.
7. Drawer/widget camera: watermark shows Trip ID of latest load but does **not** auto-attach files.
8. Pull to refresh → toast «Telegram sync started» (when bot configured).

Expected:
- No silent swipe-dismiss without confirm.
- EN dispute strings present.
- Free camera does not silently link photos to a load.

