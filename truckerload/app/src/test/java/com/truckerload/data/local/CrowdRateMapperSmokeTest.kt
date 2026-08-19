package com.truckerload.data.local

import com.truckerload.domain.crowd.CrowdRateReport
import com.truckerload.domain.crowd.CrowdRateSource
import com.truckerload.domain.model.EquipmentType
import org.junit.Assert.assertEquals
import org.junit.Test

class CrowdRateMapperSmokeTest {

    @Test
    fun crowdRate_roundTrip_preservesEquipmentType() {
        val report = CrowdRateReport(
            id = "me:load-1",
            fromState = "NC",
            toState = "GA",
            rpm = 2.5,
            rate = 2000.0,
            miles = 800.0,
            reportedAtMillis = 1_700_000_000_000L,
            source = CrowdRateSource.ME,
            equipmentType = EquipmentType.REEFER,
        )
        val back = report.toEntity(syncedAtMillis = 1_700_000_000_100L).toReport()
        assertEquals(report.id, back.id)
        assertEquals(report.fromState, back.fromState)
        assertEquals(report.toState, back.toState)
        assertEquals(report.rpm, back.rpm, 0.0)
        assertEquals(CrowdRateSource.ME, back.source)
        assertEquals(EquipmentType.REEFER, back.equipmentType)
    }
}
