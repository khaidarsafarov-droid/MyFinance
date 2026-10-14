package com.truckerload.domain.model

import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice

/**
 * Pure diesel fill math: paid total and discount savings from gallons + prices.
 *
 * Example: 500 gal @ $5.10 list / $4.09 discount → paid $2,045, saved $505.
 */
object DieselPurchaseMath {

    fun paidTotal(
        gallons: Double?,
        pricePerGallon: UnitPrice?,
        discountPricePerGallon: UnitPrice?,
    ): Money? {
        val g = gallons ?: return null
        if (g <= 0.0) return null
        val paidPrice = when {
            discountPricePerGallon != null && discountPricePerGallon.isPositive() -> discountPricePerGallon
            pricePerGallon != null && pricePerGallon.isPositive() -> pricePerGallon
            else -> return null
        }
        return paidPrice.timesGallons(g)
    }

    fun savings(
        gallons: Double?,
        pricePerGallon: UnitPrice?,
        discountPricePerGallon: UnitPrice?,
    ): Money? {
        val g = gallons ?: return null
        val list = pricePerGallon ?: return null
        val disc = discountPricePerGallon ?: return null
        if (g <= 0.0 || !list.isPositive() || !disc.isPositive()) return null
        val gap = list - disc
        if (!gap.isPositive()) return Money.ZERO
        return gap.timesGallons(g)
    }
}
