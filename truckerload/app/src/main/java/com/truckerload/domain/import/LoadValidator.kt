package com.truckerload.domain.import

import com.truckerload.domain.model.Load
import java.time.LocalDate

class LoadValidator {
    data class ValidationResult(val isValid: Boolean, val errors: List<String>)

    fun validate(load: Load): ValidationResult {
        val errors = mutableListOf<String>()

        if (load.tripId.isBlank() || load.tripId.length < 3) {
            errors.add("Invalid Trip ID: ${load.tripId}")
        }
        if (load.totalRate <= 0) {
            errors.add("Invalid rate: ${load.totalRate}")
        }
        if (!hasValidDate(load.date)) {
            errors.add("Missing or invalid load date: '${load.date}'")
        }
        if (load.stops.isEmpty() && load.pointA.isBlank() && load.pointB.isBlank()) {
            errors.add("No route points found")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }

    /** Accepts `YYYY-MM-DD` (optionally followed by time). */
    private fun hasValidDate(date: String): Boolean {
        if (date.length < 10) return false
        val ymd = date.take(10)
        if (ymd[4] != '-' || ymd[7] != '-') return false
        val parsed = runCatching { LocalDate.parse(ymd) }.getOrNull() ?: return false
        return parsed.year in 2000..2100
    }
}
