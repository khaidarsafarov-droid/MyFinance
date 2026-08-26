package com.truckerload.domain.model

/**
 * Pure diesel fill math: paid total and discount savings from gallons + prices.
 *
 * Example: 500 gal @ $5.10 list / $4.09 discount → paid $2,045, saved $505.
 */
object DieselPurchaseMath {

    fun paidTotal(
        gallons: Double?,
        pricePerGallon: Double?,
        discountPricePerGallon: Double?,
    ): Double? {
        val g = gallons ?: return null
        if (g <= 0.0) return null
        val paidPrice = when {
            discountPricePerGallon != null && discountPricePerGallon > 0.0 -> discountPricePerGallon
            pricePerGallon != null && pricePerGallon > 0.0 -> pricePerGallon
            else -> return null
        }
        // FIX: persist cents, not IEEE remainder (500 * 4.09 → 2044.999…)
        return com.truckerload.domain.goal.GoalMoneyMath.roundMoney(g * paidPrice)
    }

    fun savings(
        gallons: Double?,
        pricePerGallon: Double?,
        discountPricePerGallon: Double?,
    ): Double? {
        val g = gallons ?: return null
        val list = pricePerGallon ?: return null
        val disc = discountPricePerGallon ?: return null
        if (g <= 0.0 || list <= 0.0 || disc <= 0.0) return null
        return com.truckerload.domain.goal.GoalMoneyMath.roundMoney((list - disc) * g).coerceAtLeast(0.0)
    }
}
