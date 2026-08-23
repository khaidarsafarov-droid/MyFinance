package com.truckerload.data.local

import com.truckerload.data.local.entities.DieselEntity
import com.truckerload.domain.model.Diesel

fun DieselEntity.toDomain(): Diesel =
    Diesel(
        id = id,
        weekNumber = weekNumber,
        year = year,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        totalAmount = totalAmount,
        gallons = gallons,
        pricePerGallon = pricePerGallon,
        discountPricePerGallon = discountPricePerGallon,
        location = location,
        rawExtractedText = rawExtractedText,
        sourceFileName = sourceFileName,
        addedAt = addedAt
    )

fun Diesel.toEntity(): DieselEntity =
    DieselEntity(
        id = id,
        weekNumber = weekNumber,
        year = year,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        totalAmount = totalAmount,
        gallons = gallons,
        pricePerGallon = pricePerGallon,
        discountPricePerGallon = discountPricePerGallon,
        location = location,
        rawExtractedText = rawExtractedText,
        sourceFileName = sourceFileName,
        addedAt = addedAt
    )
