package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import com.truckerload.R
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.LoadRepository
import com.truckerload.sync.import.ImportDocumentHandler
import com.truckerload.sync.import.ImportSessionManager

/** Documents and photos attached to a Telegram update, including chat-export import. */
internal class TelegramDocumentUpdateHandler(
    private val context: Context,
    private val apiClient: TelegramApiClient,
    private val messageParser: TelegramMessageParser,
    private val ingest: () -> TelegramFileIngestHandler,
) {
    suspend fun handle(update: TelegramUpdate, loadRepository: LoadRepository, prefs: SharedPreferences) {
        val importSessions = ImportSessionManager(prefs)
        val fileId = update.documentFileId
        when {
            fileId != null && isBulkImport(update, importSessions) -> {
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
                if (reply.isNotBlank()) apiClient.sendWithMenu(update.chatId, reply)
            }

            fileId != null && importSessions.isActive(update.chatId) &&
                messageParser.isExportTextDocument(update) -> ingestExportText(update, fileId, loadRepository, prefs)

            fileId != null && messageParser.isExportTextDocument(update) -> {
                val reply = messageParser.importExportDocument(
                    telegramApi = apiClient.api(),
                    update = update,
                    loadRepository = loadRepository,
                )
                apiClient.sendWithMenu(update.chatId, reply)
            }

            else -> ingest().handleMedia(update = update, loadRepository = loadRepository, prefs = prefs)
        }
    }

    private fun isBulkImport(update: TelegramUpdate, importSessions: ImportSessionManager): Boolean {
        val isHtml = ImportDocumentHandler.isSupportedHtml(update.documentFileName, update.documentMimeType)
        val isJson = ImportDocumentHandler.isSupportedJson(update.documentFileName, update.documentMimeType)
        val isChatExport = ImportDocumentHandler.isTelegramExportFileName(update.documentFileName) ||
            ImportDocumentHandler.isResultJsonFileName(update.documentFileName)
        return (isHtml || isJson) && (isChatExport || importSessions.isActive(update.chatId))
    }

    private suspend fun ingestExportText(
        update: TelegramUpdate,
        fileId: String,
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
    ) {
        val declaredSize = update.documentFileSize
        val tooLarge = declaredSize != null && declaredSize > TelegramApi.MAX_DOWNLOAD_BYTES
        if (tooLarge) {
            apiClient.sendWithMenu(
                update.chatId,
                context.getString(
                    R.string.sync_import_file_too_large,
                    (TelegramApi.MAX_DOWNLOAD_BYTES / BYTES_PER_MIB).toInt(),
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
        val reply = messageParser.importMessageHandler(loadRepository, prefs)
            .handle(update.chatId, bytes.toString(Charsets.UTF_8), apiClient.api())
        if (reply.isNotBlank()) apiClient.sendWithMenu(update.chatId, reply)
    }

    private companion object {
        const val BYTES_PER_MIB = 1_048_576
    }
}
