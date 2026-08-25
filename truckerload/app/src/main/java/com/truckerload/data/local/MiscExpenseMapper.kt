package com.truckerload.data.local

import com.truckerload.data.local.entities.MiscExpenseEntity
import com.truckerload.domain.model.MiscExpense

fun MiscExpenseEntity.toDomain(): MiscExpense =
    MiscExpense(
        id = id,
        amount = amount,
        description = description,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun MiscExpense.toEntity(): MiscExpenseEntity =
    MiscExpenseEntity(
        id = id,
        amount = amount,
        description = description,
        date = date,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
