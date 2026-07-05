# TruckerLoad Rollback Checklist

Use this checklist for fast, safe rollback during production incidents.

## Preconditions

- [ ] Incident severity confirmed (typically P0/P1).
- [ ] Incident Commander assigned.
- [ ] Rollback target version identified and available.
- [ ] Stakeholders notified that rollback is starting.

## 1) Decision Gate

- [ ] Root cause is unclear or hotfix risk is too high.
- [ ] User impact is active and significant.
- [ ] Rollback is lower risk than immediate patch.
- [ ] IC explicitly approves rollback.

## 2) Execute Rollback

- [ ] Freeze all non-essential deployments.
- [ ] Deploy last known stable version.
- [ ] Confirm deployment finished successfully.
- [ ] Verify config/feature flags match stable baseline.

## 3) Immediate Validation (5-10 min)

- [ ] App launch succeeds.
- [ ] Core navigation works (Finance/Logs/Profile/Statistics).
- [ ] Finance period switch updates totals.
- [ ] Statistics period switch works and is consistent.
- [ ] Add Diesel / Add Salary critical entry points open.
- [ ] No new crash spike after rollback.

## 4) Communication

- [ ] Post "rollback completed" update with timestamp.
- [ ] Share current user impact status.
- [ ] Share next ETA for deeper investigation.

## 5) Post-Rollback Follow-up

- [ ] Keep incident open until stability is confirmed.
- [ ] Create root-cause analysis task.
- [ ] Create prevention task (test/checklist/alert gap).
- [ ] Document lessons learned.

## Rollback Log (Fill During Incident)

- Incident ID:
- Start time:
- Rollback start:
- Rollback complete:
- Stable version restored:
- Validation owner:
- Status after 15 minutes:
- Remaining risks:

