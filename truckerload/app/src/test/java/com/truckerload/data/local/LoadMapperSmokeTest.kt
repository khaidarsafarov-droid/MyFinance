package com.truckerload.data.local

import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Penalty
import com.truckerload.domain.model.Stop
import com.truckerload.domain.model.StopType
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadMapperSmokeTest {

    @Test
    fun loadEntity_toDomain_preservesScalarsAndNestedStops() {
        val entity = LoadEntity(
            id = "load-1",
            tripId = "T-ABC123",
            date = "2026-01-15",
            totalRate = 2500.0,
            totalMiles = 850.0,
            pointA = "Garner, NC",
            pointB = "Atlanta, GA",
            puCount = 1,
            delCount = 1,
            weekNumber = 3,
            year = 2026,
            rawMessage = "Trip ID: T-ABC123",
            parsedAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_100_000L,
            route = "Garner, NC → Atlanta, GA",
            firstPuCityState = "Garner, NC",
            lastDelCityState = "Atlanta, GA",
            durationDays = 2.0,
            pace = 1250.0,
            stopCount = 2,
            isDispute = false,
            disputeResponseDate = null,
            disputeCompleted = false,
            actualFinishDate = null,
        )
        val stops = listOf(
            StopEntity(
                id = 1,
                loadId = entity.id,
                stopNumber = 1,
                type = "PU",
                puNumber = "PU1",
                note = "",
                scheduledTime = "2026-01-15 08:00",
                timezone = "America/New_York",
                facilityCode = "SWF2",
                fullAddress = "123 Main St",
                city = "Garner",
                state = "NC",
                zip = "27529",
            ),
            StopEntity(
                id = 2,
                loadId = entity.id,
                stopNumber = 2,
                type = "DEL",
                puNumber = null,
                note = "",
                scheduledTime = "2026-01-16 14:00",
                timezone = "America/New_York",
                facilityCode = "ATL1",
                fullAddress = "456 Peachtree",
                city = "Atlanta",
                state = "GA",
                zip = "30303",
            ),
        )
        val penalties = listOf(
            PenaltyEntity(id = 1, loadId = entity.id, description = "Detention", amount = 75.0),
        )

        val domain = entity.toDomain(stops = stops, penalties = penalties)

        assertEquals(entity.id, domain.id)
        assertEquals(entity.tripId, domain.tripId)
        assertEquals(entity.totalRate, domain.totalRate, 0.0)
        assertEquals(entity.totalMiles, domain.totalMiles, 0.0)
        assertEquals(2, domain.stops.size)
        assertEquals(StopType.PU, domain.stops.first().type)
        assertEquals(1, domain.penalties.size)
        assertEquals("Detention", domain.penalties.first().description)
    }

    @Test
    fun load_toEntity_roundTrip_preservesCoreFields() {
        val load = Load(
            id = "load-2",
            tripId = "T-XYZ999",
            date = "2026-02-01",
            totalRate = 1800.0,
            totalMiles = 620.0,
            pointA = "Dallas, TX",
            pointB = "Houston, TX",
            puCount = 1,
            delCount = 1,
            weekNumber = 5,
            year = 2026,
            rawMessage = "Trip ID: T-XYZ999",
            parsedAt = 1_700_100_000_000L,
            updatedAt = 1_700_100_100_000L,
            route = "Dallas, TX → Houston, TX",
            firstPuCityState = "Dallas, TX",
            lastDelCityState = "Houston, TX",
            durationDays = 1.0,
            pace = 1800.0,
            stopCount = 0,
            equipmentType = com.truckerload.domain.model.EquipmentType.AMAZON_RELAY,
        )

        val entity = load.toEntity()
        val back = entity.toDomain()

        assertEquals(load.id, back.id)
        assertEquals(load.tripId, back.tripId)
        assertEquals(load.totalRate, back.totalRate, 0.0)
        assertEquals(load.totalMiles, back.totalMiles, 0.0)
        assertEquals(load.route, back.route)
        assertEquals(load.pace, back.pace, 0.0)
        assertEquals(com.truckerload.domain.model.EquipmentType.AMAZON_RELAY, back.equipmentType)
    }

    @Test
    fun stop_roundTrip_preservesFields() {
        val stop = Stop(
            id = 1,
            loadId = "load-1",
            stopNumber = 1,
            type = StopType.DEL,
            puNumber = null,
            note = "Call ahead",
            scheduledTime = "2026-01-20 10:00",
            timezone = "America/Chicago",
            facilityCode = "CHI9",
            fullAddress = "789 Lake Shore",
            city = "Chicago",
            state = "IL",
            zip = "60601",
        )

        val entity = stop.toEntity(loadId = "load-1")
        val back = entity.toDomain()

        assertEquals(stop, back)
    }

    @Test
    fun penalty_roundTrip_preservesFields() {
        val penalty = Penalty(id = 1, loadId = "load-1", description = "Lumper", amount = 120.0)

        val entity = penalty.toEntity(loadId = "load-1")
        val back = entity.toDomain()

        assertEquals(penalty, back)
    }
}
