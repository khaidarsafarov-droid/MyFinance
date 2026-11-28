package com.truckerload.domain.parser

import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import com.truckerload.domain.parser.DecimalInput
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
    fun parsedRate(): Double? = DecimalInput.parse(rate)?.takeIf { it > 0.0 }

    fun parsedMiles(): Double = DecimalInput.parse(miles)?.coerceAtLeast(0.0) ?: 0.0

    /** Keeps this draft's values and takes [other]'s only where this one is blank. */
    fun fillBlanksFrom(other: LoadDraftFields): LoadDraftFields = LoadDraftFields(
        tripId = tripId.ifBlank { other.tripId },
        date = date.ifBlank { other.date },
        rate = rate.ifBlank { other.rate },
        miles = miles.ifBlank { other.miles },
        pointA = pointA.ifBlank { other.pointA },
        pointB = pointB.ifBlank { other.pointB },
    )

    /** Blanks values that are clearly not what the field means (a time as a city, $2 as a rate). */
    fun dropImplausible(): LoadDraftFields = copy(
        tripId = tripId.takeIf { it.isBlank() || (it.any(Char::isDigit) && !generatedId.matches(it)) }.orEmpty(),
        rate = rate.takeIf { (parsedRate() ?: 0.0) >= MIN_PLAUSIBLE_RATE }.orEmpty(),
        pointA = pointA.takeUnless { looksLikeNotAPlace(it) }.orEmpty(),
        pointB = pointB.takeUnless { looksLikeNotAPlace(it) }.orEmpty(),
    )

    /** A usable rate plus both ends of the trip: nothing left to ask the user for. */
    fun isComplete(): Boolean = parsedRate() != null && pointA.isNotBlank() && pointB.isNotBlank()

    /** Both ends look like "City, ST" (optionally with a ZIP): a facility or label name is not a place. */
    fun hasValidPlaces(): Boolean = listOf(pointA, pointB).all { isPlaceName(it) }

    fun isEmpty(): Boolean = listOf(tripId, date, rate, miles, pointA, pointB).all { it.isBlank() }

    companion object {
        private const val MIN_PLAUSIBLE_RATE = 50.0
        private val cityState = Regex("""[A-Za-z][A-Za-z.'\- ]*,?\s+[A-Z]{2}\b(?:\s+\d{5}(?:-\d{4})?)?\s*$""")
        private val generatedId = Regex("""M-\d{8}-[A-F0-9]{6}""")
        private val timeLike = Regex("""\d{1,2}:\d{2}|^\d+\s|\s{3,}|\*|@""")

        private val plainPlace = Regex("""[A-Z][A-Za-z.'\-]+(?: [A-Z][A-Za-z.'\-]+){0,2}""")
        private val facilityWord = Regex(
            "billing|facility|plant|distribution|warehouse|reduced|center|\\binc\\b|llc|logistics|broker|sees",
            RegexOption.IGNORE_CASE,
        )

        private fun isPlaceName(value: String): Boolean {
            val v = value.trim()
            val shaped = cityState.containsMatchIn(v) || plainPlace.matches(v)
            return shaped && !facilityWord.containsMatchIn(v)
        }

        private fun looksLikeNotAPlace(value: String): Boolean =
            value.isNotBlank() && timeLike.containsMatchIn(value.trim())

        internal fun fromExtracted(fields: RateConTextExtractor.Fields): LoadDraftFields = LoadDraftFields(
            tripId = fields.loadId.orEmpty(),
            date = fields.date?.toString().orEmpty(),
            rate = formatAmount(fields.rate ?: 0.0),
            miles = formatAmount(fields.miles ?: 0.0),
            pointA = fields.pickup.orEmpty(),
            pointB = fields.delivery.orEmpty(),
        )

        fun formatAmount(value: Double): String {
            if (value <= 0.0) return ""
            val rounded = Math.round(value * 100.0) / 100.0
            return if (abs(rounded - rounded.toLong()) < 0.0001) {
                rounded.toLong().toString()
            } else {
                String.format(Locale.US, "%.2f", rounded)
            }
        }

        fun formatAmount(value: Money): String {
            if (!value.isPositive()) return ""
            return if (value.cents % 100L == 0L) {
                (value.cents / 100L).toString()
            } else {
                value.formatPlain()
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
