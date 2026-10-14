package com.truckerload.utils

import com.truckerload.R
import com.truckerload.domain.parser.DecimalInput

/** Resource ids for money and number fields. Null means the value is acceptable. */
object FieldErrors {

    fun amountRes(raw: String): Int? =
        if (AmountInputValidator.parsePositiveAmount(raw) != null) {
            null
        } else {
            R.string.field_error_amount_example
        }

    fun gallonsRes(raw: String): Int? =
        if (AmountInputValidator.parsePositiveAmount(raw) != null) {
            null
        } else {
            R.string.field_error_gallons_positive
        }

    fun milesRes(raw: String): Int? {
        if (raw.isBlank()) return null
        val miles = DecimalInput.parse(raw)
        return if (miles == null || miles < 0.0) R.string.field_error_miles else null
    }

    /** Blank discount is optional. A filled value must be a positive price at or below pump price. */
    fun discountRes(discountRaw: String, priceRaw: String): Int? {
        if (discountRaw.isBlank()) return null
        val discount = AmountInputValidator.parsePositiveAmount(discountRaw)
            ?: return R.string.field_error_amount_example
        val price = AmountInputValidator.parsePositiveAmount(priceRaw) ?: return null
        return if (discount > price) R.string.add_diesel_discount_above_price else null
    }
}
