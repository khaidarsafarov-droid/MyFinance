package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.sync.TelegramChatRestore
import com.truckerload.sync.TelegramStatusMessages
import com.truckerload.sync.import.ImportCommandHandler
import com.truckerload.sync.import.ImportDocumentHandler
import com.truckerload.sync.import.ImportSessionManager
import com.truckerload.utils.LogRedactor
/**
 * Routes Telegram updates / commands to import, restore, or inbound parse paths.
 */
class TelegramUpdateDispatcher(
    private val context: Context,
    private val apiClient: TelegramApiClient,
    private val messageParser: TelegramMessageParser,
) {
    suspend fun handleUpdate(
        update: TelegramUpdate,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        chatRestore: TelegramChatRestore,
        prefs: SharedPreferences,
        stateMachine: TelegramStateMachine,
    ) {
        // FIX: only the paired private chat may mutate journal data / hijack exports
        if (!authorizeChat(update)) return

        if (!update.isCallbackQuery && update.text.isNotBlank()) {
            chatRestore.persistIncoming(update)
        }

        if (update.isCallbackQuery && update.callbackQueryId != null) {
            if (TelegramReceiptKeyboard.isReceiptCallback(update.text)) {
                ingest().handleCallback(
                    data = update.text,
                    chatId = update.chatId,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                    prefs = prefs,
                    callbackQueryId = update.callbackQueryId,
                )
                return
            }
            val cmd = TelegramBotFeatures.menuButtonToCommand(update.text)
                ?: update.text.takeIf { it.startsWith("cmd:") }
                    ?.removePrefix("cmd:")
                    ?.let { "/$it" }
            update.callbackQueryId.let { id ->
                apiClient.answerCallbackQuery(id, "OK").onFailure { e ->
                    Log.w(TAG, "answerCallbackQuery: ${LogRedactor.redact(e.message)}")
                }
            }
            if (cmd != null) {
                handleCommand(
                    cmd,
                    update.chatId,
                    loadRepository,
                    paycheckRepository,
                    dieselRepository,
                    prefs,
                    stateMachine,
                )
            }
            return
        }

        var rawText = update.text.trim()
        if (TelegramBotFeatures.isMenuButtonText(rawText)) {
            rawText = TelegramBotFeatures.menuButtonToCommand(rawText) ?: rawText
        }
        rawText = TelegramBotFeatures.aliasCommand(rawText)

        when {
            isCommand(rawText, "/start") -> {
                apiClient.sendWithMenu(update.chatId, context.getString(R.string.sync_welcome))
                return
            }
            isCommand(rawText, "/help") -> {
                apiClient.sendWithMenu(update.chatId, context.getString(R.string.sync_help))
                return
            }
            isCommand(rawText, "/status") -> {
                val status = buildStatusMessage(loadRepository, paycheckRepository, dieselRepository)
                apiClient.sendWithMenu(update.chatId, status)
                return
            }
            isCommand(rawText, "/stats") -> {
                val stats = buildStatsMessage(loadRepository)
                apiClient.sendWithMenu(update.chatId, stats)
                return
            }
            isCommand(rawText, "/dedup") -> {
                apiClient.sendWithMenu(
                    update.chatId,
                    context.getString(R.string.sync_dedup_running),
                )
                val report = messageParser.runDuplicateAudit(loadRepository, paycheckRepository, dieselRepository)
                apiClient.sendWithMenu(update.chatId, report)
                return
            }
            isCommand(rawText, "/cancel") -> {
                val importSessions = ImportSessionManager(prefs)
                val importCancelled = importSessions.cancelSession(update.chatId)
                val restoreCancelled = if (stateMachine.isManualRestoreActive(update.chatId)) {
                    stateMachine.clearManualRestore(update.chatId)
                    true
                } else {
                    false
                }
                TelegramReceiptConfirmStore(prefs, context).clear(update.chatId, discardFile = true)
                val message = when {
                    importCancelled && restoreCancelled ->
                        context.getString(R.string.sync_import_and_restore_cancelled)
                    importCancelled -> context.getString(R.string.sync_import_cancelled)
                    restoreCancelled -> context.getString(R.string.sync_restore_manual_cancelled)
                    else -> context.getString(R.string.sync_restore_not_active)
                }
                apiClient.sendWithMenu(update.chatId, message)
                return
            }
            isCommand(rawText, "/import") -> {
                val importSessions = ImportSessionManager(prefs)
                if (stateMachine.isManualRestoreActive(update.chatId)) {
                    stateMachine.clearManualRestore(update.chatId)
                }
                ImportCommandHandler(context, importSessions)
                    .startImport(update.chatId) { id -> stateMachine.clearManualRestore(id) }
                ImportCommandHandler(context, importSessions)
                    .sendPrompt(update.chatId, apiClient.api())
                return
            }
            isCommand(rawText, "/help_load") -> {
                apiClient.sendWithMenu(update.chatId, context.getString(R.string.sync_help_load))
                return
            }
            isCommand(rawText, "/help_pay") -> {
                apiClient.sendWithMenu(update.chatId, context.getString(R.string.sync_help_pay))
                return
            }
            TelegramBotFeatures.isRestoreRequest(rawText) || isCommand(rawText, "/restore") -> {
                ImportSessionManager(prefs).endSession(update.chatId)
                stateMachine.startManualRestore(update.chatId)
                apiClient.sendWithMenu(
                    update.chatId,
                    context.getString(R.string.sync_restore_manual_prompt),
                )
                return
            }
            rawText.isBlank() && (update.photoFileId != null || update.documentFileId != null) -> {
                handleDocumentUpdate(
                    update = update,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                    prefs = prefs,
                )
                return
            }
            rawText.isBlank() -> {
                apiClient.sendMessage(update.chatId, context.getString(R.string.sync_no_new_data))
                return
            }
        }

        if (update.photoFileId != null || update.documentFileId != null) {
            handleDocumentUpdate(
                update = update,
                loadRepository = loadRepository,
                paycheckRepository = paycheckRepository,
                dieselRepository = dieselRepository,
                prefs = prefs,
            )
            return
        }

        val importSessions = ImportSessionManager(prefs)
        if (importSessions.isActive(update.chatId) && rawText.isNotBlank()) {
            val reply = messageParser.importMessageHandler(loadRepository, prefs)
                .handle(update.chatId, rawText, apiClient.api())
            if (reply.isNotBlank()) {
                apiClient.sendWithMenu(update.chatId, reply)
            }
            return
        }

        if (stateMachine.isManualRestoreActive(update.chatId)) {
            stateMachine.touchManualRestore(update.chatId)
            val reply = messageParser.processManualRestoreMessage(
                text = rawText,
                messageDateSeconds = update.messageDateSeconds,
                chatId = update.chatId,
                loadRepository = loadRepository,
                stateMachine = stateMachine,
            )
            apiClient.sendWithMenu(update.chatId, reply)
            return
        }

        apiClient.sendMessage(update.chatId, context.getString(R.string.sync_processing))
            .onFailure { e -> Log.e(TAG, "ack failed: ${LogRedactor.redact(e.message)}") }

        ingest().applyText(
            chatId = update.chatId,
            text = rawText,
            fileName = null,
            messageDateSeconds = update.messageDateSeconds,
            loadRepository = loadRepository,
            prefs = prefs,
        )
    }

    private suspend fun handleDocumentUpdate(
        update: TelegramUpdate,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
    ) {
        val importSessions = ImportSessionManager(prefs)
        val fileId = update.documentFileId
        if (fileId != null) {
            val isHtml = ImportDocumentHandler.isSupportedHtml(
                update.documentFileName,
                update.documentMimeType,
            )
            val isJson = ImportDocumentHandler.isSupportedJson(
                update.documentFileName,
                update.documentMimeType,
            )
            // FIX: only chat exports / an open /import session go to bulk import — a
            // standalone load.json must reach the single-load ingest path instead.
            val isChatExport = ImportDocumentHandler.isTelegramExportFileName(update.documentFileName) ||
                ImportDocumentHandler.isResultJsonFileName(update.documentFileName)
            if ((isHtml || isJson) && (isChatExport || importSessions.isActive(update.chatId))) {
                if (!importSessions.isActive(update.chatId)) {
                    importSessions.startSession(update.chatId)
                }
                val reply = messageParser.importDocumentHandler(loadRepository, prefs).handle(
                    chatId = update.chatId,
                    fileId = fileId,
                    fileName = update.documentFileName,
                    mimeType = update.documentMimeType,
                    fileSize = update.documentFileSize,
                    telegramApi = apiClient.api(),
                )
                if (reply.isNotBlank()) {
                    apiClient.sendWithMenu(update.chatId, reply)
                }
                return
            }
            if (importSessions.isActive(update.chatId) && messageParser.isExportTextDocument(update)) {
                ingestExportText(update, fileId, loadRepository, prefs)
                return
            }
            if (messageParser.isExportTextDocument(update)) {
                val reply = messageParser.importExportDocument(
                    telegramApi = apiClient.api(),
                    update = update,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                )
                apiClient.sendWithMenu(update.chatId, reply)
                return
            }
        }
        ingest().handleMedia(
            update = update,
            loadRepository = loadRepository,
            prefs = prefs,
        )
    }

    private suspend fun ingestExportText(
        update: TelegramUpdate,
        fileId: String,
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
    ) {
        val declaredSize = update.documentFileSize
        if (declaredSize != null && declaredSize > TelegramApi.MAX_DOWNLOAD_BYTES) {
            apiClient.sendWithMenu(
                update.chatId,
                context.getString(
                    R.string.sync_import_file_too_large,
                    (TelegramApi.MAX_DOWNLOAD_BYTES / (1024 * 1024)).toInt(),
                ),
            )
            return
        }
        val bytes = apiClient.downloadFile(fileId).getOrElse {
            apiClient.sendWithMenu(
                update.chatId,
                context.getString(R.string.sync_import_error, it.message ?: "?"),
            )
            return
        }
        val text = bytes.toString(Charsets.UTF_8)
        val reply = messageParser.importMessageHandler(loadRepository, prefs)
            .handle(update.chatId, text, apiClient.api())
        if (reply.isNotBlank()) {
            apiClient.sendWithMenu(update.chatId, reply)
        }
    }

    private fun ingest(): TelegramFileIngestHandler =
        TelegramFileIngestHandler(context, apiClient, messageParser)

    suspend fun handleCommand(
        command: String,
        chatId: String,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
        stateMachine: TelegramStateMachine,
    ) {
        val resolved = TelegramBotFeatures.aliasCommand(command)
        when {
            isCommand(resolved, "/start") -> apiClient.sendWithMenu(chatId, context.getString(R.string.sync_welcome))
            isCommand(resolved, "/help") -> apiClient.sendWithMenu(chatId, context.getString(R.string.sync_help))
            isCommand(resolved, "/status") -> {
                val status = buildStatusMessage(loadRepository, paycheckRepository, dieselRepository)
                apiClient.sendWithMenu(chatId, status)
            }
            isCommand(resolved, "/stats") -> {
                val stats = buildStatsMessage(loadRepository)
                apiClient.sendWithMenu(chatId, stats)
            }
            isCommand(resolved, "/dedup") -> {
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_dedup_running))
                val report = messageParser.runDuplicateAudit(loadRepository, paycheckRepository, dieselRepository)
                apiClient.sendWithMenu(chatId, report)
            }
            isCommand(resolved, "/help_load") ->
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_help_load))
            isCommand(resolved, "/help_pay") ->
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_help_pay))
            isCommand(resolved, "/import") -> {
                val importSessions = ImportSessionManager(prefs)
                if (stateMachine.isManualRestoreActive(chatId)) stateMachine.clearManualRestore(chatId)
                ImportCommandHandler(context, importSessions)
                    .startImport(chatId) { id -> stateMachine.clearManualRestore(id) }
                ImportCommandHandler(context, importSessions).sendPrompt(chatId, apiClient.api())
            }
            isCommand(resolved, "/cancel") -> {
                val importSessions = ImportSessionManager(prefs)
                val importCancelled = importSessions.cancelSession(chatId)
                val restoreCancelled = if (stateMachine.isManualRestoreActive(chatId)) {
                    stateMachine.clearManualRestore(chatId)
                    true
                } else {
                    false
                }
                TelegramReceiptConfirmStore(prefs, context).clear(chatId, discardFile = true)
                val message = when {
                    importCancelled && restoreCancelled ->
                        context.getString(R.string.sync_import_and_restore_cancelled)
                    importCancelled -> context.getString(R.string.sync_import_cancelled)
                    restoreCancelled -> context.getString(R.string.sync_restore_manual_cancelled)
                    else -> context.getString(R.string.sync_restore_not_active)
                }
                apiClient.sendWithMenu(chatId, message)
            }
            isCommand(resolved, "/restore") || TelegramBotFeatures.isRestoreRequest(resolved) -> {
                ImportSessionManager(prefs).endSession(chatId)
                stateMachine.startManualRestore(chatId)
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_restore_manual_prompt))
            }
        }
    }

    private suspend fun buildStatsMessage(loadRepository: LoadRepository): String =
        TelegramStatusMessages.buildStatsMessage(context, loadRepository)

    private suspend fun buildStatusMessage(
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): String = TelegramStatusMessages.buildStatusMessage(
        context = context,
        loadRepository = loadRepository,
        paycheckRepository = paycheckRepository,
        dieselRepository = dieselRepository,
    )

    fun isCommand(text: String, command: String): Boolean {
        val trimmed = text.trim()
        return trimmed.equals(command, ignoreCase = true) ||
            trimmed.startsWith("$command@", ignoreCase = true) ||
            trimmed.startsWith("$command ", ignoreCase = true)
    }

    /**
     * Pairing rules:
     * - Bound chat: only that chat id is accepted (others get a short reject).
     * - Unbound: `/start` in a private chat binds once; other traffic is rejected.
     * Never overwrite a bound chat id from inbound messages.
     */
    private suspend fun authorizeChat(update: TelegramUpdate): Boolean {
        val chatIdLong = update.chatId.toLongOrNull()
        if (chatIdLong == null) {
            Log.w(TAG, "Reject update with non-numeric chatId")
            return false
        }
        val settings = SettingsDataStore(context)
        val bound = settings.getTelegramChatIdOnce()
        if (bound != null) {
            if (bound == chatIdLong) return true
            // Still advance offset by returning successfully; do not process payload.
            runCatching {
                apiClient.sendMessage(
                    update.chatId,
                    context.getString(R.string.sync_chat_not_authorized),
                )
            }
            Log.w(TAG, "Rejected unauthorized chatId=$chatIdLong (bound=$bound)")
            return false
        }
        val raw = update.text.trim()
        val isStart = isCommand(raw, "/start") ||
            (update.isCallbackQuery && TelegramBotFeatures.menuButtonToCommand(raw) == "/start")
        if (isStart && (update.chatType == "private" || update.chatType.isBlank())) {
            settings.saveTelegramChatId(chatIdLong)
            Log.i(TAG, "Paired Telegram chatId=$chatIdLong")
            return true
        }
        runCatching {
            apiClient.sendMessage(
                update.chatId,
                context.getString(R.string.sync_chat_pair_required),
            )
        }
        Log.w(TAG, "Rejected unpaired chatId=$chatIdLong — send /start to pair")
        return false
    }

    private companion object {
        const val TAG = "TelegramBotSync"
    }
}
