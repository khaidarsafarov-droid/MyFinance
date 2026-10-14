package com.truckerload.data.local

import com.truckerload.data.local.entities.MiscExpenseEntity
import com.truckerload.domain.model.MiscExpense
import com.truckerload.domain.money.Money

fun MiscExpenseEntity.toDomain(): MiscExpense =
    MiscExpense(
        id = id,
        amount = Money.fromCents(amount),
        description = description,
        date = date,
        receiptPhotoPath = receiptPhotoPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun MiscExpense.toEntity(): MiscExpenseEntity =
    MiscExpenseEntity(
        id = id,
        amount = amount.cents,
        description = description,
        date = date,
        receiptPhotoPath = receiptPhotoPath,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
