package com.truckerload.domain.units

import com.truckerload.domain.parser.DecimalInput
import java.util.Locale
import kotlin.math.abs

/**
 * Presentation-boundary conversions. Stored journal values stay miles and US gallons.
 * 1 mi = 1.609344 km. 1 US gal = 3.785411784 L.
 */
object MeasurementUnits {
    const val MILES_TO_KM = 1.609344
    const val GALLONS_TO_LITERS = 3.785411784

    fun displayDistance(canonicalMiles: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(canonicalMiles, if (metric) MILES_TO_KM else 1.0)

    fun storedDistance(displayDistance: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(displayDistance, if (metric) 1.0 / MILES_TO_KM else 1.0)

    fun displayVolume(canonicalGallons: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(canonicalGallons, if (metric) GALLONS_TO_LITERS else 1.0)

    fun storedVolume(displayVolume: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(displayVolume, if (metric) 1.0 / GALLONS_TO_LITERS else 1.0)

    /** Dollars per mile → dollars per kilometer when [metric]. */
    fun displayRatePerDistance(perMile: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(perMile, if (metric) 1.0 / MILES_TO_KM else 1.0)

    fun storedRatePerDistance(displayRate: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(displayRate, if (metric) MILES_TO_KM else 1.0)

    /** Dollars per gallon → dollars per liter when [metric]. */
    fun displayPricePerVolume(perGallon: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(perGallon, if (metric) 1.0 / GALLONS_TO_LITERS else 1.0)

    fun storedPricePerVolume(displayPrice: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(displayPrice, if (metric) GALLONS_TO_LITERS else 1.0)

    /** Miles per gallon → L/100 km when [metric]. Non-positive MPG has no economy. */
    fun displayFuelEconomy(mpg: Double, metric: Boolean = DisplayUnits.isMetric): Double? {
        if (!mpg.isFinite() || mpg <= 0.0) return null
        if (!metric) return mpg
        return (GALLONS_TO_LITERS * 100.0) / (mpg * MILES_TO_KM)
    }

    fun storedFuelEconomy(displayEconomy: Double, metric: Boolean = DisplayUnits.isMetric): Double? {
        if (!displayEconomy.isFinite() || displayEconomy <= 0.0) return null
        if (!metric) return displayEconomy
        return (GALLONS_TO_LITERS * 100.0) / (displayEconomy * MILES_TO_KM)
    }

    /** Dollars per 100 miles → dollars per 100 km when [metric]. */
    fun displayCostPer100(per100Miles: Double, metric: Boolean = DisplayUnits.isMetric): Double =
        scaled(per100Miles, if (metric) 1.0 / MILES_TO_KM else 1.0)

    fun parseOrNull(raw: String?): Double? = DecimalInput.parse(raw)

    /** Blank and unparsable input stay null. Does not convert. */
    fun parseDisplayOrNull(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        return DecimalInput.parse(raw)
    }

    fun formatInput(value: Double): String {
        if (!value.isFinite()) return ""
        val text = String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        return text.ifEmpty { "0" }
    }

    private fun scaled(value: Double, factor: Double): Double {
        if (!value.isFinite()) return value
        if (value == 0.0 || factor == 1.0) return value
        val scaled = value * factor
        return if (abs(scaled) < 1e-12) 0.0 else scaled
    }
}
