package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import com.truckerload.R
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.sync.TelegramStatusMessages
import com.truckerload.sync.import.ImportCommandHandler
import com.truckerload.sync.import.ImportSessionManager

/**
 * Slash commands from the menu and from callback buttons such as cmd:status.
 */
class TelegramBotCommands(
    private val context: Context,
    private val apiClient: TelegramApiClient,
    private val messageParser: TelegramMessageParser,
) {
    suspend fun handle(
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
            isCommand(resolved, "/start") ->
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_welcome))
            isCommand(resolved, "/help") ->
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_help))
            isCommand(resolved, "/status") -> {
                val status = TelegramStatusMessages.buildStatusMessage(
                    context = context,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                )
                apiClient.sendWithMenu(chatId, status)
            }
            isCommand(resolved, "/stats") -> {
                apiClient.sendWithMenu(
                    chatId,
                    TelegramStatusMessages.buildStatsMessage(context, loadRepository),
                )
            }
            isCommand(resolved, "/dedup") -> {
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_dedup_running))
                val report = messageParser.runDuplicateAudit(
                    loadRepository,
                    paycheckRepository,
                    dieselRepository,
                )
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
            isCommand(resolved, "/cancel") -> cancel(chatId, prefs, stateMachine)
            isCommand(resolved, "/restore") || TelegramBotFeatures.isRestoreRequest(resolved) -> {
                ImportSessionManager(prefs).endSession(chatId)
                stateMachine.startManualRestore(chatId)
                apiClient.sendWithMenu(chatId, context.getString(R.string.sync_restore_manual_prompt))
            }
        }
    }

    private suspend fun cancel(
        chatId: String,
        prefs: SharedPreferences,
        stateMachine: TelegramStateMachine,
    ) {
        val importSessions = ImportSessionManager(prefs)
        val importCancelled = importSessions.cancelSession(chatId)
        val restoreCancelled = if (stateMachine.isManualRestoreActive(chatId)) {
            stateMachine.clearManualRestore(chatId)
            true
        } else {
            false
        }
        TelegramReceiptConfirmStore(prefs, context).clear(chatId, discardFile = true)
        val incompleteCancelled = TelegramIncompleteLoadStore(prefs).clear(chatId)
        val message = when {
            importCancelled && restoreCancelled ->
                context.getString(R.string.sync_import_and_restore_cancelled)
            importCancelled -> context.getString(R.string.sync_import_cancelled)
            restoreCancelled -> context.getString(R.string.sync_restore_manual_cancelled)
            incompleteCancelled -> context.getString(R.string.sync_receipt_cancelled)
            else -> context.getString(R.string.sync_restore_not_active)
        }
        apiClient.sendWithMenu(chatId, message)
    }

    private fun isCommand(text: String, command: String): Boolean {
        val trimmed = text.trim()
        return trimmed.equals(command, ignoreCase = true) ||
            trimmed.startsWith("$command@", ignoreCase = true) ||
            trimmed.startsWith("$command ", ignoreCase = true)
    }
}
