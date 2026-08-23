package com.truckerload.presentation.screens.add

import com.truckerload.domain.parser.LoadCompleteness
import com.truckerload.domain.parser.LoadCompletenessChecker
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
    /** Extra stops after B: C, D, E, … */
    val extraPoints: List<String> = emptyList(),
) {
    fun parsedRate(): Double? = rate.replace(",", ".").trim().toDoubleOrNull()

    fun parsedMiles(): Double = miles.replace(",", ".").trim().toDoubleOrNull() ?: 0.0

    fun allPoints(): List<String> = listOf(pointA, pointB) + extraPoints

    fun filledPoints(): List<String> = allPoints().map { it.trim() }.filter { it.isNotBlank() }

    fun canSave(): Boolean = completeness().canSave

    fun completeness(): LoadCompleteness = LoadCompletenessChecker.of(
        rate = parsedRate(),
        miles = parsedMiles(),
        points = allPoints(),
        tripId = tripId,
        date = date,
    )

    fun canAddPoint(): Boolean = extraPoints.size + MIN_ROUTE_POINTS < MAX_ROUTE_POINTS

    fun nextPointLetter(): String? =
        if (canAddPoint()) pointLetter(MIN_ROUTE_POINTS + extraPoints.size) else null

    fun withPoint(index: Int, value: String): ManualLoadFields = when (index) {
        0 -> copy(pointA = value)
        1 -> copy(pointB = value)
        else -> {
            val extraIndex = index - MIN_ROUTE_POINTS
            if (extraIndex !in extraPoints.indices) this
            else copy(extraPoints = extraPoints.toMutableList().also { it[extraIndex] = value })
        }
    }

    fun addPoint(): ManualLoadFields {
        if (!canAddPoint()) return this
        return copy(extraPoints = extraPoints + "")
    }

    fun removePoint(index: Int): ManualLoadFields {
        val extraIndex = index - MIN_ROUTE_POINTS
        if (extraIndex !in extraPoints.indices) return this
        return copy(extraPoints = extraPoints.filterIndexed { i, _ -> i != extraIndex })
    }

    companion object {
        const val MIN_ROUTE_POINTS = 2
        const val MAX_ROUTE_POINTS = 26

        fun pointLetter(index: Int): String =
            if (index in 0..25) ('A' + index).toString() else (index + 1).toString()

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
