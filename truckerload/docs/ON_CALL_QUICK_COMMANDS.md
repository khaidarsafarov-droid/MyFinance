# TruckerLoad On-Call Quick Commands (First 5 Minutes)

Fast-response cheat sheet for the on-call engineer during production incidents.

## 0) First Minute: Triage Snapshot

Fill immediately:

- Time detected:
- Reporter/source:
- Affected screen/flow:
- Severity guess (P0/P1/P2/P3):
- Current build/version:

## 1) Immediate Actions (Minute 1-2)

- Confirm if issue is still active.
- Check if crash/blocking flow affects core user journey.
- Assign incident owner (or self-assign temporarily).
- Freeze non-essential deployments.

## 2) Quick Repro Script (Minute 2-4)

Run in this exact order:

1. Cold launch app.
2. Navigate: Finance -> Logs -> Profile -> Statistics -> Finance.
3. In Finance, change period and verify:
   - Gross updates
   - Diesel updates
   - Net Profit updates
4. Open FAB speed dial and close it via backdrop.
5. Open Statistics and set same period as Finance.
6. Check consistency between Finance and Statistics.

## 3) Decision Prompt (Minute 4-5)

Use this quick gate:

- If crash loop / unusable app / major incorrect totals -> treat as P0.
- If core flow blocked without crash -> treat as P1.
- If UI-only non-blocking issue -> P2/P3 and schedule patch.

Then choose:

- Rollback path -> use `ROLLBACK_CHECKLIST.md`
- Hotfix path -> use `HOTFIX_PLAYBOOK.md`

## Quick Evidence Checklist

- [ ] Repro steps captured.
- [ ] Screenshot/video captured.
- [ ] Device + OS captured.
- [ ] Timestamp captured.
- [ ] Impact estimate captured.

## Communication One-Liners

### Initial

"Investigating incident on [screen/flow], severity [P?], impact [brief]. Next update in 15 minutes."

### Recovery

"Containment in progress via [rollback/hotfix]. Validation running now. Next update in 15 minutes."

### Resolved

"Service restored via [rollback/hotfix]. Core flows validated. Monitoring continues for 24 hours."

## Where to Go Next

- Monitoring plan: `POST_RELEASE_MONITORING.md`
- Rollback procedure: `ROLLBACK_CHECKLIST.md`
- Hotfix runbook: `HOTFIX_PLAYBOOK.md`
- Full release assets: `RELEASE_PACKET_INDEX.md`

