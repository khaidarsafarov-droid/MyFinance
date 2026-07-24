package com.truckerload.sync

import androidx.core.content.edit
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadImportRepositoryImpl
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.import.usecase.ImportLoadsUseCase
import com.truckerload.sync.DuplicateAuditRunner
import com.truckerload.sync.import.ImportCommandHandler
import com.truckerload.sync.import.ImportDocumentHandler
import com.truckerload.sync.import.ImportHandlerSupport
import com.truckerload.sync.import.ImportMessageHandler
import com.truckerload.sync.import.ImportReportFormatter
import com.truckerload.sync.import.ImportSessionManager
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.parser.LoadProcessor
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.domain.parser.ParserConfig
import com.truckerload.domain.parser.ProcessingResult
import com.truckerload.data.preferences.AccountIds
import com.truckerload.utils.LogRedactor
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.LoadImporter
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetUpdateWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared Telegram long-poll + parse + DB + reply logic for Worker and ForegroundService.
 */
class TelegramBotSyncEngine(private val context: Context) {

    private val messageParseService = MessageParseService()
    private val messageArchive = TelegramMessageArchive(context)
    private val settingsDataStore = SettingsDataStore(context)

    suspend fun runOnce(token: String): SyncRunResult {
        if (token.isBlank()) {
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        val result = TelegramPollCoordinator.withPollLock {
            runOnceLocked(token)
        }
        return result ?: SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 15)
    }

    private suspend fun runOnceLocked(token: String): SyncRunResult {
        val userId = AuthStore(context).currentUserIdOrNull()
        if (userId.isNullOrBlank()) {
            Log.w(TAG, "No active user session — skip Telegram sync")
            return SyncRunResult(skipped = true, processedUpdates = 0, nextDelaySeconds = 60)
        }
        val prefs = telegramSyncPrefs(context, userId)
        val settingsDataStore = SettingsDataStore(context)
        var nextRequestOffset = loadNextRequestOffset(prefs, settingsDataStore)
        Log.d(TAG, "📥 Last update offset (next request): $nextRequestOffset user=$userId")

        val telegramApi = TelegramApi(token)

        val db = AppDatabase.getInstance(context, userId)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)
        val chatRestore = TelegramChatRestore(db.telegramInboxDao(), messageArchive)

        val result = telegramApi.getUpdates(
            offset = nextRequestOffset.takeIf { it > 0L },
            timeoutSeconds = 25
        ).getOrElse { e ->
            Log.e(TAG, "getUpdates failed: ${LogRedactor.redact(e.message)}", e)
            val delay = if (e.message?.contains("409") == true) 45L else 30L
            if (TelegramAuthErrors.shouldStopService(e.message)) {
                TelegramBotForegroundService.stop(context)
            }
            return SyncRunResult(
                skipped = false,
                processedUpdates = 0,
                nextDelaySeconds = delay,
                error = LogRedactor.redact(e.message),
            )
        }

        Log.d(TAG, "📥 Received ${result.updates.size} updates (rawMax=${result.rawMaxUpdateId})")

        var processed = 0
        var stoppedOnFailure = false
        for (update in result.updates) {
            if (update.updateId + 1 <= nextRequestOffset) {
                Log.d(TAG, "⏭️ Skipping already processed updateId=${update.updateId}")
                continue
            }
            processed++
            Log.d(TAG, "📥 Processing updateId=${update.updateId}")
            try {
                handleUpdate(
                    update = update,
                    telegramApi = telegramApi,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                    chatRestore = chatRestore,
                    prefs = prefs
                )
                nextRequestOffset = update.updateId + 1
                persistNextRequestOffset(prefs, settingsDataStore, nextRequestOffset)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "handleUpdate failed for updateId=${update.updateId}; offset NOT advanced: ${LogRedactor.redact(e.message)}",
                    e,
                )
                // Stop this poll cycle so the failed update is retried next run.
                // Do NOT jump to result.nextOffset — that would skip the failed update.
                stoppedOnFailure = true
                break
            }
        }

        if (!stoppedOnFailure && result.nextOffset > nextRequestOffset) {
            nextRequestOffset = result.nextOffset
            persistNextRequestOffset(prefs, settingsDataStore, nextRequestOffset)
        }

        val nextDelay = when {
            processed > 0 -> 1L
            result.updates.isNotEmpty() -> 1L
            else -> 2L
        }
        Log.d(TAG, "📥 runOnce done processed=$processed nextOffset=$nextRequestOffset")
        return SyncRunResult(skipped = false, processedUpdates = processed, nextDelaySeconds = nextDelay)
    }

    private suspend fun loadNextRequestOffset(
        prefs: SharedPreferences,
        settingsDataStore: SettingsDataStore
    ): Long {
        val fromDataStore = settingsDataStore.getLastUpdateOffset()
        val fromPrefs = prefs.getLong(TelegramSyncWorker.KEY_LAST_OFFSET, 0L)
        return maxOf(fromDataStore, fromPrefs)
    }

    private suspend fun persistNextRequestOffset(
        prefs: SharedPreferences,
        settingsDataStore: SettingsDataStore,
        offset: Long
    ) {
        prefs.edit(commit = true) {putLong(TelegramSyncWorker.KEY_LAST_OFFSET, offset)}
        settingsDataStore.saveLastUpdateOffset(offset)
    }

    private suspend fun importMessageHandler(
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
    ): ImportMessageHandler {
        val db = AppDatabase.getInstanceForActiveUser(context)
            ?: error("No active user for Telegram import")
        val importRepo = LoadImportRepositoryImpl(loadRepository, db.loadDao())
        val settingsDataStore = SettingsDataStore(context)
        val config = parserConfig(settingsDataStore)
        val useCase = ImportLoadsUseCase(
            loadRepository = importRepo,
            loadProcessor = LoadProcessor(loadRepository),
            parserConfig = config,
        )
        val sessionManager = ImportSessionManager(prefs)
        return ImportMessageHandler(
            context = context,
            importUseCase = useCase,
            sessionManager = sessionManager,
            reportFormatter = ImportReportFormatter(context),
            loadRepository = loadRepository,
            paycheckRepository = PaycheckRepository(db),
            dieselRepository = DieselRepository(db),
        )
    }

    private suspend fun importDocumentHandler(
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
    ): ImportDocumentHandler {
        val db = AppDatabase.getInstanceForActiveUser(context)
            ?: error("No active user for Telegram import")
        val importRepo = LoadImportRepositoryImpl(loadRepository, db.loadDao())
        val settingsDataStore = SettingsDataStore(context)
        val config = parserConfig(settingsDataStore)
        val useCase = ImportLoadsUseCase(
            loadRepository = importRepo,
            loadProcessor = LoadProcessor(loadRepository),
            parserConfig = config,
        )
        return ImportDocumentHandler(
            context = context,
            importUseCase = useCase,
            sessionManager = ImportSessionManager(prefs),
            reportFormatter = ImportReportFormatter(context),
            loadRepository = loadRepository,
            paycheckRepository = PaycheckRepository(db),
            dieselRepository = DieselRepository(db),
        )
    }

    private suspend fun runDuplicateAudit(
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): String {
        val report = DuplicateAuditRunner.run(
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
        )
        if (report.deletedLoads + report.deletedPaychecks + report.deletedDiesel > 0) {
            DuplicateAuditRunner.refreshWidgets(context)
        }
        return DuplicateAuditRunner.formatReport(context, report)
    }

    private suspend fun parserConfig(settingsDataStore: SettingsDataStore): ParserConfig =
        ParserConfig(
            autoUpdate = settingsDataStore.getParserAutoUpdateOnce(),
            priceThresholdPercent = settingsDataStore.getParserPriceThresholdOnce(),
        )

    private suspend fun handleUpdate(
        update: com.truckerload.data.remote.TelegramUpdate,
        telegramApi: TelegramApi,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        chatRestore: TelegramChatRestore,
        prefs: SharedPreferences
    ) {
        if (!update.isCallbackQuery && update.text.isNotBlank()) {
            chatRestore.persistIncoming(update)
        }
        rememberTelegramChatId(update.chatId)

        if (update.isCallbackQuery && update.callbackQueryId != null) {
            val cmd = TelegramBotFeatures.menuButtonToCommand(update.text)
                ?: update.text.takeIf { it.startsWith("cmd:") }
                    ?.removePrefix("cmd:")
                    ?.let { "/$it" }
            update.callbackQueryId.let { id ->
                telegramApi.answerCallbackQuery(id, "OK").onFailure { e ->
                    Log.w(TAG, "answerCallbackQuery: ${LogRedactor.redact(e.message)}")
                }
            }
            if (cmd != null) {
                handleCommand(
                    cmd,
                    update.chatId,
                    telegramApi,
                    loadRepository,
                    paycheckRepository,
                    dieselRepository,
                    chatRestore,
                    prefs
                )
            }
            return
        }

        var rawText = update.text.trim()
        if (TelegramBotFeatures.isMenuButtonText(rawText)) {
            rawText = TelegramBotFeatures.menuButtonToCommand(rawText) ?: rawText
        }

        when {
            isCommand(rawText, "/start") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_welcome)
                )
                return
            }
            isCommand(rawText, "/help") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_help)
                )
                return
            }
            isCommand(rawText, "/status") -> {
                val status = buildStatusMessage(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, update.chatId, status)
                return
            }
            isCommand(rawText, "/stats") -> {
                val stats = buildStatsMessage(loadRepository)
                sendWithMenu(telegramApi, update.chatId, stats)
                return
            }
            isCommand(rawText, "/dedup") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_dedup_running),
                )
                val report = runDuplicateAudit(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, update.chatId, report)
                return
            }
            isCommand(rawText, "/cancel") -> {
                val importSessions = ImportSessionManager(prefs)
                val importCancelled = importSessions.cancelSession(update.chatId)
                val restoreCancelled = if (isManualRestoreMode(prefs, update.chatId)) {
                    clearManualRestoreMode(prefs, update.chatId)
                    true
                } else false
                val message = when {
                    importCancelled && restoreCancelled ->
                        context.getString(R.string.sync_import_and_restore_cancelled)
                    importCancelled -> context.getString(R.string.sync_import_cancelled)
                    restoreCancelled -> context.getString(R.string.sync_restore_manual_cancelled)
                    else -> context.getString(R.string.sync_restore_not_active)
                }
                sendWithMenu(telegramApi, update.chatId, message)
                return
            }
            isCommand(rawText, "/import") -> {
                val importSessions = ImportSessionManager(prefs)
                if (isManualRestoreMode(prefs, update.chatId)) {
                    clearManualRestoreMode(prefs, update.chatId)
                }
                ImportCommandHandler(context, importSessions)
                    .startImport(update.chatId) { id -> clearManualRestoreMode(prefs, id) }
                ImportCommandHandler(context, importSessions)
                    .sendPrompt(update.chatId, telegramApi)
                return
            }
            isCommand(rawText, "/help_load") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_help_load)
                )
                return
            }
            isCommand(rawText, "/help_pay") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_help_pay)
                )
                return
            }
            TelegramBotFeatures.isRestoreRequest(rawText) || isCommand(rawText, "/restore") -> {
                ImportSessionManager(prefs).endSession(update.chatId)
                startManualRestoreMode(prefs, update.chatId)
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_restore_manual_prompt)
                )
                return
            }
            rawText.isBlank() && update.photoFileId != null -> {
                telegramApi.sendMessage(update.chatId, context.getString(R.string.sync_ocr_disabled))
                return
            }
            rawText.isBlank() && update.documentFileId != null -> {
                val importSessions = ImportSessionManager(prefs)
                val fileId = update.documentFileId ?: return
                val isHtml = ImportDocumentHandler.isSupportedHtml(
                    update.documentFileName,
                    update.documentMimeType,
                )
                val isJson = ImportDocumentHandler.isSupportedJson(
                    update.documentFileName,
                    update.documentMimeType,
                )

                if (isHtml || isJson) {
                    if (!importSessions.isActive(update.chatId)) {
                        importSessions.startSession(update.chatId)
                    }
                    val reply = importDocumentHandler(loadRepository, prefs).handle(
                        chatId = update.chatId,
                        fileId = fileId,
                        fileName = update.documentFileName,
                        mimeType = update.documentMimeType,
                        fileSize = update.documentFileSize,
                        telegramApi = telegramApi,
                    )
                    if (reply.isNotBlank()) {
                        sendWithMenu(telegramApi, update.chatId, reply)
                    }
                    return
                }

                if (importSessions.isActive(update.chatId)) {
                    if (isExportTextDocument(update)) {
                        val bytes = telegramApi.downloadFile(fileId).getOrElse {
                            sendWithMenu(
                                telegramApi,
                                update.chatId,
                                context.getString(R.string.sync_import_error, it.message ?: "?"),
                            )
                            return
                        }
                        val text = bytes.toString(Charsets.UTF_8)
                        val reply = importMessageHandler(loadRepository, prefs)
                            .handle(update.chatId, text, telegramApi)
                        if (reply.isNotBlank()) {
                            sendWithMenu(telegramApi, update.chatId, reply)
                        }
                        return
                    }
                    sendWithMenu(
                        telegramApi,
                        update.chatId,
                        context.getString(R.string.sync_import_unsupported_file),
                    )
                    return
                }
                if (isExportTextDocument(update)) {
                    val reply = importExportDocument(
                        telegramApi = telegramApi,
                        update = update,
                        loadRepository = loadRepository,
                        paycheckRepository = paycheckRepository,
                        dieselRepository = dieselRepository,
                    )
                    sendWithMenu(telegramApi, update.chatId, reply)
                    return
                }
                val msg = if (update.documentMimeType?.startsWith("image/") == true) {
                    context.getString(R.string.sync_ocr_disabled)
                } else {
                    context.getString(R.string.sync_doc_not_supported)
                }
                telegramApi.sendMessage(update.chatId, msg)
                return
            }
            rawText.isBlank() -> {
                telegramApi.sendMessage(update.chatId, context.getString(R.string.sync_no_new_data))
                return
            }
        }

        val importSessions = ImportSessionManager(prefs)
        if (importSessions.isActive(update.chatId) && rawText.isNotBlank()) {
            val reply = importMessageHandler(loadRepository, prefs)
                .handle(update.chatId, rawText, telegramApi)
            if (reply.isNotBlank()) {
                sendWithMenu(telegramApi, update.chatId, reply)
            }
            return
        }

        if (isManualRestoreMode(prefs, update.chatId)) {
            touchManualRestoreActivity(prefs, update.chatId)
            val reply = processManualRestoreMessage(
                text = rawText,
                messageDateSeconds = update.messageDateSeconds,
                chatId = update.chatId,
                loadRepository = loadRepository,
                prefs = prefs
            )
            sendWithMenu(telegramApi, update.chatId, reply)
            return
        }

        telegramApi.sendMessage(update.chatId, context.getString(R.string.sync_processing))
            .onFailure { e -> Log.e(TAG, "ack failed: ${LogRedactor.redact(e.message)}") }

        val reply = processMessage(
            text = rawText,
            messageDateSeconds = update.messageDateSeconds,
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
            prefs = prefs
        )
        telegramApi.sendMessage(update.chatId, reply)
            .onFailure { e -> Log.e(TAG, "reply failed: ${LogRedactor.redact(e.message)}") }
    }

    private suspend fun handleCommand(
        command: String,
        chatId: String,
        telegramApi: TelegramApi,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        chatRestore: TelegramChatRestore,
        prefs: SharedPreferences
    ) {
        when {
            isCommand(command, "/start") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_welcome))
            isCommand(command, "/help") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_help))
            isCommand(command, "/status") -> {
                val status = buildStatusMessage(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, chatId, status)
            }
            isCommand(command, "/stats") -> {
                val stats = buildStatsMessage(loadRepository)
                sendWithMenu(telegramApi, chatId, stats)
            }
            isCommand(command, "/dedup") -> {
                sendWithMenu(
                    telegramApi,
                    chatId,
                    context.getString(R.string.sync_dedup_running),
                )
                val report = runDuplicateAudit(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, chatId, report)
            }
            isCommand(command, "/help_load") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_help_load))
            isCommand(command, "/help_pay") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_help_pay))
            isCommand(command, "/import") -> {
                val importSessions = ImportSessionManager(prefs)
                if (isManualRestoreMode(prefs, chatId)) clearManualRestoreMode(prefs, chatId)
                ImportCommandHandler(context, importSessions)
                    .startImport(chatId) { id -> clearManualRestoreMode(prefs, id) }
                ImportCommandHandler(context, importSessions).sendPrompt(chatId, telegramApi)
            }
            isCommand(command, "/cancel") -> {
                val importSessions = ImportSessionManager(prefs)
                val importCancelled = importSessions.cancelSession(chatId)
                val restoreCancelled = if (isManualRestoreMode(prefs, chatId)) {
                    clearManualRestoreMode(prefs, chatId)
                    true
                } else false
                val message = when {
                    importCancelled && restoreCancelled ->
                        context.getString(R.string.sync_import_and_restore_cancelled)
                    importCancelled -> context.getString(R.string.sync_import_cancelled)
                    restoreCancelled -> context.getString(R.string.sync_restore_manual_cancelled)
                    else -> context.getString(R.string.sync_restore_not_active)
                }
                sendWithMenu(telegramApi, chatId, message)
            }
            isCommand(command, "/restore") || TelegramBotFeatures.isRestoreRequest(command) -> {
                ImportSessionManager(prefs).endSession(chatId)
                startManualRestoreMode(prefs, chatId)
                sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_restore_manual_prompt))
            }
        }
    }

    private fun manualRestoreModeStore(prefs: SharedPreferences): ManualRestoreModeStore =
        ManualRestoreModeStore(prefs)

    private fun startManualRestoreMode(prefs: SharedPreferences, chatId: String) {
        manualRestoreModeStore(prefs).start(chatId)
    }

    private fun touchManualRestoreActivity(prefs: SharedPreferences, chatId: String) {
        manualRestoreModeStore(prefs).touch(chatId)
    }

    private fun isManualRestoreMode(prefs: SharedPreferences, chatId: String): Boolean {
        return manualRestoreModeStore(prefs).isActive(chatId)
    }

    private fun clearManualRestoreMode(prefs: SharedPreferences, chatId: String) {
        manualRestoreModeStore(prefs).clear(chatId)
    }

    private fun incrementManualRestoreCount(prefs: SharedPreferences, chatId: String): Int {
        return manualRestoreModeStore(prefs).incrementCount(chatId)
    }

    private fun telegramLoadHandler(loadRepository: LoadRepository): TelegramLoadHandler =
        TelegramLoadHandler(
            context = context,
            loadRepository = loadRepository,
            messageParseService = messageParseService,
            settingsDataStore = settingsDataStore,
        )

    private suspend fun processManualRestoreMessage(
        text: String,
        messageDateSeconds: Long?,
        chatId: String,
        loadRepository: LoadRepository,
        prefs: SharedPreferences
    ): String {
        if (!MessageClassifier.isLoadLike(text)) {
            return context.getString(R.string.sync_restore_manual_not_load)
        }

        val parsed = messageParseService.parseLoadsFromMessage(text).getOrNull().orEmpty()
        if (parsed.isEmpty()) {
            return context.getString(R.string.sync_restore_manual_not_load)
        }

        val handler = telegramLoadHandler(loadRepository)
        val results = handler.processLoadsStructured(
            loads = parsed,
            rawMessage = text,
            messageDateSeconds = messageDateSeconds,
            playFeedback = false,
        )

        val replies = mutableListOf<String>()
        var addedCount = 0
        parsed.zip(results).forEach { (load, result) ->
            when (result) {
                is ProcessingResult.Added -> {
                    addedCount++
                    incrementManualRestoreCount(prefs, chatId)
                    replies.add(
                        context.getString(
                            R.string.sync_restore_manual_added,
                            load.tripId,
                            load.totalMiles,
                            String.format(Locale.US, "%,.2f", load.totalRate),
                        )
                    )
                }
                is ProcessingResult.Updated,
                is ProcessingResult.Replaced -> {
                    addedCount++
                    incrementManualRestoreCount(prefs, chatId)
                    replies.add(handler.formatProcessingResult(result, load.tripId))
                }
                is ProcessingResult.Skipped -> {
                    replies.add(handler.formatProcessingResult(result, load.tripId))
                }
            }
        }

        if (addedCount > 0) {
            FeedbackManager.onLoadAdded()
        }

        return if (replies.isEmpty()) {
            context.getString(R.string.sync_no_new_data)
        } else {
            replies.joinToString("\n\n")
        }
    }

    private suspend fun buildStatsMessage(loadRepository: LoadRepository): String {
        return TelegramStatusMessages.buildStatsMessage(context, loadRepository)
    }

    private suspend fun buildStatusMessage(
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository
    ): String {
        return TelegramStatusMessages.buildStatusMessage(
            context = context,
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
        )
    }

    private suspend fun sendWithMenu(telegramApi: TelegramApi, chatId: String, text: String) {
        telegramApi.sendMessage(chatId, text, TelegramBotFeatures.mainMenuKeyboard())
            .onFailure { e -> Log.e(TAG, "menu reply failed: ${LogRedactor.redact(e.message)}") }
    }

    private fun isCommand(text: String, command: String): Boolean {
        val trimmed = text.trim()
        return trimmed.equals(command, ignoreCase = true) ||
            trimmed.startsWith("$command@", ignoreCase = true) ||
            trimmed.startsWith("$command ", ignoreCase = true)
    }

    private suspend fun processMessage(
        text: String,
        messageDateSeconds: Long?,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences
    ): String {
        messageParseService.parseLoadsFromMessage(text)
            .onSuccess { incomingLoads ->
                if (incomingLoads.isNotEmpty()) {
                    return telegramLoadHandler(loadRepository).handleLoads(
                        loads = incomingLoads,
                        rawMessage = text,
                        messageDateSeconds = messageDateSeconds,
                    )
                }
            }

        messageParseService.parsePaycheckFromText(text)
            .onSuccess { r ->
                val (weekNumber, year) = if (messageDateSeconds != null && r.weekStartDate.isNullOrBlank())
                    getWeekNumberAndYearFromDate(formatDateFromUnixSeconds(messageDateSeconds))
                else
                    getWeekNumberAndYearFromDate(r.weekStartDate)
                if (r.netAmount <= 0) return context.getString(R.string.sync_paycheck_not_found)
                if (paycheckRepository.getPaycheckForWeek(weekNumber, year) != null) {
                    return context.getString(R.string.sync_paycheck_exists, weekNumber)
                }
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                paycheckRepository.insertPaycheck(
                    Paycheck(
                        id = 0,
                        weekNumber = weekNumber,
                        year = year,
                        weekLabel = weekLabel,
                        weekStartDate = weekStart,
                        weekEndDate = weekEnd,
                        driverName = r.driverName,
                        grossAmount = r.grossAmount,
                        netAmount = r.netAmount,
                        rawExtractedText = text,
                        sourceFileName = null,
                        addedAt = System.currentTimeMillis()
                    )
                )
                return context.getString(
                    R.string.sync_last_paycheck,
                    String.format(Locale.US, "%,.2f", r.netAmount),
                    weekNumber
                )
            }

        messageParseService.parseDieselFromText(text)
            .onSuccess { r ->
                val dateForWeek = when {
                    messageDateSeconds != null && r.date.isNullOrBlank() -> formatDateFromUnixSeconds(messageDateSeconds)
                    else -> r.date
                }
                val (weekNumber, year) = getWeekNumberAndYearFromDate(dateForWeek)
                if (r.totalAmount <= 0) return context.getString(R.string.sync_diesel_not_found)
                val textHash = text.hashCode()
                val lastDieselHash = prefs.getInt("last_diesel_text_hash", 0)
                if (lastDieselHash != 0 && lastDieselHash == textHash) {
                    return context.getString(R.string.sync_duplicate_diesel)
                }
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                dieselRepository.insertDiesel(
                    Diesel(
                        id = 0,
                        weekNumber = weekNumber,
                        year = year,
                        weekLabel = weekLabel,
                        weekStartDate = weekStart,
                        weekEndDate = weekEnd,
                        totalAmount = r.totalAmount,
                        gallons = r.gallons,
                        pricePerGallon = r.pricePerGallon,
                        location = r.location,
                        rawExtractedText = text,
                        sourceFileName = null,
                        addedAt = System.currentTimeMillis()
                    )
                )
                prefs.edit {putInt("last_diesel_text_hash", textHash)}
                return context.getString(
                    R.string.sync_last_diesel,
                    String.format(Locale.US, "%,.2f", r.totalAmount),
                    weekNumber
                )
            }

        return context.getString(R.string.sync_parse_failed)
    }

    private suspend fun importExportDocument(
        telegramApi: TelegramApi,
        update: com.truckerload.data.remote.TelegramUpdate,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): String {
        val fileId = update.documentFileId ?: return context.getString(R.string.sync_doc_not_supported)
        val bytes = telegramApi.downloadFile(fileId).getOrElse { e ->
            Log.e(TAG, "download export file failed: ${LogRedactor.redact(e.message)}", e)
            return context.getString(R.string.restore_error, e.message.orEmpty())
        }
        val text = bytes.toString(Charsets.UTF_8)
        val config = parserConfig(SettingsDataStore(context))
        val result = LoadImporter.importFromText(loadRepository, text, config)
        if (result.parsed == 0) {
            return context.getString(R.string.sync_restore_manual_not_load)
        }
        WidgetDataUpdater.updateWidgetData(context)
        WidgetUpdateWorker.refreshNow(context)
        val base = context.getString(R.string.file_received, result.imported)
        val dedupSection = ImportHandlerSupport.runPostImportDedup(
            context = context,
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
        )
        return if (dedupSection.isBlank()) base else "$base\n\n$dedupSection"
    }

    private fun isExportTextDocument(update: com.truckerload.data.remote.TelegramUpdate): Boolean {
        val name = update.documentFileName?.lowercase(Locale.US).orEmpty()
        val mime = update.documentMimeType?.lowercase(Locale.US).orEmpty()
        return name.endsWith(".txt") ||
            mime == "text/plain" ||
            name.contains("trucklog_export") ||
            name.contains("truckerload_export")
    }

    private fun rememberTelegramChatId(chatId: String) {
        val id = chatId.toLongOrNull() ?: return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching { SettingsDataStore(context).saveTelegramChatId(id) }
        }
    }

    data class SyncRunResult(
        val skipped: Boolean,
        val processedUpdates: Int,
        val nextDelaySeconds: Long,
        val error: String? = null
    )

    companion object {
        private const val TAG = "TelegramBotSync"
        private val exportCaptionDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US)

        fun telegramSyncPrefs(context: Context, userId: String): SharedPreferences {
            val name = "telegram_sync_${AccountIds.sanitizeFilePart(userId)}"
            val scoped = context.getSharedPreferences(name, Context.MODE_PRIVATE)
            val meta = context.getSharedPreferences("truckerload_account_meta", Context.MODE_PRIVATE)
            val migrated = meta.getBoolean("legacy_telegram_offset_migrated", false)
            if (!migrated && !scoped.contains(TelegramSyncWorker.KEY_LAST_OFFSET)) {
                val legacy = context.getSharedPreferences(TelegramSyncWorker.PREFS_NAME, Context.MODE_PRIVATE)
                val offset = legacy.getLong(TelegramSyncWorker.KEY_LAST_OFFSET, 0L)
                if (offset > 0L) {
                    scoped.edit(commit = true) {
                        putLong(TelegramSyncWorker.KEY_LAST_OFFSET, offset)
                    }
                }
                meta.edit().putBoolean("legacy_telegram_offset_migrated", true).apply()
            }
            return scoped
        }

        suspend fun sendFileToTelegram(
            context: Context,
            token: String,
            chatId: Long,
            file: File
        ): Result<Unit> {
            val caption = context.getString(
                R.string.telegram_export_caption,
                exportCaptionDate.format(Date())
            )
            return TelegramApi(token).sendDocument(chatId.toString(), file, caption)
        }
    }
}
