package com.truckerload.domain.parser

import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits

/**
 * Fills the next missing load field from a short chat reply.
 * A long paste is a new document, not an answer, and returns null.
 */
object IncompleteLoadFill {

    fun apply(draft: LoadDraftFields, reply: String): LoadDraftFields? {
        val text = reply.trim()
        if (text.isEmpty() || text.length > MAX_REPLY_CHARS || text.lineSequence().count() > 3) {
            return null
        }
        if (text.startsWith("/")) return null
        val next = LoadCompletenessChecker.of(draft).allMissing.firstOrNull {
            it == LoadField.RATE || it == LoadField.PICKUP || it == LoadField.DELIVERY
        } ?: return null
        return when (next) {
            LoadField.RATE -> {
                val amount = ParseUtils.parseMoney(text.filter { it.isDigit() || it == '.' || it == ',' })
                if (amount <= 0.0) null else draft.copy(rate = LoadDraftFields.formatAmount(amount))
            }
            LoadField.PICKUP -> draft.copy(pointA = text)
            LoadField.DELIVERY -> draft.copy(pointB = text)
            LoadField.MILES, LoadField.DATE, LoadField.TRIP_ID -> null
        }
    }

    fun foundSummary(draft: LoadDraftFields): String {
        val rate = draft.parsedRate()?.let { "$" + LoadDraftFields.formatAmount(it) }
        val miles = draft.parsedMiles().takeIf { it > 0.0 }?.let { stored ->
            if (DisplayUnits.isMetric) {
                "${LoadDraftFields.formatAmount(MeasurementUnits.displayDistance(stored))} km"
            } else {
                "${LoadDraftFields.formatAmount(stored)} mi"
            }
        }
        val route = listOf(draft.pointA, draft.pointB)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(" → ")
            .ifBlank { null }
        return listOfNotNull(draft.tripId.trim().ifBlank { null }, rate, miles, route)
            .joinToString(" · ")
    }

    private const val MAX_REPLY_CHARS = 180
}
