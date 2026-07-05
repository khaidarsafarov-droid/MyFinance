# TruckerLoad UAT Sign-off Template

Use this document for final user acceptance before release.

## Release Information

- Release name:
- Build/version:
- Environment:
- Test window (date/time):
- UAT owner:
- Product owner:

## Scope of Validation

- Finance screen (date engine, KPIs, speed dial)
- Statistics screen (period sync and totals)
- Add flows (Load, Diesel, Salary)
- Navigation and touch ergonomics
- Visual consistency (premium cockpit styling)

## Acceptance Criteria

- [ ] No hardcoded 2024 behavior in Finance or Statistics.
- [ ] Period switching updates totals correctly and immediately.
- [ ] Finance and Statistics are consistent for the same period.
- [ ] Critical actions are reachable and usable with one hand.
- [ ] No crash in core flows.
- [ ] UI is readable and stable on target devices.

## UAT Test Matrix

| ID | Flow | Expected Result | Status (Pass/Fail/Blocked) | Notes |
|---|---|---|---|---|
| UAT-01 | App cold launch | App opens without crash |  |  |
| UAT-02 | Finance period switch | Gross/Diesel/Net Profit refresh |  |  |
| UAT-03 | Week prev/next navigation | Correct week and totals shown |  |  |
| UAT-04 | Stats period switch | Stats refresh and match period |  |  |
| UAT-05 | Finance vs Stats consistency | No mismatch for same period |  |  |
| UAT-06 | FAB speed dial open/close | Backdrop + actions work |  |  |
| UAT-07 | Add Diesel flow | Form and pickers work correctly |  |  |
| UAT-08 | Add Salary flow | Form and pickers work correctly |  |  |
| UAT-09 | Add Load flow | Form and validation are correct |  |  |
| UAT-10 | Navigation bar | All tabs open expected screens |  |  |
| UAT-11 | Empty state | Actionable, no broken layout |  |  |
| UAT-12 | Touch ergonomics | Buttons feel easy and reliable |  |  |

## Defect Log (If Any)

| Defect ID | Severity (Critical/High/Medium/Low) | Screen | Summary | Repro Steps | Status |
|---|---|---|---|---|---|
|  |  |  |  |  |  |

## Risk Assessment

- Open critical issues:
- Open non-critical issues:
- Workarounds:
- Release risk level (Low/Medium/High):

## Final Decision

- Decision: [ ] GO   [ ] NO-GO
- Decision date:
- Conditions for GO (if any):

## Signatures

- QA Lead (name/sign/date):
- Product Owner (name/sign/date):
- Engineering Lead (name/sign/date):

