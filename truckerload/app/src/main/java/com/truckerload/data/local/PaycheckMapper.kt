package com.truckerload.data.local

import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.money.Money

fun PaycheckEntity.toDomain(): Paycheck =
    Paycheck(
        id = id,
        weekNumber = weekNumber,
        year = year,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        driverName = driverName,
        grossAmount = grossAmount?.let(Money::fromCents),
        netAmount = Money.fromCents(netAmount),
        rawExtractedText = rawExtractedText,
        sourceFileName = sourceFileName,
        addedAt = addedAt,
        sourceFilePath = sourceFilePath,
    )

fun Paycheck.toEntity(): PaycheckEntity =
    PaycheckEntity(
        id = id,
        weekNumber = weekNumber,
        year = year,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        driverName = driverName,
        grossAmount = grossAmount?.cents,
        netAmount = netAmount.cents,
        rawExtractedText = rawExtractedText,
        sourceFileName = sourceFileName,
        addedAt = addedAt,
        sourceFilePath = sourceFilePath,
    )
