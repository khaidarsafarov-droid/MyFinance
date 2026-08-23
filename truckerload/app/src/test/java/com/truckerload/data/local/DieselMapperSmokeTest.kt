package com.truckerload.data.local

import com.truckerload.data.local.entities.DieselEntity
import com.truckerload.domain.model.Diesel
import org.junit.Assert.assertEquals
import org.junit.Test

class DieselMapperSmokeTest {

    @Test
    fun dieselEntity_toDomain_preservesAllFields() {
        val entity = DieselEntity(
            id = 42,
            weekNumber = 12,
            year = 2026,
            weekLabel = "W12 2026",
            weekStartDate = "2026-03-16",
            weekEndDate = "2026-03-22",
            totalAmount = 487.35,
            gallons = 118.5,
            pricePerGallon = 4.11,
            discountPricePerGallon = 3.95,
            location = "Pilot #412, OK",
            rawExtractedText = "Diesel receipt OCR text",
            sourceFileName = "fuel_receipt.jpg",
            addedAt = 1_700_200_000_000L,
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.weekNumber, domain.weekNumber)
        assertEquals(entity.year, domain.year)
        assertEquals(entity.weekLabel, domain.weekLabel)
        assertEquals(entity.totalAmount, domain.totalAmount, 0.0)
        assertEquals(entity.gallons, domain.gallons)
        assertEquals(entity.pricePerGallon, domain.pricePerGallon)
        assertEquals(entity.discountPricePerGallon, domain.discountPricePerGallon)
        assertEquals(entity.location, domain.location)
        assertEquals(entity.rawExtractedText, domain.rawExtractedText)
        assertEquals(entity.sourceFileName, domain.sourceFileName)
        assertEquals(entity.addedAt, domain.addedAt)
    }

    @Test
    fun diesel_toEntity_roundTrip_preservesFields() {
        val diesel = Diesel(
            id = 7,
            weekNumber = 8,
            year = 2026,
            weekLabel = "W08 2026",
            weekStartDate = "2026-02-17",
            weekEndDate = "2026-02-23",
            totalAmount = 312.0,
            gallons = null,
            pricePerGallon = null,
            location = null,
            rawExtractedText = "Manual entry",
            sourceFileName = null,
            addedAt = 1_700_300_000_000L,
        )

        val entity = diesel.toEntity()
        val back = entity.toDomain()

        assertEquals(diesel, back)
    }
}
