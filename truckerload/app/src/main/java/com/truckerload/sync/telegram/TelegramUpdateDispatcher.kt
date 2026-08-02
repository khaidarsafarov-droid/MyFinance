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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
            rawText.isBlank() && update.photoFileId != null -> {
                apiClient.sendMessage(update.chatId, context.getString(R.string.sync_ocr_disabled))
                return
            }
            rawText.isBlank() && update.documentFileId != null -> {
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

        val reply = messageParser.processMessage(
            text = rawText,
            messageDateSeconds = update.messageDateSeconds,
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
            prefs = prefs,
        )
        apiClient.sendMessage(update.chatId, reply)
            .onFailure { e -> Log.e(TAG, "reply failed: ${LogRedactor.redact(e.message)}") }
    }

    private suspend fun handleDocumentUpdate(
        update: TelegramUpdate,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
    ) {
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

        if (importSessions.isActive(update.chatId)) {
            if (messageParser.isExportTextDocument(update)) {
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
                return
            }
            apiClient.sendWithMenu(
                update.chatId,
                context.getString(R.string.sync_import_unsupported_file),
            )
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
        val msg = if (update.documentMimeType?.startsWith("image/") == true) {
            context.getString(R.string.sync_ocr_disabled)
        } else {
            context.getString(R.string.sync_doc_not_supported)
        }
        apiClient.sendMessage(update.chatId, msg)
    }

    suspend fun handleCommand(
        command: String,
        chatId: String,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
        stateMachine: TelegramStateMachine,
    ) {
        when {
            isCommand(command, "/start") -> apiClient.sendWithMenu(chatId, context.getString(R.string.sync_welcome))
            isCommand(command, "/help") -> apiClient.sendWithMenu(chatId, context.getString(R.string.sync_help))
            isCommand(command, "/status") -> {
                val status = buildStatusMessage(loadRepository, paycheckRepository, dieselRepository)
                apiClient.sendWithMenu(chatId, status)
            }
            isCommand(command, "/stats") -> {
                val stats = buildStatsMessage(loadRepository)
                apiClient.sendWithMenu(chatId, stats)
            }
            isCommand(command, "/dedup") -> {
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_dedup_running))
                val report = messageParser.runDuplicateAudit(loadRepository, paycheckRepository, dieselRepository)
                apiClient.sendWithMenu(chatId, report)
            }
            isCommand(command, "/help_load") ->
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_help_load))
            isCommand(command, "/help_pay") ->
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_help_pay))
            isCommand(command, "/import") -> {
                val importSessions = ImportSessionManager(prefs)
                if (stateMachine.isManualRestoreActive(chatId)) stateMachine.clearManualRestore(chatId)
                ImportCommandHandler(context, importSessions)
                    .startImport(chatId) { id -> stateMachine.clearManualRestore(id) }
                ImportCommandHandler(context, importSessions).sendPrompt(chatId, apiClient.api())
            }
            isCommand(command, "/cancel") -> {
                val importSessions = ImportSessionManager(prefs)
                val importCancelled = importSessions.cancelSession(chatId)
                val restoreCancelled = if (stateMachine.isManualRestoreActive(chatId)) {
                    stateMachine.clearManualRestore(chatId)
                    true
                } else {
                    false
                }
                val message = when {
                    importCancelled && restoreCancelled ->
                        context.getString(R.string.sync_import_and_restore_cancelled)
                    importCancelled -> context.getString(R.string.sync_import_cancelled)
                    restoreCancelled -> context.getString(R.string.sync_restore_manual_cancelled)
                    else -> context.getString(R.string.sync_restore_not_active)
                }
                apiClient.sendWithMenu(chatId, message)
            }
            isCommand(command, "/restore") || TelegramBotFeatures.isRestoreRequest(command) -> {
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

    private fun rememberTelegramChatId(chatId: String) {
        val id = chatId.toLongOrNull() ?: return
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { SettingsDataStore(context).saveTelegramChatId(id) }
        }
    }

    private companion object {
        const val TAG = "TelegramBotSync"
    }
}
