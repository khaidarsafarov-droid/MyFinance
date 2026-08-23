package com.truckerload.data.backup

import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.EquipmentType
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Paycheck
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType

object BackupTestFixtures {
    const val PARSED_AT = 1_721_548_800_000L
    const val UPDATED_AT = 1_721_552_400_000L
    const val ADDED_AT = 1_721_560_000_000L
    const val EXPORTED_AT = 1_721_570_000_000L

    fun sampleLoad(
        id: String = "L-ROUNDTRIP",
        tripId: String = "T-116KYL6KW",
    ): Load = Load(
        id = id,
        tripId = tripId,
        date = "2026-07-21",
        totalRate = 2500.0,
        totalMiles = 850.0,
        pointA = "Garner, NC",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 30,
        year = 2026,
        rawMessage = "Trip ID: $tripId",
        parsedAt = PARSED_AT,
        updatedAt = UPDATED_AT,
        actualFinishDate = "2026-07-22 18:00",
        durationDays = 1.5,
        pace = 1666.67,
        equipmentType = EquipmentType.DRY_VAN,
        stops = listOf(
            Stop(
                id = 11,
                loadId = id,
                stopNumber = 1,
                type = StopType.PU,
                puNumber = "PU-1",
                note = "gate 4",
                scheduledTime = "",
                timezone = "EDT",
                facilityCode = "SWF2",
                fullAddress = "SWF2, Garner, NC",
                city = "Garner",
                state = "NC",
                zip = "27529",
            ),
            Stop(
                id = 12,
                loadId = id,
                stopNumber = 2,
                type = StopType.DEL,
                puNumber = null,
                note = null,
                scheduledTime = "",
                timezone = "CDT",
                facilityCode = "DFW9",
                fullAddress = "Dallas, TX",
                city = "Dallas",
                state = "TX",
                zip = "75201",
            ),
        ),
        penalties = listOf(
            Penalty(id = 21, loadId = id, description = "layover", amount = 150.0),
        ),
    )

    fun samplePaycheck(): Paycheck = Paycheck(
        id = 31,
        weekNumber = 30,
        year = 2026,
        weekLabel = "Week 30",
        weekStartDate = "2026-07-20",
        weekEndDate = "2026-07-26",
        driverName = "Test Driver",
        grossAmount = 3200.0,
        netAmount = 2800.0,
        rawExtractedText = "gross 3200",
        sourceFileName = "pay.pdf",
        addedAt = ADDED_AT,
    )

    fun sampleDiesel(): Diesel = Diesel(
        id = 41,
        weekNumber = 30,
        year = 2026,
        weekLabel = "Week 30",
        weekStartDate = "2026-07-20",
        weekEndDate = "2026-07-26",
        totalAmount = 540.25,
        gallons = 120.0,
        pricePerGallon = 4.502,
        location = "Pilot, NC",
        rawExtractedText = "diesel 540.25",
        sourceFileName = "fuel.jpg",
        addedAt = ADDED_AT,
    )

    fun sampleBackup(accountId: String? = "user-abc"): BackupData = BackupData(
        schemaVersion = BackupSchema.CURRENT,
        version = BackupSchema.CURRENT,
        exportedAt = EXPORTED_AT,
        accountId = accountId,
        loads = listOf(sampleLoad()),
        paychecks = listOf(samplePaycheck()),
        diesel = listOf(sampleDiesel()),
    )
}
