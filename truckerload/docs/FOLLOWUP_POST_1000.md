# Post-QUALITY_1000 follow-ups

Branch: `cursor/post-1000-followups-f936` (after merge of PR #22 into `main`).

## Done in this pass

1. **True Room SQL paging for Home**
   - DAO: `pagingLoadsByWeek`, `pagingLoadsByDateRange`, `pagingLoadsByDate`, `pagingSearchLoads`, `pagingActiveDisputes`
   - `LoadRepository.pagingLoads(...)` → `Flow<PagingData<Load>>`
   - Home uses `collectAsLazyPagingItems` for period filters (not year-archive `ALL`)
2. **androidTest smoke**
   - `RoomLoadPagingInstrumentedTest` — Room `PagingSource` week page load
   - `MainActivitySmokeTest` — activity launches
3. **Release minify**
   - Fixed R8 missing-class rules for iText/SLF4J optional deps
   - `:app:assembleRelease` ✅ (`app-release-unsigned.apk`)
4. **Lint baseline refresh**
   - `updateLintBaseline` — `:app:lintDebug` ✅ (0 new / 260 baselined)

## Manual next (device)

- Install release/debug APK and smoke: Home week list scroll, Add Load, Settings Drive
- Run instrumented suite on emulator/device:
  `sh ./gradlew :app:connectedDebugAndroidTest`
