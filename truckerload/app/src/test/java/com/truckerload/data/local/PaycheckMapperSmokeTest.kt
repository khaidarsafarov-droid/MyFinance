package com.truckerload.data.local

import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.domain.model.Paycheck
import org.junit.Assert.assertEquals
import org.junit.Test

class PaycheckMapperSmokeTest {

    @Test
    fun paycheckEntity_toDomain_preservesSourceFilePath() {
        val entity = PaycheckEntity(
            id = 12,
            weekNumber = 35,
            year = 2026,
            weekLabel = "Неделя 35",
            weekStartDate = "2026-08-23",
            weekEndDate = "2026-08-29",
            driverName = "Khaidar Safarov",
            grossAmount = 12000.0,
            netAmount = 10907.79,
            rawExtractedText = "Settlement OCR",
            sourceFileName = "Settlement.pdf",
            addedAt = 1_700_400_000_000L,
            sourceFilePath = "paychecks/uuid_Settlement.pdf",
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.netAmount, domain.netAmount, 0.0)
        assertEquals(entity.grossAmount, domain.grossAmount)
        assertEquals(entity.sourceFileName, domain.sourceFileName)
        assertEquals(entity.sourceFilePath, domain.sourceFilePath)
        assertEquals(entity.addedAt, domain.addedAt)
    }

    @Test
    fun paycheck_toEntity_roundTrip_preservesFields() {
        val paycheck = Paycheck(
            id = 7,
            weekNumber = 34,
            year = 2026,
            weekLabel = "W34 2026",
            weekStartDate = "2026-08-16",
            weekEndDate = "2026-08-22",
            driverName = null,
            grossAmount = null,
            netAmount = 2500.0,
            rawExtractedText = "Manual",
            sourceFileName = null,
            addedAt = 1_700_500_000_000L,
            sourceFilePath = null,
        )

        val entity = paycheck.toEntity()
        val back = entity.toDomain()

        assertEquals(paycheck, back)
    }
}
