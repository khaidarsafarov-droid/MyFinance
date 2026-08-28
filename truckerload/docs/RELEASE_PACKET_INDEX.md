# TruckerLoad Release Packet Index

Single entry point for release readiness artifacts.

## Google Play (production)

- [Play Console release runbook](../store/google-play/PLAY_CONSOLE_RELEASE.md) — AAB, listing, Data safety
- [Store listing copy EN/RU](../store/google-play/listing/)
- [Privacy policy draft](../store/google-play/PRIVACY_POLICY.md)
- [Data safety draft](../store/google-play/DATA_SAFETY.md)
- [Release notes 1.5.7](../store/google-play/RELEASE_NOTES_1.5.7.md)
- [Listing images README](../store/google-play/README.md)
- [Google Sign-In / upload SHA-1](./GOOGLE_SIGNIN_SETUP.md)

## Core Documents

- [Release Notes](./RELEASE_NOTES_PREMIUM_UI_2026.md)
- [Release Checklist](./RELEASE_CHECKLIST.md)
- [Smoke Test Script (5-7 min)](./SMOKE_TEST_SCRIPT.md)
- [UAT Sign-off Template](./UAT_SIGNOFF_TEMPLATE.md)
- [Post-Release Monitoring (24-48h)](./POST_RELEASE_MONITORING.md)
- [Hotfix Playbook (P0/P1)](./HOTFIX_PLAYBOOK.md)
- [Rollback Checklist](./ROLLBACK_CHECKLIST.md)
- [On-Call Quick Commands (5 min)](./ON_CALL_QUICK_COMMANDS.md)
- [Incident Report Template (RCA)](./INCIDENT_REPORT_TEMPLATE.md)
- [Weekly Release Health Report](./WEEKLY_RELEASE_HEALTH_REPORT.md)
- [Pre-Release Risk Assessment](./PRE_RELEASE_RISK_ASSESSMENT.md)
- [Release Retro Template](./RELEASE_RETRO_TEMPLATE.md)

## Suggested Execution Order

1. Follow `store/google-play/PLAY_CONSOLE_RELEASE.md` for AAB + Console forms.
2. Run quick regression with `SMOKE_TEST_SCRIPT.md`.
3. Execute full validation using `RELEASE_CHECKLIST.md`.
4. Complete sign-off and decision in `UAT_SIGNOFF_TEMPLATE.md`.

## Owners (Fill Before Release)

- Engineering:
- QA:
- Product:

## Release Decision Snapshot

- Build/version: **1.5.7 (12)**
- Candidate date:
- Decision: [ ] GO   [ ] NO-GO
- Final approver:
