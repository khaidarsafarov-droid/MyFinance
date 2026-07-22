# Quality improvement backlog (50 tasks)

Generated for overnight execution. Priority: P0 data integrity → P1 bugs → P2 perf → P3 UX → P4 tests/security.

## P0 — Data integrity (1–10)

1. Wrap `insertLoad` / `updateLoad` in Room `withTransaction`
2. On `deleteLoad`, also delete/clear linked photos + scans (+ files)
3. Make `PhotoRepository.deletePhoto` delete the file on disk
4. On `deleteAllLoads` / backup restore wipe, purge photo & scan rows/files
5. Wrap backup restore in a single DB transaction
6. Add FK-safe orphan cleanup helper for photos/scans with missing loadId target
7. Fix CDC `syncLoadsCdc` to uppercase trip IDs like import
8. Add `normalizeTripId()` and use it in import + CDC + parsers entry points
9. Don’t invent “now” when ISO date parse fails (`dateStringToStartOfDayMillis` → nullable)
10. Fix callers of date millis helpers to handle null safely

## P1 — Correctness / UX bugs (11–25)

11. Distinct camera error strings for decode vs save
12. Log camera/scanner exceptions instead of empty `catch (_)`
13. Nullable location instead of `0.0,0.0` fake coords
14. Camera-for-load `onFinished` → `popBackStack()` (not always Home)
15. Confirm dialog before delete on LoadDetail
16. Confirm or undo snackbar for swipe-delete on Home
17. Encode `loadDetail` / `editLoad` path segments
18. Clear HomeViewModel optimistic overlay on delete
19. Re-evaluate current week yield on day change (don’t freeze week at subscribe)
20. ScannerScreen remove `!!` on pending/status
21. Cancel attached camera session: confirm remove photos from load
22. Debounce auto-backup scheduling (30–60s)
23. Queue or ignore second camera shutter without deleting silently (UI feedback)
24. Dispute + finish-date strings present in values-en and values-ru
25. Fix optimistic overlay so deleted loads cannot reappear

## P2 — Performance (26–35)

26. Thumbnail decode with `inSampleSize` in `rememberDecodedBitmap`
27. Cap camera watermark bitmap max edge (e.g. 2048)
28. `flowOn(Dispatchers.Default)` for Home filter/totals combine
29. `getLoadsForLinking` SQL LIMIT instead of load-all
30. Widget stats: use week SQL aggregates, not full hydrate
31. Chunk Analytics `getStopsByLoadIds` like LoadRepository
32. SQL filter for `PhotoRepository.watchPhotosFiltered`
33. Gate startup backfill behind one-shot SharedPreferences flag
34. Replace static `SimpleDateFormat` in PhotoManager with thread-safe formatter
35. Lower JPEG quality slightly for storage photos (e.g. 90)

## P3 — Accessibility / i18n / UX polish (36–42)

36. Add contentDescriptions for Settings actionable icons (batch)
37. Add contentDescriptions for Home toolbar / load card actions gaps
38. Move default `values/strings.xml` dispute keys to values-en + values-ru
39. Label social seed peers as demo when LOCAL_ONLY_MODE
40. Location-denied camera banner: clearer “continue without GPS” copy
41. After share from load-scoped camera, return to load detail (not only Home)
42. Scan gallery orphan PDF cleanup on open

## P4 — Tests & hardening (43–50)

43. Unit tests: `normalizeTripId` + CDC duplicate case
44. Unit tests: date parse null-on-invalid (WeekUtils)
45. Unit tests: AttachmentNaming + PhotoManager title fallback (pure logic)
46. In-memory Room test: deleteLoad clears photo rows
47. Unit test: CameraAttachContext resolves latest load tripId
48. Fix remaining broken unit test Stop.id types if any
49. Narrow FileProvider `files-path` away from `path="."`
50. Document quality backlog progress in commit messages; keep main green

---

Track status in commits on `cursor/quality-50-improvements-f936`.
