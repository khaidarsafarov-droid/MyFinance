package com.truckerload.domain.import

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.StopType

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
        if (load.stops.isEmpty() && load.pointA.isBlank() && load.pointB.isBlank()) {
            errors.add("No route points found")
        }

        return ValidationResult(errors.isEmpty(), errors)
    }
}
