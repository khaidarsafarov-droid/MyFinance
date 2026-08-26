package com.truckerload.data.local

import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.domain.model.Paycheck

fun PaycheckEntity.toDomain(): Paycheck =
    Paycheck(
        id = id,
        weekNumber = weekNumber,
        year = year,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        driverName = driverName,
        grossAmount = grossAmount,
        netAmount = netAmount,
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
        grossAmount = grossAmount,
        netAmount = netAmount,
        rawExtractedText = rawExtractedText,
        sourceFileName = sourceFileName,
        addedAt = addedAt,
        sourceFilePath = sourceFilePath,
    )
