package com.truckerload.domain.model.analytics

/**
 * Money side of a reporting period, built only from the driver's own paycheck
 * and diesel entries. Totals stay separate so the report does not pick a
 * net-profit formula for the driver.
 */
data class PeriodFinance(
    val paycheckTotal: Double = 0.0,
    val dieselTotal: Double = 0.0,
    val dieselGallons: Double = 0.0,
    val dieselSavings: Double = 0.0,
) {
    val avgPricePerGallon: Double?
        get() = if (dieselGallons > 0.0) dieselTotal / dieselGallons else null

    val hasData: Boolean
        get() = paycheckTotal > 0.0 || dieselTotal > 0.0
}
