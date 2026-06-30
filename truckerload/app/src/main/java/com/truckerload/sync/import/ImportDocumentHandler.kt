package com.truckerload.sync.import

import android.content.Context
import com.truckerload.R
import com.truckerload.data.remote.TelegramApi
import com.truckerload.domain.import.model.ImportException
import com.truckerload.domain.import.parser.TelegramHtmlExportParser
import com.truckerload.domain.import.parser.TelegramJsonExportParser
import com.truckerload.domain.import.usecase.ImportLoadsUseCase

class ImportDocumentHandler(
    private val context: Context,
    private val importUseCase: ImportLoadsUseCase,
    private val sessionManager: ImportSessionManager,
    private val reportFormatter: ImportReportFormatter,
) {

    suspend fun handle(
        chatId: String,
        fileId: String,
        fileName: String?,
        mimeType: String?,
        fileSize: Long?,
        telegramApi: TelegramApi,
    ): String {
        if (!sessionManager.isActive(chatId)) {
            return context.getString(R.string.sync_import_need_command)
        }

        if (!isSupportedImportFile(fileName, mimeType)) {
            return context.getString(R.string.sync_import_unsupported_file)
        }

        val size = fileSize ?: 0L
        if (size > MAX_FILE_SIZE_BYTES) {
            return context.getString(R.string.sync_import_file_too_large, MAX_FILE_SIZE_MB)
        }

        sessionManager.touchActivity(chatId)

        val displayName = fileName ?: "export"
        var progressMessageId = telegramApi.sendMessageReturningId(
            chatId,
            context.getString(R.string.sync_import_downloading, displayName),
        ).getOrNull()

        val bytes = telegramApi.downloadFile(fileId).getOrElse {
            val msg = context.getString(R.string.sync_import_download_failed)
            progressMessageId?.let { id ->
                telegramApi.editMessageText(chatId, id, msg)
            }
            return msg
        }

        val fileContent = bytes.toString(Charsets.UTF_8)
        val isJson = TelegramJsonExportParser.isTelegramJsonExport(fileContent) ||
            isSupportedJson(fileName, mimeType)
        val isCsv = isSupportedCsv(fileName, mimeType)

        progressMessageId?.let { id ->
            val status = when {
                isJson -> context.getString(R.string.sync_import_json_analyzing)
                isCsv -> context.getString(R.string.sync_import_csv_analyzing)
                TelegramHtmlExportParser.isTelegramExport(fileContent) ->
                    context.getString(R.string.sync_import_telegram_export_detected)
                else -> context.getString(R.string.sync_import_html_analyzing)
            }
            telegramApi.editMessageText(chatId, id, status)
        }

        val result = when {
            isJson -> importUseCase.importJson(fileContent, displayName) { current, total ->
                reportProgress(telegramApi, chatId, progressMessageId, current, total)
            }
            isCsv -> importUseCase.invoke(fileContent) { current, total ->
                reportProgress(telegramApi, chatId, progressMessageId, current, total)
            }
            else -> importUseCase.importHtml(fileContent, displayName) { current, total ->
                reportProgress(telegramApi, chatId, progressMessageId, current, total)
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
                    countAsFile = true,
                )
                progressMessageId?.let { id ->
                    telegramApi.editMessageText(chatId, id, formatted)
                }
                if (progressMessageId != null) "" else formatted
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

    private suspend fun reportProgress(
        telegramApi: TelegramApi,
        chatId: String,
        progressMessageId: Long?,
        current: Int,
        total: Int,
    ) {
        if (total > 10 && current % 5 == 0) {
            val percent = current * 100 / total
            progressMessageId?.let { id ->
                telegramApi.editMessageText(
                    chatId = chatId,
                    messageId = id,
                    text = context.getString(R.string.sync_import_progress, current, total, percent),
                )
            }
        }
    }

    companion object {
        const val MAX_FILE_SIZE_MB = 10
        const val MAX_FILE_SIZE_BYTES = MAX_FILE_SIZE_MB * 1024 * 1024

        val SUPPORTED_HTML_MIME_TYPES = listOf("text/html", "application/xhtml+xml")
        val SUPPORTED_JSON_MIME_TYPES = listOf("application/json", "text/json")
        val HTML_EXTENSIONS = listOf("html", "htm")
        val JSON_EXTENSIONS = listOf("json")

        fun isSupportedImportFile(fileName: String?, mimeType: String?): Boolean =
            isSupportedHtml(fileName, mimeType) ||
                isSupportedJson(fileName, mimeType) ||
                isSupportedCsv(fileName, mimeType)

        fun isSupportedCsv(fileName: String?, mimeType: String?): Boolean {
            val mime = mimeType?.lowercase().orEmpty()
            val mimeOk = mime == "text/csv" || mime == "application/csv" ||
                mime == "text/comma-separated-values" ||
                (mime == "application/octet-stream" && isCsvFileName(fileName))
            return mimeOk || isCsvFileName(fileName)
        }

        private fun isCsvFileName(fileName: String?): Boolean =
            baseFileName(fileName).endsWith(".csv")

        fun isSupportedHtml(fileName: String?, mimeType: String?): Boolean {
            val mime = mimeType?.lowercase().orEmpty()
            val mimeOk = SUPPORTED_HTML_MIME_TYPES.contains(mime) ||
                mime == "application/octet-stream" && isHtmlFileName(fileName)
            return mimeOk || isHtmlFileName(fileName) || isTelegramExportFileName(fileName)
        }

        fun isSupportedJson(fileName: String?, mimeType: String?): Boolean {
            val mime = mimeType?.lowercase().orEmpty()
            val mimeOk = SUPPORTED_JSON_MIME_TYPES.contains(mime) ||
                mime == "application/octet-stream" && isJsonFileName(fileName)
            return mimeOk || isJsonFileName(fileName) || isResultJsonFileName(fileName)
        }

        fun isTelegramExportFileName(fileName: String?): Boolean {
            val base = baseFileName(fileName)
            if (base.isBlank()) return false
            return base.matches(Regex("""messages\d*\.html?"""))
        }

        fun isResultJsonFileName(fileName: String?): Boolean =
            baseFileName(fileName) == "result.json"

        private fun isHtmlFileName(fileName: String?): Boolean {
            val name = baseFileName(fileName)
            return HTML_EXTENSIONS.any { name.endsWith(".$it") }
        }

        private fun isJsonFileName(fileName: String?): Boolean {
            val name = baseFileName(fileName)
            return JSON_EXTENSIONS.any { name.endsWith(".$it") }
        }

        private fun baseFileName(fileName: String?): String =
            fileName?.substringAfterLast('/')?.substringAfterLast('\\')?.lowercase().orEmpty()
    }
}
