package com.truckerload.data.local

import com.truckerload.data.local.entities.DieselEntity
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.money.Money
import com.truckerload.domain.money.UnitPrice

fun DieselEntity.toDomain(): Diesel =
    Diesel(
        id = id,
        weekNumber = weekNumber,
        year = year,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        totalAmount = Money.fromCents(totalAmount),
        gallons = gallons,
        pricePerGallon = pricePerGallon?.let(UnitPrice::fromThousandths),
        discountPricePerGallon = discountPricePerGallon?.let(UnitPrice::fromThousandths),
        location = location,
        rawExtractedText = rawExtractedText,
        sourceFileName = sourceFileName,
        addedAt = addedAt,
    )

fun Diesel.toEntity(): DieselEntity =
    DieselEntity(
        id = id,
        weekNumber = weekNumber,
        year = year,
        weekLabel = weekLabel,
        weekStartDate = weekStartDate,
        weekEndDate = weekEndDate,
        totalAmount = totalAmount.cents,
        gallons = gallons,
        pricePerGallon = pricePerGallon?.thousandths,
        discountPricePerGallon = discountPricePerGallon?.thousandths,
        location = location,
        rawExtractedText = rawExtractedText,
        sourceFileName = sourceFileName,
        addedAt = addedAt,
    )
