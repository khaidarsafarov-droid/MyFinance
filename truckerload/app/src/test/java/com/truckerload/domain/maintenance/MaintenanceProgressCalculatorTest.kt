package com.truckerload.domain.maintenance

import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceProgressCalculatorTest {

    @Test
    fun miles_notDueUntilIntervalReached() {
        val task = MaintenanceTask(
            title = "Oil",
            startDate = "2026-01-01",
            reminderType = MaintenanceReminderType.MILES,
            intervalMiles = 5000.0,
            odometerAtStart = 100_000.0,
        )
        val mid = MaintenanceProgressCalculator.progress(task, milesDrivenSinceStart = 2000.0)
        assertFalse(mid.isDue)
        assertEquals(3000.0, mid.milesRemaining!!, 0.01)
        assertEquals(102_000.0, mid.estimatedOdometer!!, 0.01)

        val due = MaintenanceProgressCalculator.progress(task, milesDrivenSinceStart = 5000.0)
        assertTrue(due.isDue)
        assertEquals(0.0, due.milesRemaining!!, 0.01)
        assertTrue(MaintenanceProgressCalculator.shouldNotify(due))
    }

    @Test
    fun date_dueOnOrAfterDueDate() {
        val task = MaintenanceTask(
            title = "Inspection",
            startDate = "2026-01-01",
            reminderType = MaintenanceReminderType.DATE,
            dueDate = "2026-07-01",
        )
        val before = MaintenanceProgressCalculator.progress(
            task,
            milesDrivenSinceStart = 0.0,
            today = LocalDate.of(2026, 6, 30),
        )
        assertFalse(before.isDue)
        assertEquals(1L, before.daysRemaining)

        val onDay = MaintenanceProgressCalculator.progress(
            task,
            milesDrivenSinceStart = 0.0,
            today = LocalDate.of(2026, 7, 1),
        )
        assertTrue(onDay.isDue)
    }

    @Test
    fun shouldNotify_skipsCompletedOrAlreadyNotified() {
        val task = MaintenanceTask(
            title = "Oil",
            startDate = "2026-01-01",
            reminderType = MaintenanceReminderType.MILES,
            intervalMiles = 100.0,
            odometerAtStart = 0.0,
            isCompleted = true,
        )
        val progress = MaintenanceProgressCalculator.progress(task, 200.0)
        assertTrue(progress.isDue)
        assertFalse(MaintenanceProgressCalculator.shouldNotify(progress))
    }
}
