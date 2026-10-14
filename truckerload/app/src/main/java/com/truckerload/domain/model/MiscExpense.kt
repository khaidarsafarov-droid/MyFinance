package com.truckerload.domain.model

import com.truckerload.domain.money.Money

/** A free-form driver expense that is not diesel, paycheck, or a load penalty. */
data class MiscExpense(
    val id: Int = 0,
    val amount: Money,
    val description: String,
    val date: String,
    /** Absolute path to an attached receipt photo (optional). */
    val receiptPhotoPath: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)
