package com.truckerload.domain.parser

/** Load fields an extraction can leave empty. */
enum class LoadField {
    RATE,
    PICKUP,
    DELIVERY,
    MILES,
    DATE,
    TRIP_ID,
}

/**
 * What a parsed draft is still missing.
 * Required gaps block the save; optional gaps only need the driver to confirm.
 */
data class LoadCompleteness(
    val missingRequired: List<LoadField> = emptyList(),
    val missingOptional: List<LoadField> = emptyList(),
) {
    val canSave: Boolean get() = missingRequired.isEmpty()
    val isComplete: Boolean get() = missingRequired.isEmpty() && missingOptional.isEmpty()
    val needsConfirmation: Boolean get() = canSave && missingOptional.isNotEmpty()
    val allMissing: List<LoadField> get() = missingRequired + missingOptional
}

object LoadCompletenessChecker {

    fun of(
        rate: Double?,
        miles: Double?,
        points: List<String>,
        tripId: String,
        date: String,
    ): LoadCompleteness {
        val filled = points.map { it.trim() }.filter { it.isNotBlank() }
        val required = buildList {
            if (rate == null || rate <= 0.0) add(LoadField.RATE)
            if (filled.isEmpty()) add(LoadField.PICKUP)
        }
        val optional = buildList {
            if (filled.size == 1) add(LoadField.DELIVERY)
            if (miles == null || miles <= 0.0) add(LoadField.MILES)
            if (ParseUtils.normalizeDate(date).length < 10) add(LoadField.DATE)
            if (tripId.isBlank()) add(LoadField.TRIP_ID)
        }
        return LoadCompleteness(missingRequired = required, missingOptional = optional)
    }

    fun of(draft: LoadDraftFields): LoadCompleteness = of(
        rate = draft.parsedRate(),
        miles = draft.parsedMiles(),
        points = listOf(draft.pointA, draft.pointB),
        tripId = draft.tripId,
        date = draft.date,
    )
}
