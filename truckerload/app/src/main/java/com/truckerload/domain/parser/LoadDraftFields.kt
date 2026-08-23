package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import java.util.Locale
import kotlin.math.abs

/**
 * Best-effort fields extracted from pasted or OCR text for the add-load form.
 * Empty strings mean "not found" — the user can fill them in before save.
 */
data class LoadDraftFields(
    val tripId: String = "",
    val date: String = "",
    val rate: String = "",
    val miles: String = "",
    val pointA: String = "",
    val pointB: String = "",
) {
    fun parsedRate(): Double? = rate.replace(",", ".").trim().toDoubleOrNull()

    fun parsedMiles(): Double = miles.replace(",", ".").trim().toDoubleOrNull() ?: 0.0

    fun isEmpty(): Boolean = listOf(tripId, date, rate, miles, pointA, pointB).all { it.isBlank() }

    companion object {
        fun formatAmount(value: Double): String {
            if (value <= 0.0) return ""
            val rounded = Math.round(value * 100.0) / 100.0
            return if (abs(rounded - rounded.toLong()) < 0.0001) {
                rounded.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", rounded)
            }
        }

        fun fromLoad(load: Load): LoadDraftFields = LoadDraftFields(
            tripId = load.tripId.trim(),
            date = load.date.take(10).takeIf { it.length == 10 }.orEmpty(),
            rate = formatAmount(load.totalRate),
            miles = formatAmount(load.totalMiles),
            pointA = load.pointA.trim(),
            pointB = load.pointB.trim(),
        )
    }
}
