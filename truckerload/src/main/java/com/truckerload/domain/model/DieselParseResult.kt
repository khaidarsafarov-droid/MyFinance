package com.truckerload.domain.model

data class DieselParseResult(
    val date: String?,
    val totalAmount: Double,
    val gallons: Double?,
    val pricePerGallon: Double?,
    val location: String?,
    val vendor: String?,
    val confidence: String
)
