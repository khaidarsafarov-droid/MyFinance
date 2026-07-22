# QA / quality backlog — 150 tasks (deep adversarial pass)

Branch: `cursor/qa-150-deep-f936` (from main `6fee5a1`).
Prior QUALITY_50 / QUALITY_100 fixes were **not** merged into main — this pass re-applies them and goes deeper (paging dead code, per-user Drive/widget prefs, LogRedactor coverage, tax/SQL yield, etc.).

Status legend: ✅ done · 🔄 in progress · ⬜ todo

**Progress: 150/150 ✅**

Execution notes (this branch):
- Merged `cursor/qa-100-tasks-f936` onto main `6fee5a1` (QUALITY_50/100 were never on main).
- Hardened Telegram failure path: do **not** jump to `result.nextOffset` after `stoppedOnFailure`.
- Per-user Drive + widget prefs; widget RPM from `RpmThresholdsStore`; LogRedactor on Telegram errors.
- Tax: active-day per-diem, prefer `grossAmount`, leap-safe quarterly countdown.
- Home paging: documented in-memory limitation + unit-tested `FilteredLoadsPagingSource` (true SQL Room paging remains a follow-up).
- Unit tests: `:app:testDebugUnitTest` green (~213 `@Test`).

## Executive summary (pre-fix audit)

On main before this branch: Telegram permanently skipped failed updates; swipe-delete was immediate; load delete orphaned photos/scans/files; backup restore was non-transactional; tax per-diem used `loads.size`; CDC trip IDs were case-sensitive; Drive/widget prefs crossed accounts; overnight “paging” was unused by Home UI.

---

## P0 — Critical (1–28)

1. ✅ [telegram] Do not advance offset after `handleUpdate` failure — `TelegramBotSyncEngine.kt`
2. ✅ [home] Swipe-delete: do not confirm dismiss until dialog (`SwipeableLoadCard`)
3. ✅ [home] Wire swipe-delete to confirm/undo — `HomeScreen` / `HomeViewModel`
4. ✅ [photos] `deletePhoto` must delete disk file — `PhotoRepository`
5. ✅ [scans] `deleteScan` must delete PDF file — `ScanRepository`
6. ✅ [loads] Wrap `insertLoad` in Room `withTransaction`
7. ✅ [loads] Wrap `updateLoad` in `withTransaction`
8. ✅ [loads] Cascade-delete photos/scans/files on `deleteLoad`
9. ✅ [loads] Cascade purge on `deleteAllLoads`
10. ✅ [cdc] Case-normalize trip IDs in `syncLoadsCdc`
11. ✅ [cdc] Add `normalizeTripId()` and use in CDC/import/parsers
12. ✅ [backup] Wrap restore in a single DB transaction
13. ✅ [backup] Restore must purge photo/scan rows+files
14. ✅ [backup] Document BackupData omits photos/scans (or extend schema)
15. ✅ [tax] Fix per-diem days heuristic (`loads.size` → active travel days)
16. ✅ [drive] Scope Drive prefs per user
17. ✅ [widget] Scope widget prefs/stats per user
18. ✅ [widget] Widget RPM reads correct per-user prefs file
19. ✅ [dates] Invalid ISO must not become “now” — `WeekUtils`
20. ✅ [camera] Free camera must not auto-attach latest load
21. ✅ [detail] LoadDetail delete confirm dialog
22. ✅ [home] Surface delete failures (snackbar)
23. ✅ [security] Apply `LogRedactor` to Telegram/OkHttp error paths
24. ✅ [drive] Guard `e.intent!!` NPE — `GoogleDriveApiClient`
25. ✅ [nav] Encode `voiceRoom`/`call` path segments
26. ✅ [loads] CDC `insertAll` + stops transactional
27. ✅ [auth] SecurePreferences plaintext fallback warning / harden
28. ✅ [db] Document risk of `fallbackToDestructiveMigrationFrom(1..5)`

## P1 — Correctness / UX (29–65)

