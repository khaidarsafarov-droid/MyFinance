package com.truckerload.domain.parser

import com.truckerload.data.repository.LoadRepository
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.withRouteMetrics
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
                is DuplicateResult.Suspicious -> {
                    return ProcessingResult.Skipped(
                        "Дубликат (${duplicate.reason}): ${duplicate.load.tripId}",
                    )
                }
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
            return ProcessingResult.Skipped("Изменений нет")
        }

        if (!config.autoUpdate) {
            return ProcessingResult.Skipped("Авто-обновление отключено")
        }

        val changes = changeDetector.detectChanges(existingLoad, incoming)

        return when {
            comparison.hasMinorChanges() -> {
                loadUpdater.updateLoad(existingLoad, incoming, changes)
                ProcessingResult.Updated(changes)
            }
            comparison.hasMajorChanges() -> {
                loadRepository.deleteLoad(existingLoad.id)
                val replacement = incoming.copy(
                    id = incoming.id.ifBlank { existingLoad.id },
                    parsedAt = existingLoad.parsedAt,
                )
                loadRepository.insertLoad(replacement, playFeedback = playFeedback)
                ProcessingResult.Replaced("Старый удалён, новый добавлен")
            }
            else -> {
                loadUpdater.updateLoad(existingLoad, incoming, changes)
                ProcessingResult.Updated(changes)
            }
        }
    }

    suspend fun processLoads(
        parsedLoads: List<Load>,
        config: ParserConfig = ParserConfig(),
        messageDateSeconds: Long? = null,
        playFeedback: Boolean = true,
    ): List<ProcessingResult> =
        parsedLoads.map { load ->
            processLoad(
                parsedLoad = load,
                config = config,
                messageDateSeconds = messageDateSeconds,
                playFeedback = playFeedback,
            )
        }

    private fun normalizeIncoming(parsedLoad: Load, messageDateSeconds: Long?): Load {
        val dated = if (messageDateSeconds != null && parsedLoad.date.isBlank()) {
            parsedLoad.copy(date = formatDateFromUnixSeconds(messageDateSeconds))
        } else {
            parsedLoad
        }
        val now = System.currentTimeMillis()
        val parsedAt = messageDateSeconds?.times(1000) ?: now
        return dated
            .copy(
                parsedAt = parsedAt,
                updatedAt = now,
            )
            .withReportingWeek()
            .withRouteMetrics()
    }
}
