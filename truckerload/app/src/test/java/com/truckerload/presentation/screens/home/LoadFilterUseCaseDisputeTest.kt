package com.truckerload.presentation.screens.home

import com.truckerload.domain.model.Load
import org.junit.Assert.assertEquals
import org.junit.Test

class LoadFilterUseCaseDisputeTest {

    private val useCase = LoadFilterUseCase()

    @Test
    fun disputeFilter_showsOnlyActiveDisputes() {
        val loads = listOf(
            sampleLoad(id = "1", isDispute = true, disputeCompleted = false),
            sampleLoad(id = "2", isDispute = true, disputeCompleted = true),
            sampleLoad(id = "3", isDispute = false),
        )

        val filtered = useCase.filterLoads(
            loads = loads,
            filter = LoadFilter.DISPUTE,
            searchQuery = "",
            selectedDate = null,
            selectedWeekStart = null,
            selectedWeekEnd = null,
            selectedYear = null,
        )

        assertEquals(1, filtered.size)
        assertEquals("1", filtered.first().id)
    }

    private fun sampleLoad(
        id: String,
        isDispute: Boolean = false,
        disputeCompleted: Boolean = false,
    ) = Load(
        id = id,
        tripId = id,
        date = "2026-07-16",
        totalRate = 1000.0,
        totalMiles = 500.0,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 29,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        isDispute = isDispute,
        disputeResponseDate = if (isDispute) "2026-07-20" else null,
        disputeCompleted = disputeCompleted,
    )
}
