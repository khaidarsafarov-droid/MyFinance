package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceMileageUseCaseTest {

    private val today = LocalDate.of(2026, 7, 28)
    private val createdAt = 1_720_000_000_000L

    @Test
    fun calculate_sumsLoadsWithEndDateOnOrAfterService_notStrictGreater() {
        val loads = listOf(
            load("T-OLD", 5_000.0, "2026-07-20", "2026-07-22", parsedAt = createdAt + 1),
            load("T-SAME-DAY", 1_778.0, "2026-07-24", "2026-07-24", parsedAt = createdAt + 1),
            load("T-B", 1_502.0, "2026-07-28", "2026-07-28", parsedAt = createdAt + 2),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 878_030L,
            targetInterval = 20_000L,
            serviceDate = "2026-07-24",
            loads = loads,
            today = today,
        )
        assertEquals(3_280L, snap.totalDrivenMiles)
        assertEquals(3_280L, snap.milesSinceService)
        assertEquals(881_310L, snap.currentOdometer)
        assertEquals(898_030L, snap.targetOdometer)
        assertEquals(16_720L, snap.remainingMiles)
        assertEquals(2, snap.loadCount)
        assertFalse(snap.isUrgent)
    }

    @Test
    fun calculate_screenshotCase_oilChangeCountsJuly28Load() {
        // ТО «Замена масло» 2026-07-24, interval 20_000, load T-113Q6BJDT = 1_502 mi end 07-28.
        val loads = listOf(
            load("T-113Q6BJDT", 1_502.0, "2026-07-28", "2026-07-28", parsedAt = createdAt - 1),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 878_030L,
            targetInterval = 20_000L,
            serviceDate = "2026-07-24",
            loads = loads,
            today = today,
            baselineRecordedAtMs = createdAt, // reminder created after load was already in journal
        )
        assertEquals(1_502L, snap.milesSinceService)
        assertEquals(1, snap.loadCount)
        assertEquals(18_498L, snap.remainingMiles)
        assertEquals(879_532L, snap.currentOdometer)
        assertFalse(snap.isUrgent)
        assertEquals(1_502f / 20_000f, snap.progressFraction, 0.0001f)
    }

    @Test
    fun calculate_countsJournalLoadsPresentBeforeReminder_whenEndOnOrAfterService() {
        // Baseline odometer is at service time — post-service trips must count even if
        // they were already in Room when the reminder row was inserted.
        val loads = listOf(
            load("T-NEW", 1_500.0, "2026-07-27", "2026-07-27", parsedAt = createdAt - 86_400_000L),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 878_030L,
            targetInterval = 20_000L,
            serviceDate = "2026-07-24",
            loads = loads,
            today = today,
            baselineRecordedAtMs = createdAt,
        )
        assertEquals(1_500L, snap.milesSinceService)
        assertEquals(879_530L, snap.currentOdometer)
        assertEquals(1, snap.loadCount)
    }

    @Test
    fun calculate_excludesLoadsFinishedBeforeService() {
        val loads = listOf(
            load("T-BEFORE", 9_000.0, "2026-07-20", "2026-07-23", parsedAt = createdAt + 1),
            load("T-OK", 800.0, "2026-07-26", "2026-07-26", parsedAt = createdAt + 1),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 100_000L,
            targetInterval = 5_000L,
            serviceDate = "2026-07-24",
            loads = loads,
            today = today,
        )
        assertEquals(800L, snap.milesSinceService)
        assertEquals(1, snap.loadCount)
    }

    @Test
    fun calculate_ignoresFarFutureMisdatedEndDates() {
        val loads = listOf(
            load("T-FUT", 200_000.0, "2026-12-01", "2026-12-01", parsedAt = createdAt + 1),
            load("T-OK", 800.0, "2026-07-26", "2026-07-26", parsedAt = createdAt + 1),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 100_000L,
            targetInterval = 5_000L,
            serviceDate = "2026-07-24",
            loads = loads,
            today = today,
        )
        assertEquals(800L, snap.milesSinceService)
    }

    @Test
    fun calculate_sameTruckingWeekFinishCountsEvenIfDeviceClockIsEarlier() {
        // Device "today" is Wed 07-22; load finishes Sat 07-25 (same Sun–Sat week) → still counts.
        val midWeek = LocalDate.of(2026, 7, 22)
        val loads = listOf(
            load("T-WEEK", 1_502.0, "2026-07-25", "2026-07-25", parsedAt = createdAt + 1),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 100_000L,
            targetInterval = 20_000L,
            serviceDate = "2026-07-20",
            loads = loads,
            today = midWeek,
        )
        assertEquals(1_502L, snap.milesSinceService)
        assertEquals(18_498L, snap.remainingMiles)
        assertEquals(1, snap.loadCount)
    }

    @Test
    fun calculate_usesFinishDateNotStart_whenTripSpansService() {
        // Started before ТО, finished after → must count (endDate >= serviceDate).
        val loads = listOf(
            load("T-SPAN", 1_502.0, "2026-07-22", "2026-07-28", parsedAt = createdAt + 1),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 878_030L,
            targetInterval = 20_000L,
            serviceDate = "2026-07-24",
            loads = loads,
            today = today,
        )
        assertEquals(1_502L, snap.milesSinceService)
        assertEquals(18_498L, snap.remainingMiles)
    }

    @Test
    fun calculate_urgentWhenRemainingNonPositive() {
        val loads = listOf(
            load("T-1", 12_000.0, "2026-07-27", "2026-07-27", parsedAt = createdAt + 1),
            load("T-2", 8_000.0, "2026-07-28", "2026-07-28", parsedAt = createdAt + 2),
        )
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 100_000L,
            targetInterval = 20_000L,
            serviceDate = "2026-07-01",
            loads = loads,
            today = today,
        )
        assertTrue(snap.isUrgent)
        assertEquals(0L, snap.remainingMiles)
        assertEquals(1f, snap.progressFraction, 0.001f)
    }

    @Test
    fun milesAsLong_neverConcatenates() {
        assertEquals(1_500L, MaintenanceMileageUseCase.milesAsLong(1500.0))
        assertEquals(1_778L, MaintenanceMileageUseCase.milesAsLong(1778.4))
        val snap = MaintenanceMileageUseCase.calculate(
            baseOdometer = 10L,
            targetInterval = 100L,
            serviceDate = "2026-07-24",
            loads = listOf(
                load("A", 1_500.0, "2026-07-25", "2026-07-25", parsedAt = createdAt + 1),
                load("B", 1_778.0, "2026-07-26", "2026-07-26", parsedAt = createdAt + 1),
            ),
            today = today,
        )
        assertEquals(3_278L, snap.milesSinceService)
        assertEquals(3_288L, snap.currentOdometer)
    }

    @Test
    fun progressForTask_mapsLongMathToUi() {
        val task = MaintenanceTask(
            title = "Замена масло",
            startDate = "2026-07-24",
            reminderType = MaintenanceReminderType.MILES,
            intervalMiles = 20_000.0,
            odometerAtStart = 878_030.0,
            createdAt = createdAt,
        )
        val progress = MaintenanceMileageUseCase.progressForTask(
            task,
            listOf(load("T-113Q6BJDT", 1_502.0, "2026-07-28", "2026-07-28", parsedAt = createdAt - 10)),
            today = today,
        )
        assertEquals(1_502.0, progress.milesDrivenSinceStart, 0.01)
        assertEquals(879_532.0, progress.estimatedOdometer!!, 0.01)
        assertEquals(898_030.0, progress.targetOdometer!!, 0.01)
        assertEquals(18_498.0, progress.milesRemaining!!, 0.01)
        assertEquals(1, progress.loadsCounted)
        assertFalse(progress.isDue)
    }

    private fun load(
        tripId: String,
        miles: Double,
        date: String,
        finish: String,
        parsedAt: Long,
    ) = MaintenanceMileageUseCase.LoadInput(
        tripId = tripId,
        id = tripId,
        miles = miles,
        date = date,
        actualFinishDate = finish,
        parsedAt = parsedAt,
    )
}
