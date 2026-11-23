package com.truckerload.domain.store

import com.truckerload.domain.model.Diesel

/** Diesel journal. Room implements this in the data layer. */
interface DieselStore {
    suspend fun getDieselForWeekOnce(weekNumber: Int, year: Int): List<Diesel>

    suspend fun insertDiesel(diesel: Diesel)

    suspend fun replaceWeekFills(weekNumber: Int, year: Int, fills: List<Diesel>)

    suspend fun getAllDieselOnce(): List<Diesel>

    suspend fun replaceReportingWeeks(updated: List<Diesel>)

    suspend fun deleteDiesel(id: Int)
}
