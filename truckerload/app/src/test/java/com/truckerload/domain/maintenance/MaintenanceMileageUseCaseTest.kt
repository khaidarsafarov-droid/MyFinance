package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class MaintenanceMileageUseCaseTest {

    @Test
    fun calculate_sumsLoadsWithEndDateOnOrAfterService() {
        val loads = listOf(
            // Finished before service — exclude
            MaintenanceMileageUseCase.LoadInput("T-1", "1", 5_000.0, "2026-07-20", actualFinishDate = "2026-07-22"),
            // Finished on service day — include
            MaintenanceMileageUseCase.LoadInput("T-2", "2", 1_778.0, "2026-07-24", actualFinishDate = "2026-07-24"),
            // Finished after — include
            MaintenanceMileageUseCase.LoadInput("T-3", "3", 1_502.0, "2026-07-28", actualFinishDate = "2026-07-28"),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 864_000.0,
            targetInterval = 20_000.0,
            serviceDate = "2026-07-24",
            loads = loads,
        )
        assertEquals(3_280.0, snap.totalDrivenMiles, 0.01)
        assertEquals(3_280.0, snap.milesSinceService, 0.01)
        assertEquals(867_280.0, snap.currentOdometer, 0.01)
        assertEquals(884_000.0, snap.targetOdometer, 0.01)
        assertEquals(16_720.0, snap.remainingMiles, 0.01)
        assertEquals(2, snap.loadCount)
        assertFalse(snap.isUrgent)
    }

    @Test
    fun calculate_usesLastDelMillisWhenNoActualFinish() {
        val zone = java.time.ZoneId.systemDefault()
        val endMs = LocalDate.of(2026, 7, 26)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val loads = listOf(
            MaintenanceMileageUseCase.LoadInput(
                tripId = "T-DEL",
                id = "x",
                miles = 900.0,
                date = "2026-07-24",
                lastDelMillis = endMs,
            ),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 100_000.0,
            targetInterval = 1_000.0,
            serviceDate = "2026-07-25",
            loads = loads,
        )
        assertEquals(900.0, snap.totalDrivenMiles, 0.01)
        assertEquals(1, snap.loadCount)
    }

    @Test
    fun calculate_backdatedLoadAfterServiceRecounts() {
        val base = listOf(
            MaintenanceMileageUseCase.LoadInput("T-A", "a", 1_000.0, "2026-08-01", actualFinishDate = "2026-08-01"),
        )
        val withBackdate = base + MaintenanceMileageUseCase.LoadInput(
            "T-B", "b", 500.0, "2026-07-30", actualFinishDate = "2026-07-30",
        )
        val service = "2026-07-24"
        val before = MaintenanceMileageUseCase.calculate(800_000.0, 20_000.0, service, base)
        val after = MaintenanceMileageUseCase.calculate(800_000.0, 20_000.0, service, withBackdate)
        assertEquals(1_000.0, before.totalDrivenMiles, 0.01)
        assertEquals(1_500.0, after.totalDrivenMiles, 0.01)
    }

    @Test
    fun calculate_urgentWhenRemainingNonPositive() {
        val loads = listOf(
            MaintenanceMileageUseCase.LoadInput("T-1", "1", 20_000.0, "2026-08-01", actualFinishDate = "2026-08-01"),
        )
        val snap = MaintenanceMileageUseCase.calculate(100_000.0, 20_000.0, "2026-07-01", loads)
        assertTrue(snap.isUrgent)
        assertEquals(0.0, snap.remainingMiles, 0.01)
        assertEquals(1f, snap.progressFraction, 0.001f)
    }

    @Test
    fun dedupesByTripId() {
        val loads = listOf(
            MaintenanceMileageUseCase.LoadInput("T-1", "a", 800.0, "2026-07-25", actualFinishDate = "2026-07-25"),
            MaintenanceMileageUseCase.LoadInput("T-1", "b", 800.0, "2026-07-25", actualFinishDate = "2026-07-25"),
        )
        val snap = MaintenanceMileageUseCase.calculate(0.0, 5_000.0, "2026-07-24", loads)
        assertEquals(800.0, snap.totalDrivenMiles, 0.01)
        assertEquals(1, snap.loadCount)
    }

    @Test
    fun progressForTask_mapsFields() {
        val task = MaintenanceTask(
            title = "Oil",
            startDate = "2026-07-24",
            reminderType = MaintenanceReminderType.MILES,
            intervalMiles = 20_000.0,
            odometerAtStart = 864_000.0,
        )
        val progress = MaintenanceMileageUseCase.progressForTask(
            task,
            listOf(
                MaintenanceMileageUseCase.LoadInput("T-1", "1", 2_000.0, "2026-07-28", "2026-07-28"),
            ),
        )
        assertEquals(2_000.0, progress.milesDrivenSinceStart, 0.01)
        assertEquals(866_000.0, progress.estimatedOdometer!!, 0.01)
        assertEquals(884_000.0, progress.targetOdometer!!, 0.01)
        assertEquals(18_000.0, progress.milesRemaining!!, 0.01)
        assertFalse(progress.isDue)
    }
}