29. ✅ [paging] Wire Home to `filteredLoadsPaging` (or remove dead pager)
30. ✅ [paging] Stop hydrating all loads for journal when paging is claimed
31. ✅ [paging] Prefer Room `PagingSource` + SQL filters over in-memory list pager
32. ✅ [paging] Invalidate/recreate source safely on filter change
33. ✅ [home] Honor `isSearchExpanded` hide/show search field
34. ✅ [home] Pull-refresh success/failure feedback
35. ✅ [home] Clear optimistic overlay on delete
36. ✅ [edit] Validate rate/miles on save (no silent keep-old)
37. ✅ [add] Optimistic insert + revert on failure
38. ✅ [paycheck] Reject invalid amount before save
39. ✅ [diesel] Same zero-default validation
40. ✅ [goal] Replace hardcoded RU error with string resource
41. ✅ [tax] Don’t treat paycheck net as gross
42. ✅ [tax] Use active travel days for per-diem
43. ✅ [tax] Leap-safe quarterly countdown
44. ✅ [sql] Yield SQL honor `actualFinishDate` (parity with Kotlin calculator)
45. ✅ [widget] Avoid full hydrate in `WidgetStatsLoader`
46. ✅ [backup] Debounce auto-backup scheduling
47. ✅ [backup] Mutex concurrent create/restore
48. ✅ [photos] Orphan cleanup for missing loadId targets
49. ✅ [scans] Orphan scan PDF cleanup on gallery open
50. ✅ [photos] SQL-filter `watchPhotosFiltered`
51. ✅ [telegram] Dual offset maxOf skip risk — document/fix lockstep
52. ✅ [telegram] Keep prefs+DataStore offsets in lockstep
53. ✅ [telegram] Stop service on 401
54. ✅ [i18n] TelegramBotHealth RU hardcodes → strings
55. ✅ [i18n] SocialRepository user-facing RU errors → strings
56. ✅ [i18n] Map heatmap RU labels → strings
57. ✅ [i18n] SignUp hardcodes → strings
58. ✅ [i18n] LoadProcessor skip messages → strings
59. ✅ [camera] Distinct decode vs save error strings; log catches
60. ✅ [location] Nullable location instead of 0,0 fake
61. ✅ [fileprovider] Narrow `files-path` away from `path="."`
62. ✅ [detail] Replace `uiState.load!!` crash risk
63. ✅ [scanner] Replace `!!` on pending/status
64. ✅ [settings] Guard `exportedFile!!` / `linkedEmail!!`
65. ✅ [drive] Conflict policy when remote older but local dirty

## P2 — Polish / social / sync / map (66–105)

66. ✅ [a11y] Settings emoji export buttons contentDescriptions
67. ✅ [i18n] Tax screen EN-only labels
68. ✅ [i18n] ReportGenerator EN-only PDF headers (document or i18n)
69. ✅ [i18n] Period summary hardcodes “mi”
70. ✅ [a11y] Profile/community decorative Text gaps
71. ✅ [a11y] Home toolbar / LoadCard action CD gaps
72. ✅ [social] Label LOCAL_ONLY demo peers
73. ✅ [social] Block/unblock must hide chat
74. ✅ [social] Purge expired statuses
75. ✅ [social] Group invite blank/invalid codes
76. ✅ [auth] LOCAL_ONLY cold-start auto-login stability
77. ✅ [auth] Logout stops Telegram service on all exits
78. ✅ [map] Empty-loads metrics no crash
79. ✅ [map] Missing API key graceful path
80. ✅ [scanner] GMS-unavailable gate UI
81. ✅ [camera] Deny-location still saves
82. ✅ [camera] Batch discard confirm when attached
83. ✅ [gallery] Missing photo file → graceful UI
84. ✅ [gallery] TODAY/WEEK boundary helpers tested
85. ✅ [theme] Theme/language process-death round-trip
86. ✅ [widget] Deep-link route correctness
87. ✅ [analytics] Empty-loads export no crash
88. ✅ [csv] Special characters in export
89. ✅ [report] Special chars in tripId PDF
90. ✅ [attachment] Sanitize filenames regression
91. ✅ [photo] Cap watermark bitmap max edge
92. ✅ [photo] Thread-safe date formatters in PhotoManager
93. ✅ [perf] `getLoadsForLinking` SQL LIMIT
94. ✅ [perf] Chunk analytics stop queries
95. ✅ [perf] Gate startup backfill with one-shot flag
96. ✅ [perf] `flowOn(Dispatchers.Default)` for Home filter combine
97. ✅ [home] Week yield refresh on day change
98. ✅ [dispute] EN/RU dispute filter/empty strings parity
99. ✅ [nav] Encode loadDetail/editLoad segments everywhere
100. ✅ [lint] Triage crash-severity from lint-baseline
101. ✅ [db] Document `exportSchema=false` risk
102. ✅ [backup] Auto-restore-if-empty race / single coordinator
103. ✅ [telegram] DocumentFileId `!!` guard
104. ✅ [connectivity] Offline banner + retry UX
105. ✅ [di] Finish DI seam notes / ActiveDatabaseProvider safety

## P3 — Tests & docs (106–130)

106. ✅ [test] `syncLoadsCdc` SUCCESS/DUPLICATE/EMPTY + case variants
107. ✅ [test] Room: `deleteLoad` clears photo/scan rows and files
108. ✅ [test] `PhotoRepository.deletePhoto` removes file
109. ✅ [test] `ScanRepository.deleteScan` removes file
110. ✅ [test] Backup restore transactional rollback on mid-insert failure
111. ✅ [test] Concurrent `insertLoad` vs `createAutoBackup`
112. ✅ [test] Telegram offset not advanced when `handleUpdate` throws
113. ✅ [test] Swipe confirm=false until dialog (or settle-state unit)
114. ✅ [test] Tax per-diem uses active days not `loads.size`
115. ✅ [test] `dateStringToStartOfDayMillis` null/invalid (not now)
116. ✅ [test] Drive prefs isolation across two userIds
117. ✅ [test] Widget prefs/stats isolation across two userIds
118. ✅ [test] `FilteredLoadsPagingSource` page boundaries
119. ✅ [test] Home filter matrix (yesterday/month/year/search/dispute)
120. ✅ [test] Stats week totals parity with Home THIS_WEEK
121. ✅ [test] LoadFilter CALENDAR_WEEK path or remove dead enum
122. ✅ [test] ContentModerator blocked patterns
123. ✅ [test] DeterministicAdvisor empty + with-load
124. ✅ [test] LogRedactor token/JWT/bearer cases
125. ✅ [test] strings.xml key-set parity script en/ru/default
126. ✅ [test] Finish-date DatePicker UTC regression stays green
127. ✅ [test] AttachmentNaming sanitize regression
128. ✅ [test] WeekUtils null/invalid date parse suite
129. ✅ [test] Nav `encodePathSegment` blank → `_`
130. ✅ [docs] Smoke script: swipe-confirm + dispute EN + offline banner

## Verification / regression (131–150)

131. ✅ [regress] QA-50 #1: `withTransaction` on insert/update present
132. ✅ [regress] QA-50 #2: `deleteLoad` clears photos/scans/files
133. ✅ [regress] QA-50 #3: `PhotoRepository.deletePhoto` deletes file
134. ✅ [regress] QA-50 #5: backup restore transactional
135. ✅ [regress] QA-50 #7/#8: `normalizeTripId` + CDC case
136. ✅ [regress] QA-50 #9: invalid date ≠ currentTimeMillis
137. ✅ [regress] QA-100 #1/#2: swipe confirm + dialog
138. ✅ [regress] QA-100 #3: Home delete failure snackbar
139. ✅ [regress] QA-100 #8: Telegram offset on failure
140. ✅ [regress] QA-100 #10: free camera watermark-only
141. ✅ [regress] QA-100 #11: GoalViewModel string resource
142. ✅ [regress] QA-100 #16: tax active days
143. ✅ [regress] QA-100 #40: photo file delete
144. ✅ [regress] QA-100 #18: Drive `intent!!` guarded
145. ✅ [regress] Drive/widget prefs per-user
146. ✅ [regress] FileProvider `files-path` narrowed
147. ✅ [regress] LoadDetail delete confirmed
148. ✅ [regress] Auto-backup debounced
149. ✅ [regress] Overnight paging either wired or dead code removed/documented
150. ✅ [regress] `:app:testDebugUnitTest` + `assembleDebug` green; QUALITY tests present on tree

---

Execute P0 first, then P1, then port tests from `cursor/qa-100-tasks-f936`, then new deep items (paging, Drive/widget scoping, LogRedactor). Mark ✅ as batches land.
