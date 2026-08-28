package com.truckerload.domain.parser

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.utils.LoadDateRepair
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.withReportingWeek

data class ParserConfig(
    val autoUpdate: Boolean = true,
    val priceThresholdPercent: Double = 1.0,
)

sealed class ProcessingResult {
    data object Added : ProcessingResult()
    data class Updated(val changes: List<String>) : ProcessingResult()
    data class Replaced(val reason: String = "") : ProcessingResult()
    data class Skipped(val reason: String) : ProcessingResult()
}

class LoadProcessor(
    private val loadRepository: LoadRepository,
    private val duplicateChecker: DuplicateChecker = DuplicateChecker(loadRepository),
    private val loadUpdater: LoadUpdater = LoadUpdater(loadRepository),
    private val changeDetector: LoadChangeDetector = LoadChangeDetector,
) {
    suspend fun processLoad(
        parsedLoad: Load,
        config: ParserConfig = ParserConfig(),
        messageDateSeconds: Long? = null,
        playFeedback: Boolean = true,
    ): ProcessingResult {
        val incoming = normalizeIncoming(parsedLoad, messageDateSeconds)

        val existingLoad = loadRepository.getByTripId(incoming.tripId)
            ?: when (val duplicate = duplicateChecker.checkDuplicate(incoming)) {
                is DuplicateResult.Found -> duplicate.load
                // FIX: route/stops match with same rate — update existing row, not silent skip
                is DuplicateResult.Suspicious -> duplicate.load
                DuplicateResult.NotFound -> null
            }

        if (existingLoad == null) {
            loadRepository.insertLoad(incoming, playFeedback = playFeedback)
            return ProcessingResult.Added
        }

        val comparison = compareLoads(
            old = existingLoad,
            new = incoming,
            priceThresholdPercent = config.priceThresholdPercent,
        )

        if (comparison.isIdentical()) {
            return ProcessingResult.Skipped("No changes")
        }

        if (!config.autoUpdate) {
            return ProcessingResult.Skipped("Auto-update disabled")
        }

        val changes = changeDetector.detectChanges(existingLoad, incoming)
        return applyUpdate(existingLoad, incoming, changes, comparison)
    }

    suspend fun processLoads(
        parsedLoads: List<Load>,
        config: ParserConfig = ParserConfig(),
        messageDateSeconds: Long? = null,
        playFeedback: Boolean = true,
    ): List<ProcessingResult> =
        loadRepository.runBatchWrite {
            parsedLoads.map { load ->
                processLoad(
                    parsedLoad = load,
                    config = config,
                    messageDateSeconds = messageDateSeconds,
                    playFeedback = playFeedback,
                )
            }
        }

    private suspend fun applyUpdate(
        existingLoad: Load,
        incoming: Load,
        changes: List<String>,
        comparison: LoadComparison,
    ): ProcessingResult {
        val merged = incoming.copy(
            id = existingLoad.id,
            parsedAt = existingLoad.parsedAt,
            isDispute = existingLoad.isDispute,
            disputeResponseDate = existingLoad.disputeResponseDate,
            disputeCompleted = existingLoad.disputeCompleted,
            disputeAmount = existingLoad.disputeAmount,
            disputeApplyToLoad = existingLoad.disputeApplyToLoad,
            disputeAmountApplied = existingLoad.disputeAmountApplied,
            actualFinishDate = existingLoad.actualFinishDate,
        )
        loadUpdater.updateLoad(existingLoad, merged, changes)
        // FIX: Relay resend under a new trip id on the same lane → Replaced, not silent duplicate
        return if (!comparison.tripIdMatch) {
            ProcessingResult.Replaced("tripId: ${existingLoad.tripId} → ${incoming.tripId}")
        } else {
            ProcessingResult.Updated(changes)
        }
    }

    private fun normalizeIncoming(parsedLoad: Load, messageDateSeconds: Long?): Load {
        val now = System.currentTimeMillis()
        // Anchor year/horizon to the Telegram message time (not wall-clock at repair).
        val parsedAt = messageDateSeconds?.times(1000) ?: parsedLoad.parsedAt.takeIf { it > 0L } ?: now
        val messageIso = messageDateSeconds?.let { formatDateFromUnixSeconds(it) }
        val messageYear = messageIso?.take(4)?.toIntOrNull()
        val repaired = LoadDateRepair.repair(
            load = parsedLoad.copy(parsedAt = parsedAt),
            anchorYearHint = messageYear,
            referenceMillis = parsedAt,
        )
        val dated = LoadDateRepair.ensureDate(
            load = repaired,
            referenceMillis = parsedAt,
        ).let { ensured ->
            if (ensured.date.isBlank() && messageIso != null) {
                ensured.copy(date = messageIso)
            } else {
                ensured
            }
        }
        return dated
            .copy(
                parsedAt = parsedAt,
                updatedAt = now,
            )
            .withReportingWeek()
            .withRouteMetrics()
    }
}
