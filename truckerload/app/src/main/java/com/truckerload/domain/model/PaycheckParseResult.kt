package com.truckerload.domain.model

data class PaycheckParseResult(
    val driverName: String?,
    val weekStartDate: String?,
    val weekEndDate: String?,
    val grossAmount: Double?,
    val netAmount: Double,
    val confidence: String
)
