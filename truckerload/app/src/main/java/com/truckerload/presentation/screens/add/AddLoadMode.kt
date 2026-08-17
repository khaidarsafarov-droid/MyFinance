package com.truckerload.presentation.screens.add

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
}
