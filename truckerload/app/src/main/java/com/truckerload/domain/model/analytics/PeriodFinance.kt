package com.truckerload.domain.model.analytics

/**
 * Money side of a reporting period, built only from the driver's own paycheck
 * and diesel entries. Net profit follows the journal rule used for weeks:
 * paycheck minus diesel.
 */
data class PeriodFinance(
    val paycheckTotal: Double = 0.0,
    val dieselTotal: Double = 0.0,
    val dieselGallons: Double = 0.0,
    val dieselSavings: Double = 0.0,
) {
    val netProfit: Double get() = paycheckTotal - dieselTotal

    val avgPricePerGallon: Double?
        get() = if (dieselGallons > 0.0) dieselTotal / dieselGallons else null

    val hasData: Boolean
        get() = paycheckTotal > 0.0 || dieselTotal > 0.0
}
