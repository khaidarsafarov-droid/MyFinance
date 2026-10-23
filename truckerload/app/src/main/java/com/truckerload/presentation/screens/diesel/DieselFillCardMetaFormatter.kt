package com.truckerload.presentation.screens.diesel

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.money.UnitPrice
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import java.util.Locale

/** One-line diesel fill summary for journal cards (gallons, list/discount $/gal, savings). */
internal object DieselFillCardMetaFormatter {

    fun format(
        diesel: Diesel,
        withDiscountFormat: String,
        listOnlyFormat: String,
        savedSuffixFormat: String,
        formatSavedAmount: (Double) -> String,
    ): String {
        val gallons = diesel.gallons?.takeIf { it > 0.0 } ?: return ""
        val metric = DisplayUnits.isMetric
        val gallonsLabel = if (metric) {
            String.format(Locale.US, "%.2f L", MeasurementUnits.displayVolume(gallons, true))
        } else {
            String.format(Locale.US, "%.2f gal", gallons)
        }

        val list = diesel.pricePerGallon?.takeIf { it.isPositive() }
        val discount = diesel.discountPricePerGallon?.takeIf { it.isPositive() }

        val line = when {
            list != null && discount != null && discount < list ->
                String.format(Locale.US, withDiscountFormat, gallonsLabel, unitDollars(list, metric), unitDollars(discount, metric))
            list != null ->
                String.format(Locale.US, listOnlyFormat, gallonsLabel, unitDollars(list, metric))
            discount != null ->
                String.format(Locale.US, listOnlyFormat, gallonsLabel, unitDollars(discount, metric))
            else -> gallonsLabel
        }

        val savedSuffix = diesel.savingsAmount
            ?.takeIf { it.isPositive() }
            ?.let { String.format(Locale.US, savedSuffixFormat, formatSavedAmount(it.cents / 100.0)) }

        return line + (savedSuffix ?: "")
    }

    private fun unitDollars(price: UnitPrice, metric: Boolean): Double {
        val perGallon = price.thousandths / 1000.0
        return if (metric) MeasurementUnits.displayPricePerVolume(perGallon, true) else perGallon
    }
}
