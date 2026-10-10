package com.truckerload.domain.model

/** Empty miles a driver adds on one load. They stack on top of loaded miles. */
object DeadheadMiles {
    const val MAX = 9_999.0

    /** The only value the journal may store. NaN and negatives become zero. */
    fun normalize(miles: Double): Double {
        if (miles.isNaN() || miles <= 0.0) return 0.0
        return miles.coerceAtMost(MAX)
    }

    /** Blank input clears deadhead. Returns null when the text is not a mileage. */
    fun parse(raw: String): Double? {
        val cleaned = raw.trim().replace(',', '.').replace(" ", "")
        if (cleaned.isEmpty()) return 0.0
        val value = cleaned.toDoubleOrNull() ?: return null
        if (value < 0.0 || value > MAX) return null
        return value
    }
}
