# QUALITY_1000 — mega improvement backlog

Branch: `cursor/qa-1000-mega-f936`. Package: `com.truckerload`.
Generated from post-QUALITY_150 adversarial scan + lint-baseline instances.

Status legend: ✅ done · 🔄 in progress · ⬜ todo

**Progress: 487/1100**

## Categories
- A: integrity / architecture / crashes
- B-lint / B-deps: lint-baseline burn-down
- C-perf: Flows, lifecycle collection, hydrate
- D-ux: i18n hardcodes
- E-test / E-sec: coverage + security
- F-verify / F-a11y / G-quality: file-level quality gates

---

1. ✅ [A] Harden allowBackup=false in AndroidManifest
2. ✅ [A] Document SecurePreferences plaintextFallbackUsed UI warning path
3. ✅ [A] Enable release minify with keep rules for Gson/Room/Compose
4. ✅ [A] Bump exportSchema consideration / document risk (already noted)
5. ✅ [A] Add Room indexes on diesel(weekNumber,year)
6. ✅ [A] Add Room indexes on paychecks(weekNumber,year)
7. ✅ [A] Add Room indexes on voice_rooms if queried
8. ✅ [A] Add LoadEntity index on isDispute if filtered in SQL
9. ✅ [A] Add LoadEntity index on firstPuMillis for date-range queries
10. ✅ [A] Bump DB version + migration for new indexes
11. ⬜ [A] Wire Home to reduce full hydrate or document SQL paging follow-up
12. ⬜ [A] Remove or use filteredLoadsPaging in HomeScreen
13. ⬜ [A] Split SocialRepository into Chat/Profile/Challenge modules (start extract)
14. ⬜ [A] Extract SettingsScreen Drive section to composable
15. ⬜ [A] Extract StatsScreen chart section to composable
16. ✅ [A] Add OptIn FlowPreview where debounce used
17. ✅ [A] Log swallowed catch at MainActivity.kt:103 — `app/src/main/java/com/truckerload/presentation/MainActivity.kt`
18. ✅ [A] Log swallowed catch at TelegramServiceRestarter.kt:29 — `app/src/main/java/com/truckerload/sync/TelegramServiceRestarter.kt`
19. ✅ [A] Log swallowed catch at BatteryOptimizationHelper.kt:28 — `app/src/main/java/com/truckerload/utils/BatteryOptimizationHelper.kt`
20. ✅ [A] Log swallowed catch at BatteryOptimizationHelper.kt:35 — `app/src/main/java/com/truckerload/utils/BatteryOptimizationHelper.kt`
21. ✅ [A] Log swallowed catch at ExportService.kt:64 — `app/src/main/java/com/truckerload/utils/ExportService.kt`
22. ✅ [A] Log swallowed catch at OCRService.kt:53 — `app/src/main/java/com/truckerload/utils/OCRService.kt`
23. ✅ [A] Log swallowed catch at TessDataManager.kt:43 — `app/src/main/java/com/truckerload/utils/TessDataManager.kt`
24. ✅ [A] Log swallowed catch at TesseractOCRService.kt:51 — `app/src/main/java/com/truckerload/utils/TesseractOCRService.kt`
25. ✅ [A] Log swallowed catch at WeekUtils.kt:550 — `app/src/main/java/com/truckerload/utils/WeekUtils.kt`
26. ✅ [A] Log swallowed catch at SupabaseAuthService.kt:115 — `app/src/main/java/com/truckerload/data/remote/SupabaseAuthService.kt`
27. ✅ [A] Log swallowed catch at SupabaseAuthService.kt:245 — `app/src/main/java/com/truckerload/data/remote/SupabaseAuthService.kt`
28. ✅ [A] Log swallowed catch at SupabaseAuthService.kt:254 — `app/src/main/java/com/truckerload/data/remote/SupabaseAuthService.kt`
29. ✅ [A] Log swallowed catch at SupabaseAuthService.kt:344 — `app/src/main/java/com/truckerload/data/remote/SupabaseAuthService.kt`
30. ✅ [A] Log swallowed catch at ParseUtils.kt:66 — `app/src/main/java/com/truckerload/domain/parser/ParseUtils.kt`
31. ✅ [A] Log swallowed catch at ForecastService.kt:39 — `app/src/main/java/com/truckerload/domain/usecase/ForecastService.kt`
32. ✅ [A] Log swallowed catch at FuelAnalyticsService.kt:36 — `app/src/main/java/com/truckerload/domain/usecase/FuelAnalyticsService.kt`
33. ✅ [A] Log swallowed catch at TelegramJsonExportParser.kt:62 — `app/src/main/java/com/truckerload/domain/import/parser/TelegramJsonExportParser.kt`
34. ✅ [A] Log swallowed catch at ImportLoadsUseCase.kt:53 — `app/src/main/java/com/truckerload/domain/import/usecase/ImportLoadsUseCase.kt`
35. ✅ [A] Log swallowed catch at ImportLoadsUseCase.kt:80 — `app/src/main/java/com/truckerload/domain/import/usecase/ImportLoadsUseCase.kt`
36. ✅ [A] Log swallowed catch at ImportLoadsUseCase.kt:110 — `app/src/main/java/com/truckerload/domain/import/usecase/ImportLoadsUseCase.kt`
37. ✅ [A] Log swallowed catch at GoogleSignInLauncher.kt:313 — `app/src/main/java/com/truckerload/presentation/auth/GoogleSignInLauncher.kt`
38. ✅ [A] Log swallowed catch at GoogleMapsHeatmapCard.kt:317 — `app/src/main/java/com/truckerload/presentation/components/GoogleMapsHeatmapCard.kt`
39. ✅ [A] Log swallowed catch at CameraScreen.kt:171 — `app/src/main/java/com/truckerload/presentation/screens/camera/CameraScreen.kt`
40. ✅ [A] Log swallowed catch at LoginScreen.kt:88 — `app/src/main/java/com/truckerload/presentation/screens/login/LoginScreen.kt`
41. ✅ [A] Log swallowed catch at ScannerViewModel.kt:97 — `app/src/main/java/com/truckerload/presentation/screens/scanner/ScannerViewModel.kt`
42. ✅ [A] Log swallowed catch at ScannerViewModel.kt:182 — `app/src/main/java/com/truckerload/presentation/screens/scanner/ScannerViewModel.kt`
43. ✅ [A] Log swallowed catch at ScannerViewModel.kt:241 — `app/src/main/java/com/truckerload/presentation/screens/scanner/ScannerViewModel.kt`
44. ✅ [A] Replace !! at MainActivity.kt:134 — `userId = userId!!,`
45. ✅ [A] Replace !! at MainActivity.kt:189 — `val deps = dependencies!!`
46. ✅ [A] Replace !! at TelegramBotSyncEngine.kt:388 — `val fileId = update.documentFileId!!`
47. ✅ [A] Replace !! at AppDatabase.kt:140 — `if (INSTANCE != null && currentUserId == id) return INSTANCE!!`
48. ✅ [A] Replace !! at ImportLoadsUseCase.kt:180 — `val processing = loadProcessor!!.processLoad(`
49. ✅ [A] Replace !! at LoadDetailScreen.kt:177 — `val l = uiState.load!!`
50. ✅ [A] Replace !! at SettingsScreen.kt:610 — `val file = exportedFile!!`
51. ✅ [A] Replace !! at SettingsScreen.kt:878 — `stringResource(R.string.drive_sync_status_on, linkedEmail!!)`
52. ⬜ [A] Audit dead/legacy Forecast code and remove or wire
53. ⬜ [A] Audit dead/legacy Fuel code and remove or wire
54. ⬜ [A] Audit dead/legacy HybridOcr code and remove or wire
55. ⬜ [A] Audit dead/legacy Gold code and remove or wire
56. ✅ [C-perf] Use collectAsStateWithLifecycle at MainActivity.kt:118 — `app/src/main/java/com/truckerload/presentation/MainActivity.kt`
57. ✅ [C-perf] Use collectAsStateWithLifecycle at MainActivity.kt:119 — `app/src/main/java/com/truckerload/presentation/MainActivity.kt`
58. ✅ [C-perf] Use collectAsStateWithLifecycle at MainActivity.kt:155 — `app/src/main/java/com/truckerload/presentation/MainActivity.kt`
59. ✅ [C-perf] Use collectAsStateWithLifecycle at LoadCard.kt:70 — `app/src/main/java/com/truckerload/presentation/components/LoadCard.kt`
60. ✅ [C-perf] Use collectAsStateWithLifecycle at RpmColorLegend.kt:33 — `app/src/main/java/com/truckerload/presentation/components/RpmColorLegend.kt`
61. ✅ [C-perf] Use collectAsStateWithLifecycle at NavGraph.kt:142 — `app/src/main/java/com/truckerload/presentation/navigation/NavGraph.kt`
62. ✅ [C-perf] Use collectAsStateWithLifecycle at NavGraph.kt:219 — `app/src/main/java/com/truckerload/presentation/navigation/NavGraph.kt`
63. ✅ [C-perf] Use collectAsStateWithLifecycle at AddDieselScreen.kt:73 — `app/src/main/java/com/truckerload/presentation/screens/add/AddDieselScreen.kt`
64. ✅ [C-perf] Use collectAsStateWithLifecycle at AddLoadScreen.kt:58 — `app/src/main/java/com/truckerload/presentation/screens/add/AddLoadScreen.kt`
65. ✅ [C-perf] Use collectAsStateWithLifecycle at AddPaycheckScreen.kt:72 — `app/src/main/java/com/truckerload/presentation/screens/add/AddPaycheckScreen.kt`
66. ✅ [C-perf] Use collectAsStateWithLifecycle at FinancialAdvisorScreen.kt:91 — `app/src/main/java/com/truckerload/presentation/screens/advisor/FinancialAdvisorScreen.kt`
67. ✅ [C-perf] Use collectAsStateWithLifecycle at AnalyticsScreen.kt:80 — `app/src/main/java/com/truckerload/presentation/screens/analytics/AnalyticsScreen.kt`
68. ✅ [C-perf] Use collectAsStateWithLifecycle at ProfileSetupScreen.kt:64 — `app/src/main/java/com/truckerload/presentation/screens/auth/ProfileSetupScreen.kt`
69. ✅ [C-perf] Use collectAsStateWithLifecycle at CameraScreen.kt:72 — `app/src/main/java/com/truckerload/presentation/screens/camera/CameraScreen.kt`
70. ✅ [C-perf] Use collectAsStateWithLifecycle at PhotoPreviewScreen.kt:204 — `app/src/main/java/com/truckerload/presentation/screens/camera/PhotoPreviewScreen.kt`
71. ✅ [C-perf] Use collectAsStateWithLifecycle at LoadDetailScreen.kt:98 — `app/src/main/java/com/truckerload/presentation/screens/detail/LoadDetailScreen.kt`
72. ✅ [C-perf] Use collectAsStateWithLifecycle at LoadDetailScreen.kt:99 — `app/src/main/java/com/truckerload/presentation/screens/detail/LoadDetailScreen.kt`
73. ✅ [C-perf] Use collectAsStateWithLifecycle at LoadDetailScreen.kt:100 — `app/src/main/java/com/truckerload/presentation/screens/detail/LoadDetailScreen.kt`
74. ✅ [C-perf] Use collectAsStateWithLifecycle at EditLoadScreen.kt:63 — `app/src/main/java/com/truckerload/presentation/screens/edit/EditLoadScreen.kt`
75. ✅ [C-perf] Use collectAsStateWithLifecycle at PhotoGalleryScreen.kt:53 — `app/src/main/java/com/truckerload/presentation/screens/gallery/PhotoGalleryScreen.kt`
76. ✅ [C-perf] Use collectAsStateWithLifecycle at PhotoGalleryScreen.kt:54 — `app/src/main/java/com/truckerload/presentation/screens/gallery/PhotoGalleryScreen.kt`
77. ✅ [C-perf] Use collectAsStateWithLifecycle at WeeklyGoalScreen.kt:84 — `app/src/main/java/com/truckerload/presentation/screens/goal/WeeklyGoalScreen.kt`
78. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:115 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
79. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:116 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
80. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:128 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
81. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:129 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
82. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:130 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
83. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:131 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
84. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:134 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
85. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:175 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
86. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:368 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
87. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:369 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
88. ✅ [C-perf] Use collectAsStateWithLifecycle at HomeScreen.kt:370 — `app/src/main/java/com/truckerload/presentation/screens/home/HomeScreen.kt`
89. ✅ [C-perf] Use collectAsStateWithLifecycle at MapScreen.kt:49 — `app/src/main/java/com/truckerload/presentation/screens/map/MapScreen.kt`
90. ✅ [C-perf] Use collectAsStateWithLifecycle at ScanGalleryScreen.kt:59 — `app/src/main/java/com/truckerload/presentation/screens/scanner/ScanGalleryScreen.kt`
91. ✅ [C-perf] Use collectAsStateWithLifecycle at ScannerScreen.kt:56 — `app/src/main/java/com/truckerload/presentation/screens/scanner/ScannerScreen.kt`
92. ✅ [C-perf] Use collectAsStateWithLifecycle at ParserSettings.kt:42 — `app/src/main/java/com/truckerload/presentation/screens/settings/ParserSettings.kt`
93. ✅ [C-perf] Use collectAsStateWithLifecycle at ParserSettings.kt:43 — `app/src/main/java/com/truckerload/presentation/screens/settings/ParserSettings.kt`
94. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:107 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
95. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:108 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
96. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:118 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
97. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:119 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
98. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:120 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
99. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:121 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
100. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:151 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
101. ✅ [C-perf] Use collectAsStateWithLifecycle at SettingsScreen.kt:826 — `app/src/main/java/com/truckerload/presentation/screens/settings/SettingsScreen.kt`
102. ✅ [C-perf] Use collectAsStateWithLifecycle at CommunityScreen.kt:89 — `app/src/main/java/com/truckerload/presentation/screens/social/CommunityScreen.kt`
103. ✅ [C-perf] Use collectAsStateWithLifecycle at CommunityScreen.kt:90 — `app/src/main/java/com/truckerload/presentation/screens/social/CommunityScreen.kt`
104. ✅ [C-perf] Use collectAsStateWithLifecycle at CommunityScreen.kt:91 — `app/src/main/java/com/truckerload/presentation/screens/social/CommunityScreen.kt`
105. ✅ [C-perf] Use collectAsStateWithLifecycle at GroupDetailScreen.kt:45 — `app/src/main/java/com/truckerload/presentation/screens/social/GroupDetailScreen.kt`
106. ✅ [C-perf] Use collectAsStateWithLifecycle at GroupsScreen.kt:54 — `app/src/main/java/com/truckerload/presentation/screens/social/GroupsScreen.kt`
107. ✅ [C-perf] Use collectAsStateWithLifecycle at GroupsScreen.kt:55 — `app/src/main/java/com/truckerload/presentation/screens/social/GroupsScreen.kt`
108. ✅ [C-perf] Use collectAsStateWithLifecycle at PeerProfileScreen.kt:53 — `app/src/main/java/com/truckerload/presentation/screens/social/PeerProfileScreen.kt`
109. ✅ [C-perf] Use collectAsStateWithLifecycle at ProfileEditScreen.kt:57 — `app/src/main/java/com/truckerload/presentation/screens/social/ProfileEditScreen.kt`
110. ✅ [C-perf] Use collectAsStateWithLifecycle at ProfileScreen.kt:73 — `app/src/main/java/com/truckerload/presentation/screens/social/ProfileScreen.kt`
111. ✅ [C-perf] Use collectAsStateWithLifecycle at SocialChatScreen.kt:82 — `app/src/main/java/com/truckerload/presentation/screens/social/SocialChatScreen.kt`
112. ✅ [C-perf] Use collectAsStateWithLifecycle at StatusScreen.kt:76 — `app/src/main/java/com/truckerload/presentation/screens/social/StatusScreen.kt`
113. ✅ [C-perf] Use collectAsStateWithLifecycle at StatusScreen.kt:80 — `app/src/main/java/com/truckerload/presentation/screens/social/StatusScreen.kt`
114. ✅ [C-perf] Use collectAsStateWithLifecycle at StatsScreen.kt:127 — `app/src/main/java/com/truckerload/presentation/screens/stats/StatsScreen.kt`
115. ✅ [C-perf] Use collectAsStateWithLifecycle at StatsScreen.kt:128 — `app/src/main/java/com/truckerload/presentation/screens/stats/StatsScreen.kt`
116. ✅ [C-perf] Use collectAsStateWithLifecycle at StatsScreen.kt:144 — `app/src/main/java/com/truckerload/presentation/screens/stats/StatsScreen.kt`
117. ✅ [C-perf] Use collectAsStateWithLifecycle at TaxTrackerScreen.kt:58 — `app/src/main/java/com/truckerload/presentation/screens/tax/TaxTrackerScreen.kt`
118. ✅ [C-perf] Use collectAsStateWithLifecycle at CallScreens.kt:58 — `app/src/main/java/com/truckerload/presentation/screens/voice/CallScreens.kt`
119. ✅ [C-perf] Use collectAsStateWithLifecycle at CallScreens.kt:147 — `app/src/main/java/com/truckerload/presentation/screens/voice/CallScreens.kt`
120. ✅ [C-perf] Use collectAsStateWithLifecycle at VoiceScreens.kt:89 — `app/src/main/java/com/truckerload/presentation/screens/voice/VoiceScreens.kt`
121. ✅ [C-perf] Use collectAsStateWithLifecycle at VoiceScreens.kt:212 — `app/src/main/java/com/truckerload/presentation/screens/voice/VoiceScreens.kt`
122. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at AiRepository.kt:24 — `): Flow<String> = advisorService.chatStream(history, userMessage, appContext)`
123. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at DieselRepository.kt:16 — `fun getAllDiesel(): Flow<List<Diesel>> =`
124. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at DieselRepository.kt:19 — `fun getDieselForWeek(weekNumber: Int, year: Int): Flow<List<Diesel>> =`
125. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:58 — `fun getAllLoads(): Flow<List<Load>> =`
126. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:61 — `fun watchTotalLoadStats(): Flow<LoadStatsAgg> =`
127. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:64 — `fun watchWeeklyLoadStats(weekNumber: Int, year: Int): Flow<WeeklyLoadStatsAgg> =`
128. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:71 — `fun watchLoads(): Flow<List<Load>> = getAllLoads()`
129. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:74 — `fun watchCurrentWeekYieldSnapshot(): Flow<WeekYieldSnapshot> =`
130. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:115 — `fun watchActualDailyYield(weekNumber: Int, year: Int): Flow<Double> =`
131. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:118 — `fun getLoadsByMonth(monthPrefix: String): Flow<List<Load>> =`
132. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:121 — `fun searchLoads(query: String): Flow<List<Load>> =`
133. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:124 — `fun getLoadsByWeek(weekNumber: Int, year: Int): Flow<List<Load>> =`
134. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:128 — `fun getLoadsByDate(loadDate: String): Flow<List<Load>> =`
135. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadRepository.kt:132 — `fun getLoadsByDateRange(startDate: String, endDate: String): Flow<List<Load>> =`
136. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PaycheckRepository.kt:17 — `fun getAllPaychecks(): Flow<List<Paycheck>> =`
137. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PaycheckRepository.kt:23 — `fun getPaychecksForWeek(weekNumber: Int, year: Int): Flow<List<Paycheck>> =`
138. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PhotoRepository.kt:13 — `fun watchPhotos(): Flow<List<PhotoEntity>> = photoDao.getAllPhotos()`
139. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PhotoRepository.kt:15 — `fun watchPhotosByLoadId(loadId: String): Flow<List<PhotoEntity>> =`
140. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PhotoRepository.kt:22 — `): Flow<List<PhotoEntity>> =`
141. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at ScanRepository.kt:13 — `fun watchScans(): Flow<List<ScanEntity>> = scanDao.getAllScans()`
142. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at ScanRepository.kt:15 — `fun watchScansByLoadId(loadId: String): Flow<List<ScanEntity>> =`
143. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:261 — `fun watchMyEnhancedProfile(): Flow<EnhancedDriverProfile> =`
144. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:276 — `fun watchMyProfile(): Flow<DriverProfile> =`
145. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:363 — `fun watchChats(): Flow<List<SocialChat>> =`
146. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:372 — `fun watchPublicGroups(): Flow<List<SocialChat>> =`
147. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:383 — `fun watchPeers(): Flow<List<SocialPeerProfile>> =`
148. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:392 — `fun watchChatsSearch(query: String): Flow<List<SocialChat>> {`
149. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:408 — `fun watchTotalUnread(): Flow<Int> = chatDao.watchTotalUnread()`
150. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:410 — `fun watchMessages(chatId: String, limit: Int = MESSAGE_PAGE_SIZE): Flow<List<SocialMessage`
151. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:503 — `fun recommendGroups(): Flow<List<SocialChat>> =`
152. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:623 — `fun watchIsBlocked(targetId: String): Flow<Boolean> =`
153. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:627 — `fun watchFriendStatuses(): Flow<List<DriverStatusPost>> =`
154. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:720 — `fun watchGroupMembers(chatId: String): Flow<List<ChatMember>> =`
155. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:787 — `fun watchIsFollowing(targetId: String): Flow<Boolean> =`
156. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:790 — `fun watchPeer(peerId: String): Flow<SocialPeerProfile?> =`
157. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialRepository.kt:796 — `fun watchLeaderboard(category: LeaderboardCategory = LeaderboardCategory.OVERALL): Flow<Li`
158. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceRepository.kt:47 — `fun watchRooms(): Flow<List<VoiceRoom>> =`
159. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceRepository.kt:60 — `fun watchRoom(roomId: String, myName: String): Flow<VoiceRoom?> =`
160. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceRepository.kt:160 — `fun watchIncomingCall(): Flow<CallState?> =`
161. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceRepository.kt:163 — `fun watchCall(callId: String): Flow<CallState?> =`
162. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at WeekRepository.kt:17 — `fun getWeekSummary(weekNumber: Int, year: Int): Flow<WeekSummary> {`
163. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SignalingService.kt:13 — `fun watchSignals(sessionId: String, excludeUserId: String): Flow<List<Signal>>`
164. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SignalingService.kt:36 — `override fun watchSignals(sessionId: String, excludeUserId: String): Flow<List<Signal>> =`
165. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at DieselDao.kt:14 — `fun getAllDiesel(): Flow<List<DieselEntity>>`
166. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at DieselDao.kt:17 — `fun getDieselForWeek(weekNumber: Int, year: Int): Flow<List<DieselEntity>>`
167. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:54 — `fun getAllLoads(): Flow<List<LoadEntity>>`
168. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:63 — `fun getLoadsByMonth(monthPrefix: String): Flow<List<LoadEntity>>`
169. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:66 — `fun searchLoads(query: String): Flow<List<LoadEntity>>`
170. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:75 — `fun getLoadsByWeek(weekNumber: Int, year: Int): Flow<List<LoadEntity>>`
171. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:129 — `fun getLoadsByDate(loadDate: String): Flow<List<LoadEntity>>`
172. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:133 — `fun getLoadsByDateRange(startDate: String, endDate: String): Flow<List<LoadEntity>>`
173. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:168 — `fun watchActualDailyYield(weekNumber: Int, year: Int): Flow<Double>`
174. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:187 — `fun watchWeekYieldAgg(weekNumber: Int, year: Int): Flow<WeekYieldAgg>`
175. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:242 — `fun watchTotalLoadStats(): Flow<LoadStatsAgg>`
176. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at LoadDao.kt:254 — `fun watchWeeklyLoadStats(weekNumber: Int, year: Int): Flow<WeeklyLoadStatsAgg>`
177. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PaycheckDao.kt:14 — `fun getAllPaychecks(): Flow<List<PaycheckEntity>>`
178. ✅ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PaycheckDao.kt:20 — `fun getPaychecksForWeek(weekNumber: Int, year: Int): Flow<List<PaycheckEntity>>`
179. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PhotoDao.kt:17 — `fun getAllPhotos(): Flow<List<PhotoEntity>>`
180. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PhotoDao.kt:26 — `fun getPhotosByLoadId(loadId: String): Flow<List<PhotoEntity>>`
181. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PhotoDao.kt:44 — `): Flow<List<PhotoEntity>>`
182. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at PhotoDao.kt:47 — `fun getUnlinkedPhotos(): Flow<List<PhotoEntity>>`
183. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at ScanDao.kt:17 — `fun getAllScans(): Flow<List<ScanEntity>>`
184. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at ScanDao.kt:26 — `fun getScansByLoadId(loadId: String): Flow<List<ScanEntity>>`
185. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:22 — `fun watchProfile(id: String = DriverProfileEntity.LOCAL_USER_ID): Flow<DriverProfileEntity`
186. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:34 — `fun watchChats(): Flow<List<SocialChatEntity>>`
187. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:45 — `fun watchChatsSearch(query: String): Flow<List<SocialChatEntity>>`
188. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:72 — `fun watchTotalUnread(): Flow<Int>`
189. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:85 — `fun watchRecentMessages(chatId: String, limit: Int): Flow<List<SocialMessageEntity>>`
190. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:88 — `fun watchMessages(chatId: String): Flow<List<SocialMessageEntity>>`
191. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:137 — `fun watchReactionsForChat(chatId: String): Flow<List<MessageReactionEntity>>`
192. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:149 — `fun watchBlockedIds(userId: String): Flow<List<String>>`
193. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:164 — `fun watchActiveStatuses(now: Long): Flow<List<DriverStatusEntity>>`
194. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:200 — `fun watchIsFollowing(followerId: String, followingId: String): Flow<Boolean>`
195. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:209 — `fun watchFollowingIds(followerId: String): Flow<List<String>>`
196. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:215 — `fun watchMembers(chatId: String): Flow<List<ChatMemberEntity>>`
197. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:230 — `fun watchMemberChatIds(userId: String): Flow<List<String>>`
198. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:239 — `fun watchAll(): Flow<List<SocialPeerEntity>>`
199. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at SocialDaos.kt:251 — `fun watchById(peerId: String): Flow<SocialPeerEntity?>`
200. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceDaos.kt:16 — `fun watchActiveRooms(): Flow<List<VoiceRoomEntity>>`
201. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceDaos.kt:31 — `fun watchParticipants(roomId: String): Flow<List<VoiceRoomParticipantEntity>>`
202. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceDaos.kt:34 — `fun watchAllParticipants(): Flow<List<VoiceRoomParticipantEntity>>`
203. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceDaos.kt:58 — `fun watchIncomingCall(): Flow<CallSessionEntity?>`
204. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceDaos.kt:64 — `fun watchCall(callId: String): Flow<CallSessionEntity?>`
205. ⬜ [C-perf] Add flowOn(Dispatchers.IO) for Flow at VoiceDaos.kt:76 — `fun watchSignals(sessionId: String): Flow<List<VoiceSignalEntity>>`
206. ⬜ [C-perf] Home: avoid hydrating all loads for journal when filter is THIS_WEEK
207. ⬜ [C-perf] Widget already week-scoped — verify no getAllLoadsOnce
208. ⬜ [C-perf] Photo watchPhotosFiltered SQL filter verify
209. ⬜ [C-perf] Chunk large IN queries everywhere
210. ⬜ [C-perf] DecodeSampledBitmap already — audit remaining full bitmap loads
211. ⬜ [C-perf] LazyColumn keys for remaining unkeyed lists
212. ⬜ [C-perf] derivedStateOf for Home totals derived from filter
213. ⬜ [C-perf] remember keys audit on HomeScreen periodSummary
214. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotSyncEngine.kt:106) — `📥 Получено ${result.updates.size} обновлений (rawMax=${resul`
215. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotSyncEngine.kt:112) — `⏭️ Пропуск уже обработанного updateId=${update.updateId}`
216. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotSyncEngine.kt:116) — `📥 Обработка updateId=${update.updateId}`
217. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:58) — `📝🚛 TRUCK LOG — ВСЕ РЕЙСЫ`
218. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:60) — `Дата экспорта: ${formatExportTimestamp()}`
219. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:61) — `Всего рейсов: $count`
220. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:67) — `📦 РЕЙСЫ (${loads.size})`
221. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:88) — `📊 ИТОГО:`
222. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:90) — `Всего рейсов: $count`
223. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:91) — `Общий доход: ${formatMoney(totalIncome)}`
224. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:92) — `Общие мили: ${formatMilesValue(totalMiles)}`
225. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:93) — `Средний доход за рейс: ${formatMoney(avgIncome)}`
226. ⬜ [D-ux] Extract hardcoded string to resources (LoadExporter.kt:94) — `Средняя цена за милю: ${formatRpm(avgRpm)}`
227. ⬜ [D-ux] Extract hardcoded string to resources (LoadImporter.kt:132) — `Дата экспорта`
228. ⬜ [D-ux] Extract hardcoded string to resources (LoadImporter.kt:133) — `Всего рейсов`
229. ⬜ [D-ux] Extract hardcoded string to resources (LoadImporter.kt:133) — `Общий доход`
230. ⬜ [D-ux] Extract hardcoded string to resources (LoadImporter.kt:134) — `Общие мили`
231. ⬜ [D-ux] Extract hardcoded string to resources (LoadImporter.kt:134) — `Средний`
232. ⬜ [D-ux] Extract hardcoded string to resources (LoadImporter.kt:135) — `Средняя`
233. ⬜ [D-ux] Extract hardcoded string to resources (WidgetStatsFormatter.kt:18) — ` миль`
234. ⬜ [D-ux] Extract hardcoded string to resources (WidgetStatsFormatter.kt:50) — `$0/день`
235. ⬜ [D-ux] Extract hardcoded string to resources (WidgetStatsFormatter.kt:53) — `$%,.2f/день`
236. ⬜ [D-ux] Extract hardcoded string to resources (WidgetStatsFormatter.kt:55) — `$%,.0f/день`
237. ⬜ [D-ux] Extract hardcoded string to resources (DatabaseMigrations.kt:273) — `TEXT NOT NULL DEFAULT 'Русский,Английский'`
238. ⬜ [D-ux] Extract hardcoded string to resources (DatabaseMigrations.kt:456) — `TEXT NOT NULL DEFAULT 'Русский,Английский'`
239. ⬜ [D-ux] Extract hardcoded string to resources (CredentialManagerGoogleSignIn.kt:34) — `GOOGLE_WEB_CLIENT_ID не настроен`
240. ⬜ [D-ux] Extract hardcoded string to resources (CredentialManagerGoogleSignIn.kt:65) — `Не удалось получить ID token`
241. ⬜ [D-ux] Extract hardcoded string to resources (SupabaseAuthService.kt:71) — `лимит отправки`
242. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:14) — `📋 Помощь`
243. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:15) — `📊 Статус`
244. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:18) — `📦 Лоуд`
245. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:19) — `💰 Зарплата`
246. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:22) — `📥 Импорт`
247. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:23) — `🔄 Восстановить`
248. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:32) — `Запуск и меню`
249. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:33) — `Как пользоваться ботом`
250. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:34) — `Сколько данных в приложении`
251. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:35) — `Лоуды: всего и за неделю`
252. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:36) — `Массовый импорт лоудов`
253. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:37) — `Удалить дубликаты в базе`
254. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:38) — `Отменить импорт`
255. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:39) — `Восстановить лоуды из сообщений`
256. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:45) — `Запуск и меню`
257. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:46) — `Как пользоваться ботом`
258. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:47) — `Сколько данных в приложении`
259. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:48) — `Лоуды: всего и за неделю`
260. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:49) — `Массовый импорт лоудов`
261. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:50) — `Удалить дубликаты в базе`
262. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:51) — `Отменить импорт`
263. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:52) — `Восстановить лоуды из сообщений`
264. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:62) — `📋 Помощь`
265. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:62) — `📊 Статус`
266. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:62) — `📦 Лоуд`
267. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:62) — `💰 Зарплата`
268. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:62) — `📥 Импорт`
269. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:62) — `🔄 Восстановить`
270. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:67) — `📋 Помощь`
271. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:68) — `📊 Статус`
272. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:69) — `📦 Лоуд`
273. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:70) — `💰 Зарплата`
274. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:71) — `📥 Импорт`
275. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:72) — `🔄 Восстановить`
276. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:81) — `[^a-zа-я0-9\\s]`
277. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:86) — `востонави`
278. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:87) — `восстанови`
279. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:88) — `восстановить`
280. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:89) — `восстановление`
281. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:100) — `/восстановить`
282. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:101) — `/восстановить@`
283. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotFeatures.kt:102) — `/восстановить `
284. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotHealth.kt:27) — `Токен не задан`
285. ⬜ [D-ux] Extract hardcoded string to resources (TelegramBotHealth.kt:41) — `Токен недействителен (401). Получите новый у @BotFather → /t`
286. ⬜ [D-ux] Extract hardcoded string to resources (AnalyticsRepository.kt:110) — `Вс`
287. ⬜ [D-ux] Extract hardcoded string to resources (AnalyticsRepository.kt:110) — `Пн`
288. ⬜ [D-ux] Extract hardcoded string to resources (AnalyticsRepository.kt:110) — `Вт`
289. ⬜ [D-ux] Extract hardcoded string to resources (AnalyticsRepository.kt:110) — `Ср`
290. ⬜ [D-ux] Extract hardcoded string to resources (AnalyticsRepository.kt:110) — `Чт`
291. ⬜ [D-ux] Extract hardcoded string to resources (AnalyticsRepository.kt:110) — `Пт`
292. ⬜ [D-ux] Extract hardcoded string to resources (AnalyticsRepository.kt:110) — `Сб`
293. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:117) — `Водитель`
294. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:136) — `Дальнобойщик`
295. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:136) — `открытые дороги`
296. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:137) — `Русский,Английский`
297. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:157) — `Водитель`
298. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:167) — `Водитель`
299. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:200) — `Дальнобойщик`
300. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:200) — `открытые дороги`
301. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:203) — `Русский,Английский`
302. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:460) — `📷 Фото`
303. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:461) — `🎤 Голосовое`
304. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:587) — `Вы`
305. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:743) — `Вы`
306. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:840) — `Вы`
307. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:894) — `Иван Петров`
308. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:894) — `На I-95, RPM отличный!`
309. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:895) — `Алексей С.`
310. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:895) — `Ищу груз TX → FL`
311. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:896) — `Сергей К.`
312. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:896) — `Отдыхаю в Atlanta`
313. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:918) — `На связи!`
314. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:936) — `Вы`
315. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:950) — `Вы`
316. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:993) — `Русский,Английский`
317. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:1003) — `Дальнобойщик`
318. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:1003) — `открытые дороги`
319. ⬜ [D-ux] Extract hardcoded string to resources (SocialRepository.kt:1019) — `Водитель`
320. ⬜ [D-ux] Extract hardcoded string to resources (VoiceRepository.kt:71) — `Вы`
321. ⬜ [D-ux] Extract hardcoded string to resources (VoiceRepository.kt:87) — `Новая комната`
322. ⬜ [D-ux] Extract hardcoded string to resources (VoiceRepository.kt:108) — `Вы`
323. ⬜ [D-ux] Extract hardcoded string to resources (VoiceRepository.kt:201) — `Алексей`
324. ⬜ [D-ux] Extract hardcoded string to resources (VoiceRepository.kt:211) — `Вы`
325. ⬜ [D-ux] Extract hardcoded string to resources (ContentModerator.kt:15) — `Пустое сообщение`
326. ⬜ [D-ux] Extract hardcoded string to resources (ContentModerator.kt:16) — `Слишком длинное сообщение`
327. ⬜ [D-ux] Extract hardcoded string to resources (ContentModerator.kt:19) — `Сообщение заблокировано модерацией`
328. ⬜ [D-ux] Extract hardcoded string to resources (SocialPeerSeedData.kt:13) — `Иван Петров$demoSuffix`
329. ⬜ [D-ux] Extract hardcoded string to resources (SocialPeerSeedData.kt:14) — `Алексей С.$demoSuffix`
330. ⬜ [D-ux] Extract hardcoded string to resources (SocialPeerSeedData.kt:15) — `Сергей К.$demoSuffix`
331. ⬜ [D-ux] Extract hardcoded string to resources (SocialPeerSeedData.kt:16) — `Дмитрий Л.$demoSuffix`
332. ⬜ [D-ux] Extract hardcoded string to resources (SocialPeerSeedData.kt:17) — `Андрей М.$demoSuffix`
333. ⬜ [D-ux] Extract hardcoded string to resources (SocialPeerSeedData.kt:18) — `Мария В.$demoSuffix`
334. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:11) — `Водитель`
335. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:58) — `Маршрут I-95`
336. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:61) — `Коллеги, там авария на 45-м съезде`
337. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:66) — `Маршруты`
338. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:67) — `Активные маршруты по I-95`
339. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:74) — `Топливо и цены`
340. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:77) — `Diesel \$3.89 на TA в SC`
341. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:82) — `Топливо`
342. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:83) — `Цены на дизель обновляются каждый час`
343. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:90) — `Помощь на дороге`
344. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:93) — `Кто рядом с Atlanta? Нужен jump start`
345. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:98) — `Помощь`
346. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:99) — `Круглосуточная поддержка на дороге`
347. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:106) — `Алексей С.`
348. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:109) — `Привет! Ты где сейчас?`
349. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:124) — `Антон`
350. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:125) — `Привет всем! Кто сегодня на I-95?`
351. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:132) — `Сергей`
352. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:133) — `Я проехал Richmond — пробок нет.`
353. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:140) — `Иван`
354. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:141) — `Коллеги, там авария на 45-м съезде #I95 #важное`
355. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:149) — `Алексей`
356. ⬜ [D-ux] Extract hardcoded string to resources (SocialSeedData.kt:150) — `Привет! Ты где сейчас?`
357. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:16) — `Общий зал`
358. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:17) — `Комната маршрутов`
359. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:18) — `Топливо и цены`
360. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:19) — `Кабинет дальнобойщиков`
361. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:24) — `Антон`
362. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:25) — `Сергей`
363. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:26) — `Иван`
364. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:27) — `Дмитрий`
365. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:28) — `Алексей`
366. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:29) — `Максим`
367. ⬜ [D-ux] Extract hardcoded string to resources (VoiceSeedData.kt:30) — `Николай`
368. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:38) — `Локальный анализатор активен (без внешних AI API)`
369. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:52) — `RPM ниже $2.5 — маржа на милю слабая. `
370. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:54) — `RPM $rpm — хороший показатель на текущем периоде. `
371. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:56) — `RPM $rpm — средний уровень. `
372. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:59) — `Дизель съедает более 30% вала — проверьте маршруты и заправк`
373. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:62) — `Чистая прибыль отрицательная — сократите deadhead и пересмот`
374. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:65) — `Топ штаты: ${topStates.take(3).joinToString(`
375. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:68) — `Аномалии: $anomalies`
376. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:73) — `Сфокусируйтесь на рейсах с Total Rate / miles > $2.5`
377. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:74) — `Сравните цену дизеля по неделям и выберите дешёвые АЗС`
378. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:75) — `Отложите низкомаржинальные направления до восстановления RPM`
379. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:77) — `Продолжайте синхронизировать лоуды через Telegram`
380. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:78) — `Сверяйте зарплату и дизель по неделям`
381. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:79) — `Отслеживайте RPM в разделе Статистика`
382. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:85) — `Данные обновлены. Следите за RPM и долей дизеля в расходах.`
383. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:96) — `рентаб`
384. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:96) — `ставк`
385. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:97) — `RPM считается как доход на милю. Откройте Статистику — там с`
386. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:98) — `дизел`
387. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:98) — `топлив`
388. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:99) — `Отправьте чек за дизель боту в Telegram (текст с Total и gal`
389. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:100) — `зарплат`
390. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:101) — `Перешлите текст платёжки (Grand Total / Зарплата) боту — вып`
391. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:102) — `лоуд`
392. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:102) — `груз`
393. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:102) — `рейс`
394. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:103) — `Перешлите сообщение Amazon Relay с Trip ID / PU# / Total Rat`
395. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:104) — `прибыл`
396. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:105) — `Чистая прибыль = зарплата − дизель за период. Смотрите карто`
397. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:106) — `помощ`
398. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:106) — `что умеешь`
399. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:123) — `Последние лоуды:\n${last.joinToString(`
400. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:129) — `Зарплаты:\n${lines.takeLast(3).joinToString(`
401. ⬜ [D-ux] Extract hardcoded string to resources (DeterministicAdvisorService.kt:135) — `Дизель:\n${lines.takeLast(3).joinToString(`
402. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:19) — `США`
403. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:20) — `Канада`
404. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:21) — `Мексика`
405. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:22) — `Великобритания`
406. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:23) — `Германия`
407. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:24) — `Франция`
408. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:25) — `Испания`
409. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:26) — `Италия`
410. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:27) — `Польша`
411. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:28) — `Украина`
412. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:29) — `Россия`
413. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:30) — `Казахстан`
414. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:31) — `Узбекистан`
415. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:32) — `Таджикистан`
416. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:33) — `Кыргызстан`
417. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:34) — `Турция`
418. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:35) — `ОАЭ`
419. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:36) — `Индия`
420. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:37) — `Пакистан`
421. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:38) — `Китай`
422. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:39) — `Япония`
423. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:40) — `Южная Корея`
424. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:41) — `Австралия`
425. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:42) — `Новая Зеландия`
426. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:43) — `Бразилия`
427. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:44) — `Аргентина`
428. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:45) — `ЮАР`
429. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:46) — `Нигерия`
430. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:47) — `Египет`
431. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:48) — `Израиль`
432. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:49) — `Саудовская Аравия`
433. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:50) — `Швеция`
434. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:51) — `Норвегия`
435. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:52) — `Нидерланды`
436. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:53) — `Бельгия`
437. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:54) — `Швейцария`
438. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:55) — `Австрия`
439. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:56) — `Португалия`
440. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:57) — `Румыния`
441. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:58) — `Чехия`
442. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:59) — `Венгрия`
443. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:60) — `Литва`
444. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:61) — `Латвия`
445. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:62) — `Эстония`
446. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:63) — `Грузия`
447. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:64) — `Армения`
448. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:65) — `Азербайджан`
449. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:66) — `Беларусь`
450. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:67) — `Молдова`
451. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:68) — `Филиппины`
452. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:69) — `Вьетнам`
453. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:70) — `Таиланд`
454. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:71) — `Индонезия`
455. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:72) — `Малайзия`
456. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:73) — `Сингапур`
457. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:74) — `Колумбия`
458. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:75) — `Чили`
459. ⬜ [D-ux] Extract hardcoded string to resources (CountryCatalog.kt:76) — `Перу`
460. ⬜ [D-ux] Extract hardcoded string to resources (LoadMetrics.kt:47) — `1 день`
461. ⬜ [D-ux] Extract hardcoded string to resources (LoadMetrics.kt:48) — `$rounded дня`
462. ⬜ [D-ux] Extract hardcoded string to resources (LoadMetrics.kt:49) — `$rounded дней`
463. ⬜ [D-ux] Extract hardcoded string to resources (LoadMetrics.kt:57) — `$%,.2f/день`
464. ⬜ [D-ux] Extract hardcoded string to resources (LoadMetrics.kt:59) — `$%,.0f/день`
465. ⬜ [D-ux] Extract hardcoded string to resources (DieselTextParser.kt:8) — `(?:Total\s*Amount|Amount\s*Due|Итого|Total)\s*[:\s]*\$?\s*([`
466. ⬜ [D-ux] Extract hardcoded string to resources (DieselTextParser.kt:11) — `([\d.]+)\s*(?:gal|gallons|гл)\b`
467. ⬜ [D-ux] Extract hardcoded string to resources (DieselTextParser.kt:13) — `(?:Date|Дата)\s*[:\s]*([^\n]+)`
468. ⬜ [D-ux] Extract hardcoded string to resources (DieselTextParser.kt:14) — `(?:Location|Store|Station|АЗС)\s*[:\s]*([^\n]+)`
469. ⬜ [D-ux] Extract hardcoded string to resources (DieselTextParser.kt:18) — `diesel|fuel|gallons?|gal\b|топлив|дизел`
470. ⬜ [D-ux] Extract hardcoded string to resources (LoadProcessor.kt:40) — `Дубликат (${duplicate.reason}): ${duplicate.load.tripId}`
471. ⬜ [D-ux] Extract hardcoded string to resources (LoadProcessor.kt:58) — `Изменений нет`
472. ⬜ [D-ux] Extract hardcoded string to resources (LoadProcessor.kt:62) — `Авто-обновление отключено`
473. ⬜ [D-ux] Extract hardcoded string to resources (LoadUpdater.kt:19) — `Изменение: $change`
474. ⬜ [D-ux] Extract hardcoded string to resources (MessageClassifier.kt:10) — `Grand\s*Total|Settlement\s*Date|Cutoff\s*Date|Driver\s*Settl`
475. ⬜ [D-ux] Extract hardcoded string to resources (MessageClassifier.kt:14) — `(?:diesel|fuel\s*receipt|gallons?|price\s*per\s*gallon|gal\s`
476. ⬜ [D-ux] Extract hardcoded string to resources (PaycheckTextParser.kt:8) — `(?:Grand\s*Total|Зарплата|Net\s*Pay)\s*[:\s]*\$?\s*([\d,]+\.`
477. ⬜ [D-ux] Extract hardcoded string to resources (BadgeEngine.kt:19) — `100 грузов`
478. ⬜ [D-ux] Extract hardcoded string to resources (BadgeEngine.kt:25) — `Покоритель дорог`
479. ⬜ [D-ux] Extract hardcoded string to resources (BadgeEngine.kt:25) — `50 000+ миль`
480. ⬜ [D-ux] Extract hardcoded string to resources (BadgeEngine.kt:34) — `Золотой RPM`
481. ⬜ [D-ux] Extract hardcoded string to resources (BadgeEngine.kt:34) — `$100k+ гросса`
482. ⬜ [D-ux] Extract hardcoded string to resources (BadgeEngine.kt:37) — `500+ грузов`
483. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:43) — `Другое`
484. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:55) — `Мастер грузов`
485. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:55) — `1000+ грузов`
486. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:56) — `Король миль`
487. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:56) — `100 000+ миль`
488. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:57) — `Чемпион RPM`
489. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:57) — `$2.50+/миля`
490. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:58) — `500+ грузов на платформе`
491. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:59) — `500+ грузов с охлаждением`
492. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:60) — `Сертификат Hazmat`
493. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:61) — `Помощник`
494. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:61) — `Помог 50+ водителям`
495. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:62) — `Наставник`
496. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:62) — `Обучил 10+ водителей`
497. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:63) — `Лидер сообщества`
498. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:63) — `Создал 5+ групп`
499. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:64) — `Пунктуальный`
500. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:64) — `95%+ вовремя`
501. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:65) — `Надёжный`
502. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:65) — `Никогда не отменял груз`
503. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:66) — `Легенда`
504. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:66) — `10 лет в профессии`
505. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:67) — `Первый груз`
506. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:67) — `Первый загруженный груз`
507. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:111) — `📊 Общий`
508. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:112) — `📦 Грузы`
509. ⬜ [D-ux] Extract hardcoded string to resources (EnhancedSocialModels.kt:113) — `💰 Доход`
510. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:8) — `100 грузов`
511. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:8) — `Перевезено 100+ грузов`
512. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:11) — `Покоритель дорог`
513. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:11) — `50 000+ миль`
514. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:14) — `Золотой RPM`
515. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:14) — `$100k+ гросса`
516. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:17) — `Легенда`
517. ⬜ [D-ux] Extract hardcoded string to resources (SocialBadges.kt:17) — `500+ грузов`
518. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:22) — `🟢 В сети`
519. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:23) — `🛣️ В рейсе`
520. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:24) — `😴 Отдыхает`
521. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:25) — `⚫ Не в сети`
522. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:89) — `📏 Больше всех миль`
523. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:90) — `💰 Больше всех дохода`
524. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:91) — `📈 Лучший RPM`
525. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:92) — `📦 Больше всех грузов`
526. ⬜ [D-ux] Extract hardcoded string to resources (SocialModels.kt:93) — `⏱️ Точность`
527. ⬜ [D-ux] Extract hardcoded string to resources (ImportLoadsUseCase.kt:205) — `Авто-обновление`
528. ⬜ [D-ux] Extract hardcoded string to resources (ImportLoadsUseCase.kt:206) — `Изменений нет`
529. ⬜ [D-ux] Extract hardcoded string to resources (ImportLoadsUseCase.kt:207) — `Дубликат`
530. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:29) — `Вашингтон`
531. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:30) — `Калифорния`
532. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:31) — `Орегон`
533. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:32) — `Айдахо`
534. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:33) — `Монтана`
535. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:34) — `Невада`
536. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:35) — `Аризона`
537. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:36) — `Юта`
538. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:37) — `Вайоминг`
539. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:38) — `Нью-Мексико`
540. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:39) — `Колорадо`
541. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:40) — `Оклахома`
542. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:41) — `Канзас`
543. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:42) — `Небраска`
544. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:43) — `Южная Дакота`
545. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:44) — `Северная Дакота`
546. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:45) — `Миннесота`
547. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:46) — `Висконсин`
548. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:47) — `Айова`
549. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:48) — `Миссури`
550. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:49) — `Техас`
551. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:50) — `Арканзас`
552. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:51) — `Луизиана`
553. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:52) — `Миссисипи`
554. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:53) — `Теннесси`
555. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:54) — `Кентукки`
556. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:55) — `Иллинойс`
557. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:56) — `Индиана`
558. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:57) — `Огайо`
559. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:58) — `Мичиган`
560. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:59) — `Пенсильвания`
561. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:60) — `Нью-Йорк`
562. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:61) — `Нью-Джерси`
563. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:62) — `Западная Виргиния`
564. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:63) — `Мэриленд`
565. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:64) — `Делавэр`
566. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:65) — `Вашингтон (окр.)`
567. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:66) — `Коннектикут`
568. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:67) — `Род-Айленд`
569. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:68) — `Массачусетс`
570. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:69) — `Вермонт`
571. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:70) — `Нью-Гэмпшир`
572. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:71) — `Мэн`
573. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:72) — `Алабама`
574. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:73) — `Джорджия`
575. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:74) — `Флорида`
576. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:75) — `Южная Каролина`
577. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:76) — `Северная Каролина`
578. ⬜ [D-ux] Extract hardcoded string to resources (UsStatePaths.kt:77) — `Виргиния`
579. ⬜ [D-ux] Extract hardcoded string to resources (NavGraph.kt:351) — `Вы`
580. ⬜ [D-ux] Extract hardcoded string to resources (ProfileSetupScreen.kt:88) — `Водитель`
581. ⬜ [D-ux] Extract hardcoded string to resources (WeeklyGoalScreen.kt:370) — `/день`
582. ⬜ [D-ux] Extract hardcoded string to resources (HomeScreen.kt:119) — `Водитель`
583. ⬜ [D-ux] Extract hardcoded string to resources (SocialViewModels.kt:334) — `Я`
584. ⬜ [D-ux] Extract hardcoded string to resources (SocialViewModels.kt:350) — `Я`
585. ⬜ [D-ux] Extract hardcoded string to resources (SocialViewModels.kt:357) — `Я`
586. ⬜ [D-ux] Extract hardcoded string to resources (StatsScreen.kt:131) — `Водитель`
587. ⬜ [D-ux] Extract hardcoded string to resources (VoiceScreens.kt:346) — `${participant.displayName} (Вы)`
588. ⬜ [E-test] Add unit/smoke coverage for ./TruckerLoadApp.kt
589. ⬜ [E-test] Add unit/smoke coverage for data/local/AppDatabase.kt
590. ✅ [E-test] Add unit/smoke coverage for data/local/DatabaseMigrations.kt
591. ✅ [E-test] Add unit/smoke coverage for data/local/DatabaseMigrations_extracted.kt
592. ⬜ [E-test] Add unit/smoke coverage for data/local/DieselMapper.kt
593. ⬜ [E-test] Add unit/smoke coverage for data/local/LoadMapper.kt
594. ⬜ [E-test] Add unit/smoke coverage for data/local/dao/DieselDao.kt
595. ⬜ [E-test] Add unit/smoke coverage for data/local/dao/LoadDao.kt
596. ⬜ [E-test] Add unit/smoke coverage for data/local/dao/LoadHistoryDao.kt
597. ⬜ [E-test] Add unit/smoke coverage for data/local/dao/PaycheckDao.kt
598. ⬜ [E-test] Add unit/smoke coverage for data/local/dao/PenaltyDao.kt
599. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/DieselEntity.kt
600. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/LoadEntity.kt
601. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/LoadHistory.kt
602. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/LoadStatsAgg.kt
603. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/PaycheckEntity.kt
604. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/analytics/AnalyticsTotalsAgg.kt
605. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/analytics/DailyGrossAgg.kt
606. ⬜ [E-test] Add unit/smoke coverage for data/local/entities/analytics/WeeklyRevenueAgg.kt
607. ⬜ [E-test] Add unit/smoke coverage for data/remote/CredentialManagerGoogleSignIn.kt
608. ⬜ [E-test] Add unit/smoke coverage for data/remote/SupabaseAuthService.kt
609. ⬜ [E-test] Add unit/smoke coverage for data/remote/TelegramApi.kt
610. ✅ [E-test] Add unit/smoke coverage for data/remote/TelegramBotFeatures.kt
611. ✅ [E-test] Add unit/smoke coverage for data/remote/TelegramBotHealth.kt
612. ⬜ [E-test] Add unit/smoke coverage for data/voice/AudioQualityManager.kt
613. ⬜ [E-test] Add unit/smoke coverage for data/voice/SignalingService.kt
614. ⬜ [E-test] Add unit/smoke coverage for data/voice/VoiceSeedData.kt
615. ⬜ [E-test] Add unit/smoke coverage for data/voice/WebRtcAudioEngine.kt
616. ⬜ [E-test] Add unit/smoke coverage for data/voice/WebRtcCallManager.kt
617. ⬜ [E-test] Add unit/smoke coverage for di/ActiveDatabaseProvider.kt
618. ⬜ [E-test] Add unit/smoke coverage for domain/geo/CountryCatalog.kt
619. ⬜ [E-test] Add unit/smoke coverage for domain/import/LoadValidator.kt
620. ⬜ [E-test] Add unit/smoke coverage for domain/import/model/ImportModels.kt
621. ⬜ [E-test] Add unit/smoke coverage for domain/import/repository/LoadImportRepository.kt
622. ⬜ [E-test] Add unit/smoke coverage for domain/import/usecase/ImportLoadsUseCase.kt
623. ⬜ [E-test] Add unit/smoke coverage for domain/model/analytics/AnalyticsPeriod.kt
624. ⬜ [E-test] Add unit/smoke coverage for domain/model/analytics/AnalyticsSummary.kt
625. ⬜ [E-test] Add unit/smoke coverage for domain/model/analytics/DailyData.kt
626. ⬜ [E-test] Add unit/smoke coverage for domain/model/analytics/RouteData.kt
627. ⬜ [E-test] Add unit/smoke coverage for domain/model/analytics/WeekData.kt
628. ⬜ [E-test] Add unit/smoke coverage for domain/usecase/ForecastService.kt
629. ⬜ [E-test] Add unit/smoke coverage for domain/usecase/FuelAnalyticsService.kt
630. ⬜ [E-test] Add unit/smoke coverage for domain/voice/VoiceModels.kt
631. ⬜ [E-test] Add unit/smoke coverage for presentation/auth/GoogleSignInLauncher.kt
632. ⬜ [E-test] Add unit/smoke coverage for presentation/components/charts/AnalyticsBarCharts.kt
633. ⬜ [E-test] Add unit/smoke coverage for presentation/components/charts/WeeklyRevenueLineChart.kt
634. ⬜ [E-test] Add unit/smoke coverage for presentation/connectivity/ConnectivityObserver.kt
635. ⬜ [E-test] Add unit/smoke coverage for presentation/di/AppProvider.kt
636. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/add/AddDieselScreen.kt
637. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/add/AddLoadScreen.kt
638. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/add/AddPaycheckScreen.kt
639. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/add/AddLoadViewModel.kt
640. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/add/AddDieselViewModel.kt
641. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/advisor/FinancialAdvisorScreen.kt
642. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/analytics/AnalyticsScreen.kt
643. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/analytics/AnalyticsViewModel.kt
644. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/auth/LoginEmailScreen.kt
645. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/auth/ProfileSetupScreen.kt
646. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/auth/SignUpScreen.kt
647. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/chat/ChatViewModel.kt
648. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/detail/LoadDetailScreen.kt
649. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/detail/LoadDetailViewModel.kt
650. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/edit/EditLoadScreen.kt
651. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/edit/EditLoadViewModel.kt
652. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/goal/GoalViewModel.kt
653. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/goal/WeeklyGoalScreen.kt
654. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/login/LoginScreen.kt
655. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/scanner/ScanGalleryScreen.kt
656. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/scanner/ScanResultScreen.kt
657. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/scanner/ScannerScreen.kt
658. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/scanner/ScannerViewModel.kt
659. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/settings/LanguageSettingsSection.kt
660. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/settings/ParserSettings.kt
661. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/settings/SettingsScreen.kt
662. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/settings/SettingsViewModel.kt
663. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/settings/ThemeSettingsSection.kt
664. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/stats/StatsPeriod.kt
665. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/stats/StatsScreen.kt
666. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/stats/StatsViewModel.kt
667. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/voice/CallScreens.kt
668. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/voice/VoiceScreens.kt
669. ⬜ [E-test] Add unit/smoke coverage for presentation/screens/voice/VoiceViewModels.kt
670. ⬜ [E-test] Add unit/smoke coverage for presentation/theme/Animations.kt
671. ⬜ [E-test] Add unit/smoke coverage for presentation/theme/AppColors.kt
672. ⬜ [E-test] Add unit/smoke coverage for presentation/theme/AppElevation.kt
673. ⬜ [E-test] Add unit/smoke coverage for presentation/theme/AppShapes.kt
674. ⬜ [E-test] Add unit/smoke coverage for presentation/theme/AppTextFieldDefaults.kt
675. ⬜ [E-test] Add unit/smoke coverage for sync/import/ImportCommandHandler.kt
676. ⬜ [E-test] Add unit/smoke coverage for sync/import/ImportDocumentHandler.kt
677. ⬜ [E-test] Add unit/smoke coverage for sync/import/ImportHandlerSupport.kt
678. ⬜ [E-test] Add unit/smoke coverage for sync/import/ImportMessageHandler.kt
679. ⬜ [E-test] Add unit/smoke coverage for sync/import/ImportReportFormatter.kt
680. ⬜ [E-test] Add unit/smoke coverage for utils/ocr/HybridOCRService.kt
681. ⬜ [E-test] Add unit/smoke coverage for utils/ocr/LanguageDetector.kt
682. ⬜ [E-sec] Hash or encrypt local passwords (no reversible storage)
683. ✅ [E-sec] Stop embedding secrets in BuildConfig where possible
684. ⬜ [E-sec] Redact remaining Log paths outside Telegram
685. ✅ [E-sec] Review exported components in Manifest
686. ✅ [E-sec] Network security config verify cleartext blocked
687. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SocialRepository.kt
688. ✅ [F-verify] Compile-safe nullability pass for SocialRepository.kt
689. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SocialRepository.kt
690. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SettingsScreen.kt
691. ✅ [F-verify] Compile-safe nullability pass for SettingsScreen.kt
692. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SettingsScreen.kt
693. ✅ [F-verify] Regression-verify QUALITY_150 invariants in StatsScreen.kt
694. ✅ [F-verify] Compile-safe nullability pass for StatsScreen.kt
695. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in StatsScreen.kt
696. ✅ [F-verify] Regression-verify QUALITY_150 invariants in TelegramBotSyncEngine.kt
697. ✅ [F-verify] Compile-safe nullability pass for TelegramBotSyncEngine.kt
698. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in TelegramBotSyncEngine.kt
699. ✅ [F-verify] Regression-verify QUALITY_150 invariants in HomeScreen.kt
700. ✅ [F-verify] Compile-safe nullability pass for HomeScreen.kt
701. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in HomeScreen.kt
702. ✅ [F-verify] Regression-verify QUALITY_150 invariants in NavGraph.kt
703. ✅ [F-verify] Compile-safe nullability pass for NavGraph.kt
704. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in NavGraph.kt
705. ✅ [F-verify] Regression-verify QUALITY_150 invariants in LoginScreen.kt
706. ✅ [F-verify] Compile-safe nullability pass for LoginScreen.kt
707. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in LoginScreen.kt
708. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SocialViewModels.kt
709. ✅ [F-verify] Compile-safe nullability pass for SocialViewModels.kt
710. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SocialViewModels.kt
711. ✅ [F-verify] Regression-verify QUALITY_150 invariants in WeekUtils.kt
712. ✅ [F-verify] Compile-safe nullability pass for WeekUtils.kt
713. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in WeekUtils.kt
714. ✅ [F-verify] Regression-verify QUALITY_150 invariants in StatsViewModel.kt
715. ✅ [F-verify] Compile-safe nullability pass for StatsViewModel.kt
716. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in StatsViewModel.kt
717. ✅ [F-verify] Regression-verify QUALITY_150 invariants in LoadDetailScreen.kt
718. ✅ [F-verify] Compile-safe nullability pass for LoadDetailScreen.kt
719. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in LoadDetailScreen.kt
720. ✅ [F-verify] Regression-verify QUALITY_150 invariants in HomeViewModel.kt
721. ✅ [F-verify] Compile-safe nullability pass for HomeViewModel.kt
722. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in HomeViewModel.kt
723. ✅ [F-verify] Regression-verify QUALITY_150 invariants in CommunityScreen.kt
724. ✅ [F-verify] Compile-safe nullability pass for CommunityScreen.kt
725. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in CommunityScreen.kt
726. ✅ [F-verify] Regression-verify QUALITY_150 invariants in DatabaseMigrations.kt
727. ✅ [F-verify] Compile-safe nullability pass for DatabaseMigrations.kt
728. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in DatabaseMigrations.kt
729. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ProfileScreen.kt
730. ✅ [F-verify] Compile-safe nullability pass for ProfileScreen.kt
731. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ProfileScreen.kt
732. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SupabaseAuthService.kt
733. ✅ [F-verify] Compile-safe nullability pass for SupabaseAuthService.kt
734. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SupabaseAuthService.kt
735. ✅ [F-verify] Regression-verify QUALITY_150 invariants in FinancialAdvisorScreen.kt
736. ✅ [F-verify] Compile-safe nullability pass for FinancialAdvisorScreen.kt
737. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in FinancialAdvisorScreen.kt
738. ✅ [F-verify] Regression-verify QUALITY_150 invariants in LoadRepository.kt
739. ✅ [F-verify] Compile-safe nullability pass for LoadRepository.kt
740. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in LoadRepository.kt
741. ✅ [F-verify] Regression-verify QUALITY_150 invariants in WeeklyGoalScreen.kt
742. ✅ [F-verify] Compile-safe nullability pass for WeeklyGoalScreen.kt
743. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in WeeklyGoalScreen.kt
744. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SocialChatScreen.kt
745. ✅ [F-verify] Compile-safe nullability pass for SocialChatScreen.kt
746. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SocialChatScreen.kt
747. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SignUpScreen.kt
748. ✅ [F-verify] Compile-safe nullability pass for SignUpScreen.kt
749. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SignUpScreen.kt
750. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AnalyticsScreen.kt
751. ✅ [F-verify] Compile-safe nullability pass for AnalyticsScreen.kt
752. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AnalyticsScreen.kt
753. ✅ [F-verify] Regression-verify QUALITY_150 invariants in VoiceScreens.kt
754. ✅ [F-verify] Compile-safe nullability pass for VoiceScreens.kt
755. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in VoiceScreens.kt
756. ✅ [F-verify] Regression-verify QUALITY_150 invariants in WidgetRemoteViewsFactory.kt
757. ✅ [F-verify] Compile-safe nullability pass for WidgetRemoteViewsFactory.kt
758. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in WidgetRemoteViewsFactory.kt
759. ✅ [F-verify] Regression-verify QUALITY_150 invariants in GoogleMapsHeatmapCard.kt
760. ✅ [F-verify] Compile-safe nullability pass for GoogleMapsHeatmapCard.kt
761. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in GoogleMapsHeatmapCard.kt
762. ✅ [F-verify] Regression-verify QUALITY_150 invariants in BackupService.kt
763. ✅ [F-verify] Compile-safe nullability pass for BackupService.kt
764. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in BackupService.kt
765. ✅ [F-verify] Regression-verify QUALITY_150 invariants in MainActivity.kt
766. ✅ [F-verify] Compile-safe nullability pass for MainActivity.kt
767. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in MainActivity.kt
768. ✅ [F-verify] Regression-verify QUALITY_150 invariants in CameraScreen.kt
769. ✅ [F-verify] Compile-safe nullability pass for CameraScreen.kt
770. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in CameraScreen.kt
771. ✅ [F-verify] Regression-verify QUALITY_150 invariants in GoogleSignInLauncher.kt
772. ✅ [F-verify] Compile-safe nullability pass for GoogleSignInLauncher.kt
773. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in GoogleSignInLauncher.kt
774. ✅ [F-verify] Regression-verify QUALITY_150 invariants in CameraViewModel.kt
775. ✅ [F-verify] Compile-safe nullability pass for CameraViewModel.kt
776. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in CameraViewModel.kt
777. ✅ [F-verify] Regression-verify QUALITY_150 invariants in TelegramApi.kt
778. ✅ [F-verify] Compile-safe nullability pass for TelegramApi.kt
779. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in TelegramApi.kt
780. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AppDatabase.kt
781. ✅ [F-verify] Compile-safe nullability pass for AppDatabase.kt
782. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AppDatabase.kt
783. ✅ [F-verify] Regression-verify QUALITY_150 invariants in LoadMessageParser.kt
784. ✅ [F-verify] Compile-safe nullability pass for LoadMessageParser.kt
785. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in LoadMessageParser.kt
786. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AdaptiveScaffold.kt
787. ✅ [F-verify] Compile-safe nullability pass for AdaptiveScaffold.kt
788. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AdaptiveScaffold.kt
789. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AddDieselScreen.kt
790. ✅ [F-verify] Compile-safe nullability pass for AddDieselScreen.kt
791. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AddDieselScreen.kt
792. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AddPaycheckScreen.kt
793. ✅ [F-verify] Compile-safe nullability pass for AddPaycheckScreen.kt
794. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AddPaycheckScreen.kt
795. ✅ [F-verify] Regression-verify QUALITY_150 invariants in VoiceRepository.kt
796. ✅ [F-verify] Compile-safe nullability pass for VoiceRepository.kt
797. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in VoiceRepository.kt
798. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ProfileAvatarComponents.kt
799. ✅ [F-verify] Compile-safe nullability pass for ProfileAvatarComponents.kt
800. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ProfileAvatarComponents.kt
801. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ProfileEditScreen.kt
802. ✅ [F-verify] Compile-safe nullability pass for ProfileEditScreen.kt
803. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ProfileEditScreen.kt
804. ✅ [F-verify] Regression-verify QUALITY_150 invariants in StatusScreen.kt
805. ✅ [F-verify] Compile-safe nullability pass for StatusScreen.kt
806. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in StatusScreen.kt
807. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AvatarCropScreen.kt
808. ✅ [F-verify] Compile-safe nullability pass for AvatarCropScreen.kt
809. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AvatarCropScreen.kt
810. ✅ [F-verify] Regression-verify QUALITY_150 invariants in EditLoadScreen.kt
811. ✅ [F-verify] Compile-safe nullability pass for EditLoadScreen.kt
812. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in EditLoadScreen.kt
813. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ScannerViewModel.kt
814. ✅ [F-verify] Compile-safe nullability pass for ScannerViewModel.kt
815. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ScannerViewModel.kt
816. ✅ [F-verify] Regression-verify QUALITY_150 invariants in LoadCard.kt
817. ✅ [F-verify] Compile-safe nullability pass for LoadCard.kt
818. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in LoadCard.kt
819. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ScannerScreen.kt
820. ✅ [F-verify] Compile-safe nullability pass for ScannerScreen.kt
821. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ScannerScreen.kt
822. ✅ [F-verify] Regression-verify QUALITY_150 invariants in PhotoPreviewScreen.kt
823. ✅ [F-verify] Compile-safe nullability pass for PhotoPreviewScreen.kt
824. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in PhotoPreviewScreen.kt
825. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ReportGeneratorService.kt
826. ✅ [F-verify] Compile-safe nullability pass for ReportGeneratorService.kt
827. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ReportGeneratorService.kt
828. ✅ [F-verify] Regression-verify QUALITY_150 invariants in PhotoDetailScreen.kt
829. ✅ [F-verify] Compile-safe nullability pass for PhotoDetailScreen.kt
830. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in PhotoDetailScreen.kt
831. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ScanResultScreen.kt
832. ✅ [F-verify] Compile-safe nullability pass for ScanResultScreen.kt
833. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ScanResultScreen.kt
834. ✅ [F-verify] Regression-verify QUALITY_150 invariants in WebRtcCallManager.kt
835. ✅ [F-verify] Compile-safe nullability pass for WebRtcCallManager.kt
836. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in WebRtcCallManager.kt
837. ✅ [F-verify] Regression-verify QUALITY_150 invariants in GoogleDriveApiClient.kt
838. ✅ [F-verify] Compile-safe nullability pass for GoogleDriveApiClient.kt
839. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in GoogleDriveApiClient.kt
840. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ChatViewModel.kt
841. ✅ [F-verify] Compile-safe nullability pass for ChatViewModel.kt
842. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ChatViewModel.kt
843. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ProfileSetupScreen.kt
844. ✅ [F-verify] Compile-safe nullability pass for ProfileSetupScreen.kt
845. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ProfileSetupScreen.kt
846. ✅ [F-verify] Regression-verify QUALITY_150 invariants in PhotoBatchReviewScreen.kt
847. ✅ [F-verify] Compile-safe nullability pass for PhotoBatchReviewScreen.kt
848. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in PhotoBatchReviewScreen.kt
849. ✅ [F-verify] Regression-verify QUALITY_150 invariants in CallScreens.kt
850. ✅ [F-verify] Compile-safe nullability pass for CallScreens.kt
851. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in CallScreens.kt
852. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SocialDaos.kt
853. ✅ [F-verify] Compile-safe nullability pass for SocialDaos.kt
854. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SocialDaos.kt
855. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ImportLoadsUseCase.kt
856. ✅ [F-verify] Compile-safe nullability pass for ImportLoadsUseCase.kt
857. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ImportLoadsUseCase.kt
858. ✅ [F-verify] Regression-verify QUALITY_150 invariants in LoadDao.kt
859. ✅ [F-verify] Compile-safe nullability pass for LoadDao.kt
860. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in LoadDao.kt
861. ✅ [F-verify] Regression-verify QUALITY_150 invariants in CountryPhoneFields.kt
862. ✅ [F-verify] Compile-safe nullability pass for CountryPhoneFields.kt
863. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in CountryPhoneFields.kt
864. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ImportDocumentHandler.kt
865. ✅ [F-verify] Compile-safe nullability pass for ImportDocumentHandler.kt
866. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ImportDocumentHandler.kt
867. ✅ [F-verify] Regression-verify QUALITY_150 invariants in VoiceViewModels.kt
868. ✅ [F-verify] Compile-safe nullability pass for VoiceViewModels.kt
869. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in VoiceViewModels.kt
870. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ScanGalleryScreen.kt
871. ✅ [F-verify] Compile-safe nullability pass for ScanGalleryScreen.kt
872. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ScanGalleryScreen.kt
873. ✅ [F-verify] Regression-verify QUALITY_150 invariants in SettingsViewModel.kt
874. ✅ [F-verify] Compile-safe nullability pass for SettingsViewModel.kt
875. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in SettingsViewModel.kt
876. ✅ [F-verify] Regression-verify QUALITY_150 invariants in TruckLogNavigationRail.kt
877. ✅ [F-verify] Compile-safe nullability pass for TruckLogNavigationRail.kt
878. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in TruckLogNavigationRail.kt
879. ✅ [F-verify] Regression-verify QUALITY_150 invariants in DisputeSection.kt
880. ✅ [F-verify] Compile-safe nullability pass for DisputeSection.kt
881. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in DisputeSection.kt
882. ✅ [F-verify] Regression-verify QUALITY_150 invariants in GoogleDriveBackupService.kt
883. ✅ [F-verify] Compile-safe nullability pass for GoogleDriveBackupService.kt
884. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in GoogleDriveBackupService.kt
885. ✅ [F-verify] Regression-verify QUALITY_150 invariants in WidgetConfigureActivity.kt
886. ✅ [F-verify] Compile-safe nullability pass for WidgetConfigureActivity.kt
887. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in WidgetConfigureActivity.kt
888. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AddPaycheckViewModel.kt
889. ✅ [F-verify] Compile-safe nullability pass for AddPaycheckViewModel.kt
890. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AddPaycheckViewModel.kt
891. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AddDieselViewModel.kt
892. ✅ [F-verify] Compile-safe nullability pass for AddDieselViewModel.kt
893. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AddDieselViewModel.kt
894. ✅ [F-verify] Regression-verify QUALITY_150 invariants in PeerProfileScreen.kt
895. ✅ [F-verify] Compile-safe nullability pass for PeerProfileScreen.kt
896. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in PeerProfileScreen.kt
897. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ParseUtils.kt
898. ✅ [F-verify] Compile-safe nullability pass for ParseUtils.kt
899. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ParseUtils.kt
900. ✅ [F-verify] Regression-verify QUALITY_150 invariants in BentoGlass.kt
901. ✅ [F-verify] Compile-safe nullability pass for BentoGlass.kt
902. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in BentoGlass.kt
903. ✅ [F-verify] Regression-verify QUALITY_150 invariants in TaxTrackerScreen.kt
904. ✅ [F-verify] Compile-safe nullability pass for TaxTrackerScreen.kt
905. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in TaxTrackerScreen.kt
906. ✅ [F-verify] Regression-verify QUALITY_150 invariants in LoadCalendarWithDots.kt
907. ✅ [F-verify] Compile-safe nullability pass for LoadCalendarWithDots.kt
908. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in LoadCalendarWithDots.kt
909. ✅ [F-verify] Regression-verify QUALITY_150 invariants in PhotoGalleryScreen.kt
910. ✅ [F-verify] Compile-safe nullability pass for PhotoGalleryScreen.kt
911. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in PhotoGalleryScreen.kt
912. ✅ [F-verify] Regression-verify QUALITY_150 invariants in TelegramBotForegroundService.kt
913. ✅ [F-verify] Compile-safe nullability pass for TelegramBotForegroundService.kt
914. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in TelegramBotForegroundService.kt
915. ✅ [F-verify] Regression-verify QUALITY_150 invariants in AuthStore.kt
916. ✅ [F-verify] Compile-safe nullability pass for AuthStore.kt
917. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in AuthStore.kt
918. ✅ [F-verify] Regression-verify QUALITY_150 invariants in GroupsScreen.kt
919. ✅ [F-verify] Compile-safe nullability pass for GroupsScreen.kt
920. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in GroupsScreen.kt
921. ✅ [F-verify] Regression-verify QUALITY_150 invariants in WeekCalendarPicker.kt
922. ✅ [F-verify] Compile-safe nullability pass for WeekCalendarPicker.kt
923. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in WeekCalendarPicker.kt
924. ✅ [F-verify] Regression-verify QUALITY_150 invariants in ExportBottomSheet.kt
925. ✅ [F-verify] Compile-safe nullability pass for ExportBottomSheet.kt
926. ⬜ [F-a11y] a11y contentDescription audit for interactive UI in ExportBottomSheet.kt
927. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/TruckerLoadApp.kt
928. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/backup/BackupData.kt
929. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/backup/BackupDataCodec.kt
930. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/backup/DriveSyncPolicy.kt
931. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/backup/GoogleDriveApiClient.kt
932. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/backup/GoogleDriveBackupPrefs.kt
933. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/backup/GoogleDriveBackupService.kt
934. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/AppDatabase.kt
935. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/DatabaseMigrations.kt
936. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/DatabaseMigrations_extracted.kt
937. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/DieselMapper.kt
938. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/LoadMapper.kt
939. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/PaycheckMapper.kt
940. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/DieselDao.kt
941. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/LoadDao.kt
942. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/LoadHistoryDao.kt
943. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/PaycheckDao.kt
944. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/PenaltyDao.kt
945. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/PhotoDao.kt
946. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/ScanDao.kt
947. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/SocialDaos.kt
948. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/StopDao.kt
949. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/TelegramInboxDao.kt
950. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/dao/VoiceDaos.kt
951. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/DieselEntity.kt
952. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/LoadEntity.kt
953. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/LoadHistory.kt
954. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/LoadStatsAgg.kt
955. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/PaycheckEntity.kt
956. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/PenaltyEntity.kt
957. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/PhotoEntity.kt
958. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/ScanEntity.kt
959. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/SocialEntities.kt
960. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/StopEntity.kt
961. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/TelegramInboxEntity.kt
962. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/VoiceEntities.kt
963. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/WeekYieldAgg.kt
964. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/analytics/AnalyticsTotalsAgg.kt
965. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/analytics/DailyGrossAgg.kt
966. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/local/entities/analytics/WeeklyRevenueAgg.kt
967. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/paging/FilteredLoadsPagingSource.kt
968. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/AccountIds.kt
969. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/AppLanguage.kt
970. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/AppThemeMode.kt
971. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/AuthCredentialsStore.kt
972. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/AuthLogin.kt
973. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/AuthStore.kt
974. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/RpmThresholdsStore.kt
975. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/SecurePreferences.kt
976. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/SelectedStateStore.kt
977. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/SettingsDataStore.kt
978. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/StatsSelectionStore.kt
979. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/TelegramTokenStore.kt
980. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/UserProfileStore.kt
981. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/preferences/WeeklyProfitGoalStore.kt
982. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/remote/CredentialManagerGoogleSignIn.kt
983. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/remote/SupabaseAuthService.kt
984. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/remote/TelegramApi.kt
985. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/remote/TelegramBotFeatures.kt
986. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/remote/TelegramBotHealth.kt
987. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/AiRepository.kt
988. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/AnalyticsRepository.kt
989. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/DieselRepository.kt
990. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/LoadImportRepositoryImpl.kt
991. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/LoadRepository.kt
992. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/PaycheckRepository.kt
993. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/PhotoRepository.kt
994. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/ScanRepository.kt
995. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/SocialRepository.kt
996. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/VoiceRepository.kt
997. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/repository/WeekRepository.kt
998. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/AvatarStorage.kt
999. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/ChatAttachmentStorage.kt
1000. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/ContentModerator.kt
1001. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/RecommendationService.kt
1002. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/SocialMediaOptimizer.kt
1003. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/SocialPeerSeedData.kt
1004. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/SocialSeedData.kt
1005. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/social/VoiceNoteRecorder.kt
1006. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/voice/AudioQualityManager.kt
1007. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/voice/SignalingService.kt
1008. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/voice/VoiceSeedData.kt
1009. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/voice/WebRtcAudioEngine.kt
1010. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/data/voice/WebRtcCallManager.kt
1011. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/di/ActiveDatabaseProvider.kt
1012. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/advisor/DeterministicAdvisorService.kt
1013. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/analytics/RouteDisplayHelper.kt
1014. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/filter/LoadFilter.kt
1015. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/filter/LoadFilterUseCase.kt
1016. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/geo/CountryCatalog.kt
1017. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/goal/GoalMoneyMath.kt
1018. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/goal/LoadYieldCalculator.kt
1019. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/goal/WeekYieldSnapshot.kt
1020. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/goal/WeeklyGoalCalculator.kt
1021. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/goal/WeeklyGoalProgress.kt
1022. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/LoadValidator.kt
1023. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/model/ImportModels.kt
1024. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/CsvLoadParser.kt
1025. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/ExportTextLoadParser.kt
1026. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/HtmlLoadParser.kt
1027. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/ImportMessageType.kt
1028. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/LoadParser.kt
1029. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/MessageTypeDetector.kt
1030. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/ParserFactory.kt
1031. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/RelayMessageParser.kt
1032. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/TelegramHtmlExportParser.kt
1033. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/TelegramJsonExportParser.kt
1034. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/parser/TextLoadParser.kt
1035. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/repository/LoadImportRepository.kt
1036. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/import/usecase/ImportLoadsUseCase.kt
1037. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/Diesel.kt
1038. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/DieselParseResult.kt
1039. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/Load.kt
1040. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/LoadMetrics.kt
1041. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/Paycheck.kt
1042. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/PaycheckParseResult.kt
1043. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/Penalty.kt
1044. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/PeriodSummary.kt
1045. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/RouteStats.kt
1046. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/StateRevenue.kt
1047. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/Stop.kt
1048. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/TripId.kt
1049. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/WeekSummary.kt
1050. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/analytics/AnalyticsPeriod.kt
1051. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/analytics/AnalyticsSummary.kt
1052. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/analytics/DailyData.kt
1053. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/analytics/RouteData.kt
1054. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/model/analytics/WeekData.kt
1055. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/AmazonRelayParser.kt
1056. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/DieselTextParser.kt
1057. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/DuplicateAuditUseCase.kt
1058. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/DuplicateChecker.kt
1059. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/LoadChangeDetector.kt
1060. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/LoadComparison.kt
1061. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/LoadImportHelper.kt
1062. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/LoadMessageParser.kt
1063. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/LoadProcessor.kt
1064. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/LoadUpdater.kt
1065. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/MessageClassifier.kt
1066. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/MessageParseService.kt
1067. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/MessageType.kt
1068. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/ParseUtils.kt
1069. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/PaycheckTextParser.kt
1070. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/StopsHasher.kt
1071. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/parser/TelegramStyledTextNormalizer.kt
1072. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/BadgeEngine.kt
1073. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/EnhancedSocialModels.kt
1074. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/GroupInviteCode.kt
1075. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/ProfileMappers.kt
1076. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/SocialBadges.kt
1077. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/SocialLoadStats.kt
1078. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/SocialModels.kt
1079. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/social/SocialResult.kt
1080. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/usecase/ForecastService.kt
1081. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/usecase/FuelAnalyticsService.kt
1082. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/domain/voice/VoiceModels.kt
1083. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/MainActivity.kt
1084. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/auth/GoogleSignInLauncher.kt
1085. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/AdaptiveScaffold.kt
1086. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/AnimatedCircularProgress.kt
1087. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/AnimatedNumber.kt
1088. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/AppDrawer.kt
1089. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/AutoRestoreDialog.kt
1090. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/BentoGrid.kt
1091. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/BotStatusBadge.kt
1092. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/CameraButton.kt
1093. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/ComparisonIndicator.kt
1094. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/CountryPhoneFields.kt
1095. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/DisputeSection.kt
1096. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/ExportBottomSheet.kt
1097. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/ForecastCard.kt
1098. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/FuelAnalyticsCard.kt
1099. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/GlassCard.kt
1100. ✅ [G-quality] Code-quality pass: unused imports/warnings for com/truckerload/presentation/components/GoalLinearProgress.kt
