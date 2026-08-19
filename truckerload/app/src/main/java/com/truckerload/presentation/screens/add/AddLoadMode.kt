package com.truckerload.presentation.screens.add

import com.truckerload.domain.parser.LoadDraftFields

enum class AddLoadInputMode {
    PASTE,
    MANUAL,
    DOCUMENT,
}

data class ManualLoadFields(
    val tripId: String = "",
    val date: String = "",
    val rate: String = "",
    val miles: String = "",
    val pointA: String = "",
    val pointB: String = "",
) {
    fun parsedRate(): Double? = rate.replace(",", ".").trim().toDoubleOrNull()

    fun parsedMiles(): Double = miles.replace(",", ".").trim().toDoubleOrNull() ?: 0.0

    fun canSave(): Boolean {
        val rateValue = parsedRate() ?: return false
        return rateValue > 0.0 && (pointA.isNotBlank() || pointB.isNotBlank())
    }

    companion object {
        fun fromDraft(draft: LoadDraftFields, fallbackDate: String): ManualLoadFields = ManualLoadFields(
            tripId = draft.tripId,
            date = draft.date.ifBlank { fallbackDate },
            rate = draft.rate,
            miles = draft.miles,
            pointA = draft.pointA,
            pointB = draft.pointB,
        )
    }
}
