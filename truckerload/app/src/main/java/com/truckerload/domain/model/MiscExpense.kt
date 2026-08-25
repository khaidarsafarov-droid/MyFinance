package com.truckerload.domain.model

/** A free-form driver expense that is not diesel, paycheck, or a load penalty. */
data class MiscExpense(
    val id: Int = 0,
    val amount: Double,
    val description: String,
    val date: String,
    val createdAt: Long,
    val updatedAt: Long,
)
