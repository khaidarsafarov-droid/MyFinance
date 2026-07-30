package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadImportRepositoryImpl
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.domain.import.usecase.ImportLoadsUseCase
import com.truckerload.domain.parser.LoadProcessor
import com.truckerload.domain.parser.ParserConfig
import com.truckerload.sync.DuplicateAuditRunner
import com.truckerload.sync.ManualRestoreModeStore
import com.truckerload.sync.TelegramChatRestore
import com.truckerload.sync.TelegramStatusMessages
import com.truckerload.sync.import.ImportCommandHandler
import com.truckerload.sync.import.ImportDocumentHandler
import com.truckerload.sync.import.ImportHandlerSupport
import com.truckerload.sync.import.ImportMessageHandler
import com.truckerload.sync.import.ImportReportFormatter
import com.truckerload.sync.import.ImportSessionManager
import com.truckerload.utils.LoadImporter
import com.truckerload.utils.LogRedactor
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetUpdateWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Routes inbound Telegram updates: commands, import sessions, restore, and free-text parse.
 */
class TelegramUpdateDispatcher(
    private val context: Context,
    private val messageParser: TelegramMessageParser,
) {
    private val settingsDataStore = SettingsDataStore(context)

    suspend fun sendWithMenu(telegramApi: TelegramApi, chatId: String, text: String) {
        telegramApi.sendMessage(chatId, text, TelegramBotFeatures.mainMenuKeyboard())
            .onFailure { e -> Log.e(TAG, "menu reply failed: ${LogRedactor.redact(e.message)}") }
    }

    suspend fun importMessageHandler(
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

    suspend fun importDocumentHandler(
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

    suspend fun runDuplicateAudit(
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

    suspend fun parserConfig(settingsDataStore: SettingsDataStore): ParserConfig =
        ParserConfig(
            autoUpdate = settingsDataStore.getParserAutoUpdateOnce(),
            priceThresholdPercent = settingsDataStore.getParserPriceThresholdOnce(),
        )

    suspend fun handleUpdate(
        update: TelegramUpdate,
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
            messageParser.isCommand(rawText, "/start") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_welcome)
                )
                return
            }
            messageParser.isCommand(rawText, "/help") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_help)
                )
                return
            }
            messageParser.isCommand(rawText, "/status") -> {
                val status = buildStatusMessage(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, update.chatId, status)
                return
            }
            messageParser.isCommand(rawText, "/stats") -> {
                val stats = buildStatsMessage(loadRepository)
                sendWithMenu(telegramApi, update.chatId, stats)
                return
            }
            messageParser.isCommand(rawText, "/dedup") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_dedup_running),
                )
                val report = runDuplicateAudit(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, update.chatId, report)
                return
            }
            messageParser.isCommand(rawText, "/cancel") -> {
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
            messageParser.isCommand(rawText, "/import") -> {
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
            messageParser.isCommand(rawText, "/help_load") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_help_load)
                )
                return
            }
            messageParser.isCommand(rawText, "/help_pay") -> {
                sendWithMenu(
                    telegramApi,
                    update.chatId,
                    context.getString(R.string.sync_help_pay)
                )
                return
            }
            TelegramBotFeatures.isRestoreRequest(rawText) || messageParser.isCommand(rawText, "/restore") -> {
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
                    if (messageParser.isExportTextDocument(update)) {
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
                if (messageParser.isExportTextDocument(update)) {
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
            val reply = messageParser.processManualRestoreMessage(
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

        val reply = messageParser.processMessage(
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

    suspend fun handleCommand(
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
            messageParser.isCommand(command, "/start") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_welcome))
            messageParser.isCommand(command, "/help") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_help))
            messageParser.isCommand(command, "/status") -> {
                val status = buildStatusMessage(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, chatId, status)
            }
            messageParser.isCommand(command, "/stats") -> {
                val stats = buildStatsMessage(loadRepository)
                sendWithMenu(telegramApi, chatId, stats)
            }
            messageParser.isCommand(command, "/dedup") -> {
                sendWithMenu(
                    telegramApi,
                    chatId,
                    context.getString(R.string.sync_dedup_running),
                )
                val report = runDuplicateAudit(loadRepository, paycheckRepository, dieselRepository)
                sendWithMenu(telegramApi, chatId, report)
            }
            messageParser.isCommand(command, "/help_load") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_help_load))
            messageParser.isCommand(command, "/help_pay") -> sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_help_pay))
            messageParser.isCommand(command, "/import") -> {
                val importSessions = ImportSessionManager(prefs)
                if (isManualRestoreMode(prefs, chatId)) clearManualRestoreMode(prefs, chatId)
                ImportCommandHandler(context, importSessions)
                    .startImport(chatId) { id -> clearManualRestoreMode(prefs, id) }
                ImportCommandHandler(context, importSessions).sendPrompt(chatId, telegramApi)
            }
            messageParser.isCommand(command, "/cancel") -> {
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
            messageParser.isCommand(command, "/restore") || TelegramBotFeatures.isRestoreRequest(command) -> {
                ImportSessionManager(prefs).endSession(chatId)
                startManualRestoreMode(prefs, chatId)
                sendWithMenu(telegramApi, chatId, context.getString(R.string.sync_restore_manual_prompt))
            }
        }
    }

    fun manualRestoreModeStore(prefs: SharedPreferences): ManualRestoreModeStore =
        ManualRestoreModeStore(prefs)

    fun startManualRestoreMode(prefs: SharedPreferences, chatId: String) {
        manualRestoreModeStore(prefs).start(chatId)
    }

    fun touchManualRestoreActivity(prefs: SharedPreferences, chatId: String) {
        manualRestoreModeStore(prefs).touch(chatId)
    }

    fun isManualRestoreMode(prefs: SharedPreferences, chatId: String): Boolean {
        return manualRestoreModeStore(prefs).isActive(chatId)
    }

    fun clearManualRestoreMode(prefs: SharedPreferences, chatId: String) {
        manualRestoreModeStore(prefs).clear(chatId)
    }

    fun incrementManualRestoreCount(prefs: SharedPreferences, chatId: String): Int {
        return manualRestoreModeStore(prefs).incrementCount(chatId)
    }

    suspend fun buildStatsMessage(loadRepository: LoadRepository): String =
        TelegramStatusMessages.buildStatsMessage(context, loadRepository)

    suspend fun buildStatusMessage(
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): String = TelegramStatusMessages.buildStatusMessage(
        context = context,
        loadRepository = loadRepository,
        paycheckRepository = paycheckRepository,
        dieselRepository = dieselRepository,
    )

    suspend fun importExportDocument(
        telegramApi: TelegramApi,
        update: TelegramUpdate,
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

    fun rememberTelegramChatId(chatId: String) {
        val id = chatId.toLongOrNull() ?: return
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching { SettingsDataStore(context).saveTelegramChatId(id) }
        }
    }


    companion object {
        private const val TAG = "TelegramUpdateDisp"
    }
}
