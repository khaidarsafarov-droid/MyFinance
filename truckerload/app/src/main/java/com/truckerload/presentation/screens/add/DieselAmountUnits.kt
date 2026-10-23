package com.truckerload.presentation.screens.add

import com.truckerload.R
import com.truckerload.domain.money.UnitPrice
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits

/** Display/store boundary for a diesel form. Miles mode is an identity. */
internal object DieselAmountUnits {
    fun gallonsLabel(): Int =
        if (DisplayUnits.isMetric) R.string.add_diesel_gallons_km else R.string.add_diesel_gallons

    fun priceLabel(): Int =
        if (DisplayUnits.isMetric) R.string.add_diesel_price_per_gallon_km else R.string.add_diesel_price_per_gallon

    fun gallonsRequired(): Int =
        if (DisplayUnits.isMetric) R.string.add_diesel_gallons_required_km else R.string.add_diesel_gallons_required

    fun priceRequired(): Int =
        if (DisplayUnits.isMetric) R.string.add_diesel_price_required_km else R.string.add_diesel_price_required

    fun displayVolume(gallons: Double): Double =
        if (DisplayUnits.isMetric) MeasurementUnits.displayVolume(gallons) else gallons

    fun displayPrice(price: UnitPrice): Double =
        if (DisplayUnits.isMetric) {
            MeasurementUnits.displayPricePerVolume(price.thousandths / 1000.0)
        } else {
            price.thousandths / 1000.0
        }

    fun storedGallons(gallons: Double): Double =
        if (DisplayUnits.isMetric) MeasurementUnits.storedVolume(gallons) else gallons

    fun storedPrice(price: UnitPrice): UnitPrice =
        if (DisplayUnits.isMetric) {
            UnitPrice.fromDollars(MeasurementUnits.storedPricePerVolume(price.thousandths / 1000.0))
        } else {
            price
        }
}
