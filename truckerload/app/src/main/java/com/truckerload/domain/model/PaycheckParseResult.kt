package com.truckerload.domain.model

data class PaycheckParseResult(
    val driverName: String?,
    val weekStartDate: String?,
    val weekEndDate: String?,
    val grossAmount: Double?,
    val netAmount: Double,
    val confidence: String,
    /** Pay date printed on the statement. */
    val settlementDate: String? = null,
    val miles: Double? = null,
    val deductionsAmount: Double? = null,
    val loadsNetAmount: Double? = null,
)
