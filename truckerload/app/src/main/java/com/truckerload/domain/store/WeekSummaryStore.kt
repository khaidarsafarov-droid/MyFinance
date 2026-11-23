package com.truckerload.domain.store

import com.truckerload.domain.model.WeekSummary

/** One week's totals, without the caller opening the database. */
interface WeekSummaryStore {
    suspend fun getWeekSummaryOnce(weekNumber: Int, year: Int): WeekSummary
}
