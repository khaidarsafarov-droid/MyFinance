package com.truckerload.domain.parser

import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType
import com.truckerload.utils.parseScheduledTimeToMillis

/**
 * Structured Amazon Relay parse result for Local-First goal math.
 * Gross + PU→DEL duration feed Actual Daily Yield automatically after Room insert.
 */
data class AmazonRelayParseResult(
    val load: Load,
    val totalRate: Double,
    val totalLoadedMiles: Double,
    val firstPuTime: String?,
    val lastDelTime: String?,
    val durationDays: Double
)

/**
 * Parses Amazon Relay load text: Total Rate, Total Loaded Miles, first Pu-time, last Del-time.
 */
fun parseAmazonRelayLoad(text: String): AmazonRelayParseResult? {
    val load = LoadMessageParser.parseOne(text.trim()) ?: return null
    // FIX: sort PU/DEL with the load.date year so yearless MM/DD doesn't jump ~365d
    val yearHint = load.date.take(4).toIntOrNull()
    val firstPuTime = load.stops
        .filter { it.type == StopType.PU && it.scheduledTime.isNotBlank() }
        .minByOrNull {
            parseScheduledTimeToMillis(
                scheduledTime = it.scheduledTime,
                defaultYear = yearHint,
                trustDefaultYear = yearHint != null,
            ) ?: Long.MAX_VALUE
        }
        ?.scheduledTime
    val lastDelTime = load.stops
        .filter { it.type == StopType.DEL && it.scheduledTime.isNotBlank() }
        .maxByOrNull {
            parseScheduledTimeToMillis(
                scheduledTime = it.scheduledTime,
                defaultYear = yearHint,
                trustDefaultYear = yearHint != null,
            ) ?: 0L
        }
        ?.scheduledTime

    return AmazonRelayParseResult(
        load = load,
        totalRate = load.totalRate,
        totalLoadedMiles = load.totalMiles,
        firstPuTime = firstPuTime,
        lastDelTime = lastDelTime,
        durationDays = LoadYieldCalculator.loadActiveDurationDays(load)
    )
}
