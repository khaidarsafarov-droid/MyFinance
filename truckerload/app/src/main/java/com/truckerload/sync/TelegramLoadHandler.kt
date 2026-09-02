package com.truckerload.sync

import android.content.Context
import com.truckerload.R
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.parser.LoadProcessor
import com.truckerload.domain.parser.MessageParseService
import com.truckerload.domain.parser.ParserConfig
import com.truckerload.domain.parser.ProcessingResult
import com.truckerload.utils.FeedbackManager
import com.truckerload.widget.WidgetRefresh

class TelegramLoadHandler(
    private val context: Context,
    private val loadRepository: LoadRepository,
    private val messageParseService: MessageParseService = MessageParseService(),
    private val loadProcessor: LoadProcessor = LoadProcessor(loadRepository),
    private val settingsDataStore: SettingsDataStore,
) {
    suspend fun handleMessage(
        message: String,
        messageDateSeconds: Long? = null,
        playFeedback: Boolean = true,
    ): String {
        val referenceMillis = messageDateSeconds?.times(1000) ?: System.currentTimeMillis()
        val parsedLoads = messageParseService.parseLoadsFromMessage(message, referenceMillis)
            .getOrNull()
            .orEmpty()
        if (parsedLoads.isEmpty()) {
            return context.getString(R.string.sync_no_new_data)
        }
        return handleLoads(parsedLoads, message, messageDateSeconds, playFeedback)
    }

    suspend fun handleLoads(
        loads: List<Load>,
        rawMessage: String,
        messageDateSeconds: Long? = null,
        playFeedback: Boolean = true,
    ): String {
        val results = processLoadsStructured(loads, rawMessage, messageDateSeconds, playFeedback)
        return loads.mapIndexed { index, load ->
            formatResult(results[index], load.tripId)
        }.joinToString("\n\n")
    }

    suspend fun processLoadsStructured(
        loads: List<Load>,
        rawMessage: String,
        messageDateSeconds: Long? = null,
        playFeedback: Boolean = true,
    ): List<ProcessingResult> {
        if (loads.isEmpty()) return emptyList()
        val config = parserConfig()
        val results = loadProcessor.processLoads(
            parsedLoads = loads.map { it.copy(rawMessage = rawMessage) },
            config = config,
            messageDateSeconds = messageDateSeconds,
            playFeedback = false,
        )
        notifyIfChanged(results, playFeedback)
        return results
    }

    private suspend fun parserConfig(): ParserConfig =
        ParserConfig(
            autoUpdate = settingsDataStore.getParserAutoUpdateOnce(),
            priceThresholdPercent = settingsDataStore.getParserPriceThresholdOnce(),
        )

    private suspend fun notifyIfChanged(results: List<ProcessingResult>, playFeedback: Boolean) {
        val changed = results.any {
            it is ProcessingResult.Added ||
                it is ProcessingResult.Updated ||
                it is ProcessingResult.Replaced
        }
        if (!changed) return
        // Await Room→Glance so the home-screen widget paints before the bot replies.
        WidgetRefresh.refreshAndUpdate(context)
        if (playFeedback) {
            FeedbackManager.onLoadAdded()
        }
    }

    fun formatProcessingResult(result: ProcessingResult, tripId: String): String =
        formatResult(result, tripId)

    private fun formatResult(result: ProcessingResult, tripId: String): String =
        when (result) {
            ProcessingResult.Added ->
                context.getString(R.string.load_added, tripId)
            is ProcessingResult.Updated ->
                context.getString(R.string.load_updated, tripId) + "\n" +
                    context.getString(R.string.changes_detected) + ": " +
                    result.changes.joinToString(", ")
            is ProcessingResult.Replaced ->
                context.getString(R.string.load_replaced, tripId) + "\n" + result.reason
            is ProcessingResult.Skipped ->
                context.getString(R.string.load_skipped, tripId) + "\n" + result.reason
        }
}
