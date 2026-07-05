# TruckerLoad Pre-Release Risk Assessment

Use this template before each release candidate to evaluate delivery risk and define mitigation.

## Release Metadata

- Release name:
- Build/version:
- Planned release date:
- Prepared by:
- Reviewers (Eng/QA/Product):

## 1) Scope Summary

- Key features/changes in this release:
- Systems/screens impacted:
- Data model or migration impact:
- Third-party dependency impact:

## 2) Risk Matrix

Use scale:
- Probability: 1 (Low) to 5 (High)
- Impact: 1 (Low) to 5 (High)
- Risk score = Probability x Impact

| Risk ID | Risk Description | Area | Probability (1-5) | Impact (1-5) | Score | Mitigation | Owner |
|---|---|---|---|---|---|---|---|
| R-01 |  |  |  |  |  |  |  |
| R-02 |  |  |  |  |  |  |  |
| R-03 |  |  |  |  |  |  |  |

## 3) High-Risk Focus (Score >= 12)

For each high-risk item:

- Risk ID:
- Failure mode:
- User/business impact:
- Preventive checks before release:
- Fallback plan (rollback/hotfix/feature-flag):
- Sign-off owner:

## 4) Readiness Gates

- [ ] Build is stable and reproducible.
- [ ] Critical smoke tests pass.
- [ ] Release checklist completed.
- [ ] UAT sign-off completed (or explicitly waived).
- [ ] Monitoring plan prepared for first 24-48h.
- [ ] Rollback path validated.
- [ ] Incident ownership/on-call coverage confirmed.

## 5) Period/Data Integrity Checks (Finance/Stats)

- [ ] Date engine behavior validated for current year.
- [ ] Finance totals update correctly on period switch.
- [ ] Statistics totals align for same period.
- [ ] No stale period data after navigation changes.

## 6) Operational Preparedness

- [ ] On-call engineer assigned.
- [ ] Incident channel and comms template ready.
- [ ] Hotfix playbook reviewed.
- [ ] Rollback checklist reviewed.

## 7) Final Risk Decision

- Overall risk level: [ ] Low  [ ] Medium  [ ] High
- Release recommendation: [ ] GO  [ ] GO with Conditions  [ ] NO-GO
- Conditions (if any):
- Final approvers:
  - Engineering:
  - QA:
  - Product:

