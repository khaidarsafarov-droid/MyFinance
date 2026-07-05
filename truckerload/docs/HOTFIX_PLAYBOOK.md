# TruckerLoad Hotfix Playbook (P0/P1)

Operational runbook for urgent production fixes after release.

## Purpose

- Restore stable operation quickly when critical issues appear.
- Minimize user impact and reduce time-to-recovery.
- Keep communication clear across Engineering, QA, and Product.

## Trigger Conditions

Start this playbook when any of these occurs:

- P0 crash loop or app unusable for core flows.
- P0/P1 incorrect finance totals caused by period logic.
- P1 blocked flow in Finance/Stats/Add Diesel/Add Salary/Add Load.
- Severe regression introduced in latest production rollout.

## Roles

- **Incident Commander (IC):** coordinates response and final decisions.
- **Tech Owner:** investigates root cause and implements fix/rollback.
- **QA Owner:** validates fix, executes focused regression.
- **Comms Owner:** posts status updates to stakeholders/support.

## Response Timeline

### 0-15 minutes (Containment)

1. Confirm incident scope and severity.
2. Assign IC, Tech Owner, QA Owner, Comms Owner.
3. Freeze non-essential deployments.
4. Decide immediate containment path:
   - rollback candidate
   - config kill switch (if available)
   - urgent patch branch

### 15-45 minutes (Diagnosis)

1. Reproduce on production build.
2. Capture minimal reproducible steps.
3. Identify impacted module/screen.
4. Estimate safest recovery option:
   - Rollback (preferred for unknown/high-risk failures)
   - Hotfix patch (preferred for isolated, low-blast-radius issues)

### 45-120 minutes (Recovery)

1. Implement rollback or hotfix.
2. Run focused smoke validation:
   - launch
   - navigation
   - Finance period switch
   - Stats consistency
   - affected add flow(s)
3. Deploy and monitor immediate post-fix telemetry.

## Decision Framework: Rollback vs Hotfix

- **Choose Rollback when:**
  - root cause is unclear
  - fix confidence is low
  - multiple critical screens affected
- **Choose Hotfix when:**
  - root cause is isolated and confirmed
  - patch is minimal and testable quickly
  - rollback is more risky than targeted fix

## Hotfix Branch Workflow

1. Create branch: `hotfix/<short-incident-name>`.
2. Keep change scope minimal (no refactors, no unrelated cleanup).
3. Add/adjust tests only where they directly protect the fix.
4. Run targeted verification checklist.
5. Tag build clearly as hotfix candidate.

## Validation Gate (Must Pass)

- [ ] No crash on launch.
- [ ] Affected flow works end-to-end.
- [ ] Finance/Stats period consistency verified.
- [ ] No new blocking regressions in core navigation.
- [ ] QA owner signs off.
- [ ] IC approves deployment.

## Communication Templates

### Initial Alert

- Incident:
- Severity:
- Start time:
- User impact:
- Owners (IC/Tech/QA/Comms):
- Next update ETA:

### Progress Update

- Current status:
- Containment action:
- Root cause status:
- ETA for fix/rollback:
- Risk notes:

### Resolution Update

- Resolution path (Rollback/Hotfix):
- Time restored:
- Validation result:
- Residual risk:
- Follow-up actions:

## Post-Incident Actions (Within 24h)

1. Write concise incident summary.
2. Add regression test/checklist item preventing recurrence.
3. Document root cause and detection gap.
4. Track follow-up tasks with owners and due dates.

## Incident Record Template

- Incident ID:
- Severity:
- Start/end time:
- Root cause:
- Trigger:
- Impacted users/flows:
- Resolution:
- Validation evidence:
- Preventive actions:

