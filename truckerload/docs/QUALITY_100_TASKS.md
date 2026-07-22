# QA / quality backlog — 100 tasks

Full-app walkthrough after QUALITY_50. Branch: `cursor/qa-100-tasks-f936`.
Priority: P0 bugs → P1 verification/tests → P2 polish/i18n → P3 coverage.

Status legend: ✅ done · 🔄 in progress · ⬜ todo

## P0 — Critical bugs (1–20)

1. ✅ Swipe-to-delete: do not dismiss card until confirm (`SwipeableLoadCard`)
2. ✅ Reset swipe state when delete dialog dismissed
3. ✅ Surface Home delete-failure snackbar (not silent catch)
4. ✅ Add `home_filter_dispute` / `home_empty_dispute` to values-en + values-ru
5. ✅ Wire search expand/collapse to actually hide/show search field
6. ✅ Optimistic insert: apply overlay before DB write; revert on failure
7. ✅ EditLoad: validate rate/miles; show error on bad input
8. ✅ Telegram: do not advance offset when `handleUpdate` throws (avoid lost updates)
9. ✅ Encode `voiceRoom` / `call` path segments like other routes
10. ✅ Free camera: do not auto-attach to latest load without user intent (watermark-only)
11. ✅ GoalViewModel: replace hardcoded RU goal error with string resource
12. ✅ SignUp placeholder «Иван Иванов» → string resource
13. ✅ Map heatmap rating labels («Хорошо»…) → string resources
14. ✅ Load detail hardcoded «mi · stops» / PU labels → strings
15. ✅ Remove dead `CALENDAR_WEEK` path or wire WeekCalendarPicker
16. ✅ Tax per-diem: document/fix `perDiemDays = loads.size` heuristic
17. ✅ Settings emoji export buttons: add contentDescriptions
18. ✅ Drive `e.intent!!` NPE guard
19. ✅ AiRepository.extractTextFromImage: clear non-hardcoded failure message
20. ✅ Pull-refresh: feedback when sync enqueue succeeds/fails

## P1 — Feature verification & fixes (21–50)

21. ✅ Unit: syncLoadsCdc SUCCESS / DUPLICATE / EMPTY matrix
22. ✅ Unit: LoadFilterUseCase full matrix (yesterday/month/year/search)
23. ✅ Unit: HomeViewModel pending delete cancel/confirm
24. ✅ Unit: BackupService restoreFromJson round-trip (loads count)
25. ✅ Unit: Nav encodePathSegment blank → `_`
26. ✅ Unit: ContentModerator blocked patterns
27. ✅ Unit: Tax pure helpers (brackets / quarterly) if extractable
28. ✅ Unit: WidgetStatsFormatter edge cases
29. ⬜ Verify paste Relay fixture parses + inserts (parser already covered; AddLoad path)
30. ✅ Dispute toggle → Home DISPUTE filter
31. ✅ Finish-date set/clear recomputes duration (LoadYieldCalculator)
32. ✅ Extend DeleteLoadClearsPhotosTest for scans
33. ✅ Camera deny-location still saves
34. ✅ Camera attach-to-load vs free camera tripId watermark
35. ✅ Batch discard confirm when attached
36. ⬜ Scanner GMS-unavailable path no crash
37. ⬜ Scan attach appears on detail
38. ✅ Orphan scan cleanup unit/integration
39. ⬜ Photo link/unlink to load
40. ✅ Photo delete removes file
41. ✅ Gallery TODAY/WEEK boundary helpers tested
42. ✅ Missing photo file → graceful UI
43. ⬜ Theme survives process death (SettingsDataStore)
44. ✅ Language EN↔RU dispute strings English when EN
45. ✅ Local backup/restore count parity (test harness)
46. ✅ RPM thresholds update LoadCard colors (pure color helper test)
47. ✅ Weekly goal vs widget yield parity (calculator)
48. ✅ Widget deep-link route mapping documented/fixed if wrong
49. ✅ Analytics empty-loads export no crash
50. ⬜ Stats week totals parity with Home THIS_WEEK (filter use case)

## P2 — Social / auth / sync / map / tax (51–75)

51. ✅ LOCAL_ONLY: demo peers labeled; no Supabase
52. ✅ Block/unblock peer hides chat
53. ✅ Status purge expired
54. ⬜ Voice/mic deny UX string
55. ✅ Group invite code edge (blank/invalid)
56. ✅ Social seed only when empty (already) — assert idempotent
57. ✅ LOCAL_ONLY cold start auto-login
58. ✅ Multi-user DB isolation note + AccountIds tests
59. ⬜ Logout stops FGS when not LOCAL_ONLY (guard)
60. ✅ Legacy DB migrate flag documented
61. ✅ Map empty loads metrics
62. ⬜ Tax year switch recalculation
63. ✅ Advisor empty + with-load context (DeterministicAdvisor)
64. ✅ Add paycheck/diesel validation
65. ⬜ Map i18n remaining strings
66. ⬜ Telegram 401 stops service (verify log path)
67. ✅ Dual offset prefs+DataStore stay in sync on persist
68. ✅ Import HTML/JSON count tests (existing parsers + ImportResult)
69. ⬜ Boot/watchdog work unique name stable
70. ⬜ Notification permission rationale string API 33+
71. ✅ SmartNotificationWorker zero-loads safe
72. ✅ DuplicateAuditUseCase after import
73. ✅ Battery optimization helper no crash
74. ✅ FileProvider share paths for photos/scans/avatars
75. ✅ ReportGenerator special chars in tripId

## P3 — Emulator / ANR / hardening (76–100)

76. ✅ Startup backfill/orphan cleanup off main (verify)
77. ⬜ rememberDecodedBitmap bounds for gallery scroll
78. ⬜ Camera multi-capture bitmap recycle
79. ⬜ Map missing API key no crash
80. ⬜ Scanner without GMS no crash
81. ✅ Home animation scale note in SMOKE doc
82. ⬜ Rotation/configChanges: filter survives
83. ✅ encode camera tripId with `/` and `%`
84. ✅ Parser settings threshold affects LoadUpdater
85. ✅ CSV export special characters
86. ✅ Strings key-set parity script (en vs ru vs default)
87. ✅ Replace remaining hardcoded SignUp password CD if any
88. ✅ PDF report headers: document EN-only or i18n
89. ✅ Lint crash-severity triage note (no full lint fix)
90. ✅ exportSchema=false documented risk
91. ⬜ Concurrent insertLoad + backup mutex (already) — stress test light
92. ✅ Photo watermark GPS unknown when no fix
93. ✅ ScanResult OCR collapse still works (regression)
94. ✅ Finish-date DatePicker UTC regression test (exists) keep green
95. ✅ AttachmentNaming sanitize regression
96. ✅ normalizeTripId CDC duplicate case test
97. ✅ WeekUtils null date parse (exists) keep green
98. ✅ Smoke script update: swipe-delete confirm + dispute EN
99. ✅ CompileDebug + full unit test suite green
100. ⬜ PR + backlog progress committed

---

Execute in order; mark ✅ in this file as batches land.
