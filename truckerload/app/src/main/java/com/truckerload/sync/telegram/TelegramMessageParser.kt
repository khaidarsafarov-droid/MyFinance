package com.truckerload.sync.telegram

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.remote.TelegramUpdate
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadImportRepositoryImpl
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.import.usecase.ImportLoadsUseCase
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.parser.LoadProcessor
import com.truckerload.domain.parser.MessageClassifier
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.domain.parser.ParserConfig
import com.truckerload.domain.parser.ProcessingResult
import com.truckerload.sync.DuplicateAuditRunner
import com.truckerload.sync.TelegramLoadHandler
import com.truckerload.sync.import.ImportDocumentHandler
import com.truckerload.sync.import.ImportHandlerSupport
import com.truckerload.sync.import.ImportMessageHandler
import com.truckerload.sync.import.ImportReportFormatter
import com.truckerload.sync.import.ImportSessionManager
import com.truckerload.utils.FeedbackManager
import com.truckerload.utils.LoadImporter
import com.truckerload.utils.LogRedactor
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekRange
import com.truckerload.widget.WidgetDataUpdater
import com.truckerload.widget.WidgetUpdateWorker
import java.util.Locale

/**
 * Parses inbound Telegram text/documents into loads / paychecks / diesel and writes Room.
 * Low-level regex parsing stays in [MessageParseService].
 */
class TelegramMessageParser(
    private val context: Context,
    private val messageParseService: MessageParseService = MessageParseService(),
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context),
) {
    suspend fun importMessageHandler(
        loadRepository: LoadRepository,
        prefs: SharedPreferences,
    ): ImportMessageHandler {
        val db = AppDatabase.getInstanceForActiveUser(context)
            ?: error("No active user for Telegram import")
        val importRepo = LoadImportRepositoryImpl(loadRepository, db.loadDao())
        val config = parserConfig()
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
        val config = parserConfig()
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

    suspend fun parserConfig(): ParserConfig =
        ParserConfig(
            autoUpdate = settingsDataStore.getParserAutoUpdateOnce(),
            priceThresholdPercent = settingsDataStore.getParserPriceThresholdOnce(),
        )

    fun telegramLoadHandler(loadRepository: LoadRepository): TelegramLoadHandler =
        TelegramLoadHandler(
            context = context,
            loadRepository = loadRepository,
            messageParseService = messageParseService,
            settingsDataStore = settingsDataStore,
        )

    suspend fun processManualRestoreMessage(
        text: String,
        messageDateSeconds: Long?,
        chatId: String,
        loadRepository: LoadRepository,
        stateMachine: TelegramStateMachine,
    ): String {
        if (!MessageClassifier.isLoadLike(text)) {
            return context.getString(R.string.sync_restore_manual_not_load)
        }

        val parsed = messageParseService.parseLoadsFromMessage(
            text,
            messageDateSeconds?.times(1000) ?: System.currentTimeMillis(),
        ).getOrNull().orEmpty()
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
                    stateMachine.incrementManualRestoreCount(chatId)
                    replies.add(
                        context.getString(
                            R.string.sync_restore_manual_added,
                            load.tripId,
                            load.totalMiles,
                            String.format(Locale.US, "%,.2f", load.totalRate),
                        ),
                    )
                }
                is ProcessingResult.Updated,
                is ProcessingResult.Replaced -> {
                    addedCount++
                    stateMachine.incrementManualRestoreCount(chatId)
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

    suspend fun processMessage(
        text: String,
        messageDateSeconds: Long?,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences,
    ): String {
        val referenceMillis = messageDateSeconds?.times(1000) ?: System.currentTimeMillis()
        messageParseService.parseLoadsFromMessage(text, referenceMillis)
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
                val ingest = TelegramJournalIngest(paycheckRepository, dieselRepository)
                return when (
                    val outcome = ingest.insertPaycheck(
                        netAmount = r.netAmount,
                        grossAmount = r.grossAmount,
                        driverName = r.driverName,
                        weekStartDateHint = r.weekStartDate,
                        messageDateSeconds = messageDateSeconds,
                        rawText = text,
                    )
                ) {
                    TelegramJournalIngest.PaycheckOutcome.InvalidAmount ->
                        context.getString(R.string.sync_paycheck_not_found)
                    is TelegramJournalIngest.PaycheckOutcome.AlreadyExists ->
                        context.getString(R.string.sync_paycheck_exists, outcome.weekNumber)
                    is TelegramJournalIngest.PaycheckOutcome.Inserted ->
                        context.getString(
                            R.string.sync_last_paycheck,
                            String.format(Locale.US, "%,.2f", outcome.netAmount),
                            outcome.weekNumber,
                        )
                }
            }

        messageParseService.parseDieselFromText(text)
            .onSuccess { r ->
                val ingest = TelegramJournalIngest(paycheckRepository, dieselRepository)
                return when (
                    val outcome = ingest.insertDiesel(
                        totalAmount = r.totalAmount,
                        gallons = r.gallons,
                        pricePerGallon = r.pricePerGallon,
                        location = r.location,
                        dateHint = r.date,
                        messageDateSeconds = messageDateSeconds,
                        rawText = text,
                    )
                ) {
                    TelegramJournalIngest.DieselOutcome.InvalidAmount ->
                        context.getString(R.string.sync_diesel_not_found)
                    TelegramJournalIngest.DieselOutcome.Duplicate ->
                        context.getString(R.string.sync_duplicate_diesel)
                    is TelegramJournalIngest.DieselOutcome.Inserted ->
                        context.getString(
                            R.string.sync_last_diesel,
                            String.format(Locale.US, "%,.2f", outcome.totalAmount),
                            outcome.weekNumber,
                        )
                }
            }

        return context.getString(R.string.sync_parse_failed)
    }

    suspend fun importExportDocument(
        telegramApi: TelegramApi,
        update: TelegramUpdate,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
    ): String {
        val fileId = update.documentFileId ?: return context.getString(R.string.sync_doc_not_supported)
        val declaredSize = update.documentFileSize
        if (declaredSize != null && declaredSize > TelegramApi.MAX_DOWNLOAD_BYTES) {
            return context.getString(
                R.string.sync_import_file_too_large,
                (TelegramApi.MAX_DOWNLOAD_BYTES / (1024 * 1024)).toInt(),
            )
        }
        val bytes = telegramApi.downloadFile(fileId).getOrElse { e ->
            Log.e(TAG, "download export file failed: ${LogRedactor.redact(e.message)}", e)
            return context.getString(R.string.restore_error, e.message.orEmpty())
        }
        val text = bytes.toString(Charsets.UTF_8)
        val config = parserConfig()
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

    fun isExportTextDocument(update: TelegramUpdate): Boolean {
        val name = update.documentFileName?.lowercase(Locale.US).orEmpty()
        val mime = update.documentMimeType?.lowercase(Locale.US).orEmpty()
        return name.endsWith(".txt") ||
            mime == "text/plain" ||
            name.contains("trucklog_export") ||
            name.contains("truckerload_export")
    }

    private companion object {
        const val TAG = "TelegramBotSync"
    }
}
