package com.truckerload.domain.import.usecase

import android.util.Log
import com.truckerload.domain.import.ImportTripDedup
import com.truckerload.domain.import.LoadValidator
import com.truckerload.domain.import.model.ImportException
import com.truckerload.domain.import.model.ImportReport
import com.truckerload.domain.import.model.ImportResult
import com.truckerload.domain.import.model.ParsedLoad
import com.truckerload.domain.import.model.SkipReason
import com.truckerload.domain.import.parser.HtmlLoadParser
import com.truckerload.domain.import.parser.MessageTypeDetector
import com.truckerload.domain.import.parser.ParserFactory
import com.truckerload.domain.import.parser.TelegramHtmlExportParser
import com.truckerload.domain.import.parser.TelegramJsonExportParser
import com.truckerload.domain.import.repository.LoadImportRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadProcessor
import com.truckerload.domain.parser.ParserConfig
import com.truckerload.domain.parser.ProcessingResult
import com.truckerload.utils.CrashReporting
import com.truckerload.utils.FeedbackManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class ImportLoadsUseCase(
    private val parserFactory: ParserFactory = ParserFactory(),
    private val loadRepository: LoadImportRepository,
    private val loadProcessor: LoadProcessor? = null,
    private val parserConfig: ParserConfig = ParserConfig(),
    private val validator: LoadValidator = LoadValidator(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    companion object {
        private const val TAG = "ImportLoads"
        const val MAX_LOADS_PER_IMPORT = 100
        const val MAX_LOADS_PER_JSON_IMPORT = 500
        const val IMPORT_TIMEOUT_MS = 30_000L
        const val JSON_IMPORT_TIMEOUT_MS = 120_000L
    }

    suspend operator fun invoke(
        rawInput: String,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<ImportReport> = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(IMPORT_TIMEOUT_MS) {
                val messageType = MessageTypeDetector.detect(rawInput)
                val parser = parserFactory.getParser(messageType)
                val parsedLoads = dedupeKeepingLatest(parser.parse(rawInput))
                processParsedLoads(parsedLoads, startTime, onProgress)
            }
        } catch (e: TimeoutCancellationException) { android.util.Log.w("TL", "import timeout", e);
            Result.failure(ImportException.Timeout(IMPORT_TIMEOUT_MS))
        }
    }

    suspend fun importHtml(
        htmlContent: String,
        fileName: String?,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<ImportReport> = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(IMPORT_TIMEOUT_MS) {
                val parser = if (TelegramHtmlExportParser.isTelegramExport(htmlContent)) {
                    TelegramHtmlExportParser()
                } else {
                    HtmlLoadParser()
                }
                val parsedLoads = dedupeKeepingLatest(parser.parse(htmlContent))
                processParsedLoads(
                    parsedLoads = parsedLoads,
                    startTime = startTime,
                    onProgress = onProgress,
                    filesProcessed = 1,
                    fileName = fileName,
                )
            }
        } catch (e: TimeoutCancellationException) { android.util.Log.w("TL", "import timeout", e);
            Result.failure(ImportException.Timeout(IMPORT_TIMEOUT_MS))
        }
    }

    suspend fun importJson(
        jsonContent: String,
        fileName: String?,
        onProgress: suspend (current: Int, total: Int) -> Unit = { _, _ -> },
    ): Result<ImportReport> = withContext(dispatcher) {
        val startTime = System.currentTimeMillis()
        try {
            withTimeout(JSON_IMPORT_TIMEOUT_MS) {
                if (!TelegramJsonExportParser.isTelegramJsonExport(jsonContent)) {
                    return@withTimeout Result.failure(
                        IllegalArgumentException("Not a Telegram JSON export"),
                    )
                }
                val parsedLoads = dedupeKeepingLatest(
                    TelegramJsonExportParser().parse(jsonContent),
                )
                processParsedLoads(
                    parsedLoads = parsedLoads,
                    startTime = startTime,
                    onProgress = onProgress,
                    filesProcessed = 1,
                    fileName = fileName,
                    maxLoads = MAX_LOADS_PER_JSON_IMPORT,
                )
            }
        } catch (e: TimeoutCancellationException) { android.util.Log.w("TL", "import timeout", e);
            Result.failure(ImportException.Timeout(JSON_IMPORT_TIMEOUT_MS))
        }
    }

    private fun dedupeKeepingLatest(raw: List<Load>): List<Load> {
        val parsedLoads = ImportTripDedup.keepLatestByTripId(raw)
        val dropped = raw.size - parsedLoads.size
        if (dropped > 0) {
            // Stage3 monitoring breadcrumb: older Trip ID revisions discarded
            Log.i(TAG, "import dedup dropped $dropped older Trip ID revision(s)")
            CrashReporting.setCustomKey("import_dedup_dropped", dropped.toLong())
        }
        return parsedLoads
    }

    private suspend fun processParsedLoads(
        parsedLoads: List<Load>,
        startTime: Long,
        onProgress: suspend (current: Int, total: Int) -> Unit,
        filesProcessed: Int = 0,
        fileName: String? = null,
        maxLoads: Int = MAX_LOADS_PER_IMPORT,
    ): Result<ImportReport> {
        if (parsedLoads.isEmpty()) {
            return Result.failure(IllegalArgumentException("No loads found in message"))
        }

        if (parsedLoads.size > maxLoads) {
            return Result.failure(
                ImportException.TooManyLoads(parsedLoads.size, maxLoads),
            )
        }

        val results = mutableListOf<ImportResult>()
        val total = parsedLoads.size

        parsedLoads.forEachIndexed { index, load ->
            onProgress(index + 1, total)

            val validation = validator.validate(load)
            if (!validation.isValid) {
                results.add(
                    ImportResult.Failed(
                        tripId = load.tripId,
                        rawBlock = load.rawMessage.take(200),
                        error = validation.errors.joinToString("; "),
                    )
                )
                return@forEachIndexed
            }

            val result = if (loadProcessor != null) {
                processWithProcessor(load)
            } else {
                processLegacy(load)
            }
            results.add(result)
        }

        if (results.any {
                it is ImportResult.Added ||
                    it is ImportResult.Updated ||
                    it is ImportResult.Replaced
            }
        ) {
            FeedbackManager.onLoadAdded()
        }

        return Result.success(
            buildReport(
                results = results,
                durationMs = System.currentTimeMillis() - startTime,
                filesProcessed = filesProcessed,
                fileName = fileName,
            )
        )
    }

    private suspend fun processWithProcessor(load: Load): ImportResult {
        val processor = loadProcessor
            ?: return processLegacy(load)
        return when (
            val processing = processor.processLoad(
                parsedLoad = load,
                config = parserConfig,
                playFeedback = false,
            )
        ) {
            ProcessingResult.Added -> ImportResult.Added(ParsedLoad.from(load))
            is ProcessingResult.Updated -> ImportResult.Updated(load.tripId, processing.changes)
            is ProcessingResult.Replaced -> ImportResult.Replaced(load.tripId)
            is ProcessingResult.Skipped -> ImportResult.Skipped(load.tripId, mapSkipReason(processing.reason))
        }
    }

    private suspend fun processLegacy(load: Load): ImportResult =
        when {
            loadRepository.exists(load.tripId) -> {
                ImportResult.Skipped(load.tripId, SkipReason.DUPLICATE)
            }
            else -> {
                loadRepository.insertLoad(load, playFeedback = false)
                ImportResult.Added(ParsedLoad.from(load))
            }
        }

    private fun mapSkipReason(reason: String): SkipReason = when {
        reason.contains("Авто-обновление", ignoreCase = true) ||
            reason.contains("Auto-update disabled", ignoreCase = true) -> SkipReason.AUTO_UPDATE_DISABLED
        reason.contains("Изменений нет", ignoreCase = true) ||
            reason.contains("No changes", ignoreCase = true) -> SkipReason.NO_CHANGES
        reason.contains("Дубликат", ignoreCase = true) ||
            reason.contains("Duplicate", ignoreCase = true) -> SkipReason.SUSPICIOUS_DUPLICATE
        else -> SkipReason.DUPLICATE
    }

    private fun buildReport(
        results: List<ImportResult>,
        durationMs: Long,
        filesProcessed: Int = 0,
        fileName: String? = null,
    ): ImportReport {
        val added = results.filterIsInstance<ImportResult.Added>()
        val updated = results.filterIsInstance<ImportResult.Updated>()
        val replaced = results.filterIsInstance<ImportResult.Replaced>()
        val skipped = results.filterIsInstance<ImportResult.Skipped>()
        val failed = results.filterIsInstance<ImportResult.Failed>()

        return ImportReport(
            totalFound = results.size,
            added = added.size,
            updated = updated.size,
            replaced = replaced.size,
            skipped = skipped.size,
            failed = failed.size,
            addedLoads = added.map { it.load },
            skippedLoads = skipped.map { it.tripId to it.reason },
            failedBlocks = failed.map { (it.tripId ?: "unknown") to it.error },
            durationMs = durationMs,
            filesProcessed = filesProcessed,
            fileName = fileName,
        )
    }
}
