package com.truckerload.domain.model

import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice

data class Diesel(
    val id: Int,
    val weekNumber: Int,
    val year: Int,
    val weekLabel: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val totalAmount: Money,
    val gallons: Double?,
    val pricePerGallon: UnitPrice?,
    /** Pump / rack price paid after fleet discount; null when unknown or no discount. */
    val discountPricePerGallon: UnitPrice? = null,
    val location: String?,
    val rawExtractedText: String,
    val sourceFileName: String?,
    val addedAt: Long,
) {
    /** Dollars saved vs list price when gallons + both prices are present. */
    val savingsAmount: Money?
        get() = DieselPurchaseMath.savings(
            gallons = gallons,
            pricePerGallon = pricePerGallon,
            discountPricePerGallon = discountPricePerGallon,
        )
}
