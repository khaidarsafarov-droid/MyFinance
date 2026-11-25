package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.StringRes
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.TelegramBotFeatures
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.sync.TelegramChatRestore
import com.truckerload.sync.TelegramStatusMessages
import com.truckerload.sync.import.ImportCommandHandler
import com.truckerload.sync.import.ImportSessionManager
import com.truckerload.utils.LogRedactor
import com.truckerload.utils.runCatchingCancellable
/**
 * Routes Telegram updates / commands to import, restore, or inbound parse paths.
 */
class TelegramUpdateDispatcher(
    private val context: Context,
    private val apiClient: TelegramApiClient,
    private val messageParser: TelegramMessageParser,
) {
    init {
        UserBrokerTemplateStore.publish(context)
    }

    /** One update with the repositories and state it is handled against. */
    private data class UpdateCall(
        val update: TelegramUpdate,
        val loadRepository: LoadRepository,
        val paycheckRepository: PaycheckRepository,
        val dieselRepository: DieselRepository,
        val chatRestore: TelegramChatRestore,
        val prefs: SharedPreferences,
        val stateMachine: TelegramStateMachine,
    )

    suspend fun handleUpdate(
        update: TelegramUpdate,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        chatRestore: TelegramChatRestore,
        prefs: SharedPreferences,
        stateMachine: TelegramStateMachine,
    ) {
        dispatch(
            UpdateCall(
                update = update,
                loadRepository = loadRepository,
                paycheckRepository = paycheckRepository,
                dieselRepository = dieselRepository,
                chatRestore = chatRestore,
                prefs = prefs,
                stateMachine = stateMachine,
            ),
        )
    }

    /** Stages run in a fixed order; a stage that handles the update ends the chain. */
    private suspend fun dispatch(incoming: UpdateCall) {
        val call = recoverCallbackChat(incoming)
        val gate = authorizeChat(call.update)
        if (!rejectIfNotAccepted(call, gate)) {
            acknowledgeAndStore(call)
            routeAccepted(call, gate)
        }
    }

    private suspend fun routeAccepted(call: UpdateCall, gate: ChatGate) {
        val update = call.update
        val callbackQueryId = update.callbackQueryId
        if (update.isCallbackQuery && callbackQueryId != null) {
            handleCallbackQuery(call, gate, callbackQueryId)
        } else {
            val rawText = normalizedText(update.text)
            val handled = handleTextCommand(call, rawText) ||
                handleAttachment(call, rawText) ||
                handleActiveSession(call, rawText)
            if (!handled) {
                ingest().applyText(
                    chatId = update.chatId,
                    text = rawText,
                    fileName = null,
                    messageDateSeconds = update.messageDateSeconds,
                    loadRepository = call.loadRepository,
                    prefs = call.prefs,
                )
            }
        }
    }

    /** A callback without a chat id is attributed to the only chat with a pending receipt. */
    private fun recoverCallbackChat(call: UpdateCall): UpdateCall {
        val update = call.update
        val needsRecovery = update.isCallbackQuery && update.chatId.isBlank()
        val recovered = if (needsRecovery) TelegramReceiptConfirmStore(call.prefs, context).soleChatId() else null
        return if (recovered.isNullOrBlank()) call else call.copy(update = update.copy(chatId = recovered))
    }

    /**
     * Only the paired private chat may mutate journal data or hijack exports.
     * Unpaired load texts are stored and imported after /start so the offset can move.
     */
    private suspend fun rejectIfNotAccepted(call: UpdateCall, gate: ChatGate): Boolean {
        val update = call.update
        return when (gate) {
            ChatGate.Foreign -> {
                answerCallback(update, context.getString(R.string.sync_chat_not_authorized))
                true
            }

            ChatGate.Unpaired -> {
                if (!update.isCallbackQuery && call.chatRestore.isRestorableDataMessage(update.text)) {
                    call.chatRestore.persistIncoming(update)
                    TelegramPairingReplay.remember(call.prefs, update.updateId)
                }
                val hint = context.getString(R.string.sync_chat_pair_required)
                if (update.isCallbackQuery) {
                    answerCallback(update, hint)
                } else if (TelegramPairHint.markIfFirst(call.prefs, update.chatId)) {
                    runCatchingCancellable { apiClient.sendMessage(update.chatId, hint) }
                }
                true
            }

            ChatGate.JustPaired, ChatGate.Accepted -> false
        }
    }

    private suspend fun acknowledgeAndStore(call: UpdateCall) {
        val update = call.update
        if (shouldAckImmediately(update)) {
            acknowledgeNow(update.chatId)
        }
        if (!update.isCallbackQuery && update.text.isNotBlank()) {
            call.chatRestore.persistIncoming(update)
        }
    }

    private suspend fun handleCallbackQuery(call: UpdateCall, gate: ChatGate, callbackQueryId: String) {
        val update = call.update
        val action = TelegramReceiptChoice.receiptActionData(
            raw = update.text,
            yes = context.getString(R.string.sync_paycheck_confirm_yes),
            typeOwn = context.getString(R.string.sync_amount_type_own),
            cancel = context.getString(R.string.common_cancel),
        )
        if (action != null) {
            ingest().handleCallback(
                data = action,
                chatId = update.chatId,
                loadRepository = call.loadRepository,
                paycheckRepository = call.paycheckRepository,
                dieselRepository = call.dieselRepository,
                prefs = call.prefs,
                callbackQueryId = callbackQueryId,
            )
            return
        }
        val cmd = TelegramBotFeatures.menuButtonToCommand(update.text)
            ?: update.text.takeIf { it.startsWith("cmd:") }
                ?.removePrefix("cmd:")
                ?.let { "/$it" }
        apiClient.answerCallbackQuery(callbackQueryId, "OK").onFailure { e ->
            Log.w(TAG, "answerCallbackQuery: ${LogRedactor.redact(e.message)}")
        }
        if (cmd == null) {
            if (TelegramReceiptConfirmStore(call.prefs, context).load(update.chatId) != null) {
                apiClient.sendWithMenu(update.chatId, context.getString(R.string.sync_amount_type_prompt))
            }
            return
        }
        handleCommand(
            cmd,
            update.chatId,
            call.loadRepository,
            call.paycheckRepository,
            call.dieselRepository,
            call.prefs,
            call.stateMachine,
        )
        if (gate == ChatGate.JustPaired) replayUnpaired(update.chatId, call)
    }

    private fun normalizedText(text: String): String {
        var rawText = text.trim()
        if (TelegramBotFeatures.isMenuButtonText(rawText)) {
            rawText = TelegramBotFeatures.menuButtonToCommand(rawText) ?: rawText
        }
        return TelegramBotFeatures.aliasCommand(rawText)
    }

    /** Slash commands, load edits and broker teaching. True when one of them answered. */
    private suspend fun handleTextCommand(call: UpdateCall, rawText: String): Boolean {
        val chatId = call.update.chatId
        return TelegramLoadEdits(context, apiClient).handle(rawText, chatId, call.loadRepository) ||
            TelegramBrokerTeach(context, apiClient).handleCommand(rawText, chatId, call.prefs) ||
            handleSlashCommand(call, rawText)
    }

    private suspend fun handleSlashCommand(call: UpdateCall, rawText: String): Boolean {
        val update = call.update
        return when {
            isCommand(rawText, "/start") -> {
                apiClient.sendWithMenu(update.chatId, context.getString(R.string.sync_welcome))
                replayUnpaired(update.chatId, call)
                true
            }

            isCommand(rawText, "/help") -> sendText(update.chatId, R.string.sync_help)

            isCommand(rawText, "/help_load") -> sendText(update.chatId, R.string.sync_help_load)

            isCommand(rawText, "/help_pay") -> sendText(update.chatId, R.string.sync_help_pay)

            isCommand(rawText, "/status") -> {
                val status = buildStatusMessage(call.loadRepository, call.paycheckRepository, call.dieselRepository)
                apiClient.sendWithMenu(update.chatId, status)
                true
            }

            isCommand(rawText, "/stats") -> {
                apiClient.sendWithMenu(update.chatId, buildStatsMessage(call.loadRepository))
                true
            }

            isCommand(rawText, "/dedup") -> {
                runDuplicateAudit(call)
                true
            }

            isCommand(rawText, "/cancel") -> {
                cancelPendingWork(call)
                true
            }

            isCommand(rawText, "/import") -> {
                startImport(call)
                true
            }

            TelegramBotFeatures.isRestoreRequest(rawText) || isCommand(rawText, "/restore") -> {
                ImportSessionManager(call.prefs).endSession(update.chatId)
                call.stateMachine.startManualRestore(update.chatId)
                sendText(update.chatId, R.string.sync_restore_manual_prompt)
            }

            else -> false
        }
    }

    private suspend fun sendText(chatId: String, @StringRes resId: Int): Boolean {
        apiClient.sendWithMenu(chatId, context.getString(resId))
        return true
    }

    private suspend fun runDuplicateAudit(call: UpdateCall) {
        val chatId = call.update.chatId
        apiClient.sendWithMenu(chatId, context.getString(R.string.sync_dedup_running))
        val report = messageParser.runDuplicateAudit(
            call.loadRepository,
            call.paycheckRepository,
            call.dieselRepository,
        )
        apiClient.sendWithMenu(chatId, report)
    }

    private suspend fun cancelPendingWork(call: UpdateCall) {
        val chatId = call.update.chatId
        val importCancelled = ImportSessionManager(call.prefs).cancelSession(chatId)
        val restoreCancelled = call.stateMachine.isManualRestoreActive(chatId)
        if (restoreCancelled) call.stateMachine.clearManualRestore(chatId)
        TelegramReceiptConfirmStore(call.prefs, context).clear(chatId, discardFile = true)
        val incompleteCancelled = TelegramIncompleteLoadStore(call.prefs).clear(chatId)
        val teachCancelled = TelegramBrokerTeach(context, apiClient).clear(call.prefs, chatId)
        val message = when {
            importCancelled && restoreCancelled ->
                context.getString(R.string.sync_import_and_restore_cancelled)

            importCancelled -> context.getString(R.string.sync_import_cancelled)

            restoreCancelled -> context.getString(R.string.sync_restore_manual_cancelled)

            incompleteCancelled || teachCancelled -> context.getString(R.string.sync_receipt_cancelled)

            else -> context.getString(R.string.sync_restore_not_active)
        }
        apiClient.sendWithMenu(chatId, message)
    }

    private suspend fun startImport(call: UpdateCall) {
        val chatId = call.update.chatId
        val importSessions = ImportSessionManager(call.prefs)
        if (call.stateMachine.isManualRestoreActive(chatId)) {
            call.stateMachine.clearManualRestore(chatId)
        }
        ImportCommandHandler(context, importSessions)
            .startImport(chatId) { id -> call.stateMachine.clearManualRestore(id) }
        ImportCommandHandler(context, importSessions).sendPrompt(chatId, apiClient.api())
    }

    /** Photos and documents, and empty text. True when handled. */
    private suspend fun handleAttachment(call: UpdateCall, rawText: String): Boolean {
        val update = call.update
        return when {
            update.photoFileId != null || update.documentFileId != null -> {
                documents().handle(update, call.loadRepository, call.prefs)
                true
            }

            rawText.isBlank() -> {
                apiClient.sendMessage(update.chatId, context.getString(R.string.sync_no_new_data))
                true
            }

            else -> false
        }
    }

    /** Plain text while an import, manual restore, pending reply, amount, teach or incomplete load is open. */
    private suspend fun handleActiveSession(call: UpdateCall, rawText: String): Boolean {
        val update = call.update
        val chatId = update.chatId
        val prefs = call.prefs
        return when {
            ImportSessionManager(prefs).isActive(chatId) -> {
                val reply = messageParser.importMessageHandler(call.loadRepository, prefs)
                    .handle(chatId, rawText, apiClient.api())
                if (reply.isNotBlank()) apiClient.sendWithMenu(chatId, reply)
                true
            }

            call.stateMachine.isManualRestoreActive(chatId) -> {
                call.stateMachine.touchManualRestore(chatId)
                val reply = messageParser.processManualRestoreMessage(
                    text = rawText,
                    messageDateSeconds = update.messageDateSeconds,
                    chatId = chatId,
                    loadRepository = call.loadRepository,
                    stateMachine = call.stateMachine,
                )
                apiClient.sendWithMenu(chatId, reply)
                true
            }

            else -> handlePendingInput(call, rawText)
        }
    }

    private suspend fun handlePendingInput(call: UpdateCall, rawText: String): Boolean {
        val chatId = call.update.chatId
        val prefs = call.prefs
        val ingest = ingest()
        return ingest.tryHandlePendingReply(
            chatId = chatId,
            text = rawText,
            loadRepository = call.loadRepository,
            paycheckRepository = call.paycheckRepository,
            dieselRepository = call.dieselRepository,
            prefs = prefs,
        ) ||
            ingest.tryHandleTypedAmount(
                chatId = chatId,
                text = rawText,
                loadRepository = call.loadRepository,
                paycheckRepository = call.paycheckRepository,
                dieselRepository = call.dieselRepository,
                prefs = prefs,
            ) ||
            TelegramBrokerTeach(context, apiClient).continueSession(
                text = rawText,
                chatId = chatId,
                prefs = prefs,
            ) ||
            ingest.tryCompleteIncompleteLoad(
                chatId = chatId,
                text = rawText,
                loadRepository = call.loadRepository,
                prefs = prefs,
            )
    }

    private suspend fun replayUnpaired(chatId: String, call: UpdateCall) {
        val imported = TelegramPairingReplay.importPending(
            chatId = chatId,
            chatRestore = call.chatRestore,
            messageParser = messageParser,
            loadRepository = call.loadRepository,
            paycheckRepository = call.paycheckRepository,
            dieselRepository = call.dieselRepository,
            prefs = call.prefs,
        )
        if (imported > 0) {
            apiClient.sendWithMenu(
                chatId,
                context.getString(R.string.sync_unpaired_replayed, imported),
            )
        }
    }

    private fun shouldAckImmediately(update: TelegramUpdate): Boolean {
        if (update.isCallbackQuery) return false
        if (update.photoFileId != null || update.documentFileId != null) return true
        val text = update.text.trim()
        if (text.isBlank() || text.startsWith("/")) return false
        if (TelegramBotFeatures.isMenuButtonText(text)) return false
        if (TelegramBotFeatures.isRestoreRequest(text)) return false
        return true
    }

    private suspend fun acknowledgeNow(chatId: String) {
        apiClient.sendChatAction(chatId)
        apiClient.sendMessage(chatId, context.getString(R.string.sync_processing))
            .onFailure { e -> Log.w(TAG, "ack failed: ${LogRedactor.redact(e.message)}") }
    }

    private fun ingest(): TelegramFileIngestHandler = TelegramFileIngestHandler(context, apiClient, messageParser)
    private fun documents() = TelegramDocumentUpdateHandler(context, apiClient, messageParser, ::ingest)

    suspend fun handleCommand(
        command: String,
        chatId: String,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
        stateMachine: TelegramStateMachine,
    ) {
        TelegramBotCommands(context, apiClient, messageParser).handle(
            command = command,
            chatId = chatId,
            loadRepository = loadRepository,
            paycheckRepository = paycheckRepository,
            dieselRepository = dieselRepository,
            prefs = prefs,
            stateMachine = stateMachine,
        )
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
     * Bound chat: only that chat id is accepted.
     * Unbound private `/start` (the Telegram Start button, with or without a code) pairs once.
     * Never overwrite a bound chat id from inbound messages.
     */
    private suspend fun answerCallback(update: TelegramUpdate, text: String) {
        val id = update.callbackQueryId ?: return
        apiClient.answerCallbackQuery(id, text).onFailure { e ->
            Log.w(TAG, "answerCallbackQuery: ${LogRedactor.redact(e.message)}")
        }
    }

    private suspend fun authorizeChat(update: TelegramUpdate): ChatGate {
        val chatIdLong = update.chatId.toLongOrNull()
        if (chatIdLong == null) {
            Log.w(TAG, "Reject update with non-numeric chatId")
            return ChatGate.Foreign
        }
        val settings = SettingsDataStore(context)
        val bound = settings.getTelegramChatIdOnce()
        if (bound != null) {
            if (bound == chatIdLong) return ChatGate.Accepted
            runCatchingCancellable {
                apiClient.sendMessage(
                    update.chatId,
                    context.getString(R.string.sync_chat_not_authorized),
                )
            }
            Log.w(TAG, "Rejected unauthorized chatId=$chatIdLong (bound=$bound)")
            return ChatGate.Foreign
        }
        if (shouldPairOnStart(
                text = update.text,
                isCallbackQuery = update.isCallbackQuery,
                chatType = update.chatType,
            )
        ) {
            telegramStartPayload(update.text)?.let { settings.consumeTelegramPairingCode(it) }
            settings.saveTelegramChatId(chatIdLong)
            Log.i(TAG, "Paired Telegram chatId=$chatIdLong via /start")
            return ChatGate.JustPaired
        }
        Log.w(TAG, "Unpaired chatId=$chatIdLong — /start pairs this chat")
        return ChatGate.Unpaired
    }

    private enum class ChatGate { Accepted, JustPaired, Foreign, Unpaired }

    private companion object {
        const val TAG = "TelegramBotSync"
    }
}
