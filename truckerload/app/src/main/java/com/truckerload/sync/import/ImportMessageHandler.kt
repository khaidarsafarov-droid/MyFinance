package com.truckerload.sync.import

import android.content.Context
import com.truckerload.R
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.data.remote.TelegramApi
import com.truckerload.domain.import.model.ImportException
import com.truckerload.domain.import.usecase.ImportLoadsUseCase

class ImportMessageHandler(
    private val context: Context,
    private val importUseCase: ImportLoadsUseCase,
    private val sessionManager: ImportSessionManager,
    private val reportFormatter: ImportReportFormatter,
    private val loadRepository: LoadRepository,
    private val paycheckRepository: PaycheckRepository,
    private val dieselRepository: DieselRepository,
) {

    suspend fun handle(
        chatId: String,
        rawText: String,
        telegramApi: TelegramApi,
    ): String {
        if (!sessionManager.isActive(chatId)) {
            return context.getString(R.string.sync_import_need_command)
        }

        if (rawText.isBlank()) {
            return context.getString(R.string.sync_import_empty_message)
        }

        sessionManager.touchActivity(chatId)

        val showProgress = rawText.length > 500
        var progressMessageId: Long? = null
        if (showProgress) {
            progressMessageId = telegramApi.sendMessageReturningId(
                chatId,
                context.getString(R.string.sync_import_analyzing),
            ).getOrNull()
        }

        val result = importUseCase(rawText) { current, total ->
            if (total > 10 && current % 5 == 0) {
                val percent = current * 100 / total
                progressMessageId?.let { messageId ->
                    telegramApi.editMessageText(
                        chatId = chatId,
                        messageId = messageId,
                        text = context.getString(R.string.sync_import_progress, current, total, percent),
                    )
                }
            }
        }

        return result.fold(
            onSuccess = { report ->
                if (report.added > 0) {
                    ImportHandlerSupport.refreshWidgets(context)
                }
                val formatted = ImportHandlerSupport.formatSuccess(
                    context = context,
                    reportFormatter = reportFormatter,
                    sessionManager = sessionManager,
                    chatId = chatId,
                    report = report,
                )
                val dedupSection = ImportHandlerSupport.runPostImportDedup(
                    context = context,
                    loadRepository = loadRepository,
                    paycheckRepository = paycheckRepository,
                    dieselRepository = dieselRepository,
                )
                val message = if (dedupSection.isBlank()) formatted else "$formatted\n\n$dedupSection"
                progressMessageId?.let { id ->
                    telegramApi.editMessageText(chatId, id, message)
                }
                if (progressMessageId != null) "" else message
            },
            onFailure = { error ->
                val errorText = when (error) {
                    is ImportException.TooManyLoads ->
                        context.getString(R.string.sync_import_too_many, error.found, error.max)
                    is ImportException.Timeout ->
                        context.getString(R.string.sync_import_timeout, error.timeoutMs / 1000)
                    is IllegalArgumentException ->
                        context.getString(R.string.sync_import_nothing_found)
                    else -> context.getString(R.string.sync_import_error, error.message ?: "?")
                }
                progressMessageId?.let { id ->
                    telegramApi.editMessageText(chatId, id, errorText)
                }
                if (progressMessageId != null) "" else errorText
            },
        )
    }
}
