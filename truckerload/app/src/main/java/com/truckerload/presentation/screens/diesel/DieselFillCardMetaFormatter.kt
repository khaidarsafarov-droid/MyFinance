package com.truckerload.presentation.screens.diesel

import com.truckerload.domain.model.Diesel
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
        val gallonsLabel = diesel.gallons
            ?.takeIf { it > 0.0 }
            ?.let { String.format(Locale.US, "%.2f gal", it) }
            ?: return ""

        val list = diesel.pricePerGallon?.takeIf { it > 0.0 }
        val discount = diesel.discountPricePerGallon?.takeIf { it > 0.0 }

        val line = when {
            list != null && discount != null && discount < list ->
                String.format(Locale.US, withDiscountFormat, gallonsLabel, list, discount)
            list != null ->
                String.format(Locale.US, listOnlyFormat, gallonsLabel, list)
            discount != null ->
                String.format(Locale.US, listOnlyFormat, gallonsLabel, discount)
            else -> gallonsLabel
        }

        val savedSuffix = diesel.savingsAmount
            ?.takeIf { it > 0.0 }
            ?.let { String.format(Locale.US, savedSuffixFormat, formatSavedAmount(it)) }

        return line + (savedSuffix ?: "")
    }
}
