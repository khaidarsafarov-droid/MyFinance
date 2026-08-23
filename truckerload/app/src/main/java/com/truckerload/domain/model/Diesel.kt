package com.truckerload.domain.model

data class Diesel(
    val id: Int,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val totalAmount: Double,
    val gallons: Double?,
    val pricePerGallon: Double?,
    /** Pump / rack price paid after fleet discount; null when unknown or no discount. */
    val discountPricePerGallon: Double? = null,
    val location: String?,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long,
) {
    /** Dollars saved vs list price when gallons + both prices are present. */
    val savingsAmount: Double?
        get() = DieselPurchaseMath.savings(
            gallons = gallons,
            pricePerGallon = pricePerGallon,
            discountPricePerGallon = discountPricePerGallon,
        )
}
