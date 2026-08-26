package com.truckerload.domain.week

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.di.userComponentManager
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.formatIsoDate
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.getWeekRange

/**
 * Rewrites persisted weekNumber/year (and diesel/paycheck week labels) after the
 * driver changes the loads or diesel week-start day. Does not bump [Diesel.addedAt]
 * or [Paycheck.addedAt].
 */
object WeekStartRebinder {

    suspend fun rebindIfPossible(context: Context) {
        val app = context.applicationContext
        val session = runCatching { app.userComponentManager().currentOrNull() }.getOrNull()
        if (session != null) {
            rebind(session.loadRepository, session.dieselRepository, session.paycheckRepository)
        } else {
            val db = AppDatabase.getInstanceForActiveUser(app) ?: return
            rebind(LoadRepository(db), DieselRepository(db), PaycheckRepository(db))
        }
    }

    suspend fun rebind(
        loadRepository: LoadRepository,
        dieselRepository: DieselRepository,
        paycheckRepository: PaycheckRepository,
    ) {
        loadRepository.refreshReportingWeeks()
        dieselRepository.replaceReportingWeeks(recomputeDiesel(dieselRepository.getAllDieselOnce()))
        paycheckRepository.replaceReportingWeeks(recomputePaychecks(paycheckRepository.getAllPaychecksOnce()))
    }

    fun recomputeDiesel(entries: List<Diesel>): List<Diesel> {
        val firstDay = WeekStartRuntime.diesel
        return entries.mapNotNull { diesel ->
            val (weekNumber, year) = getWeekNumberAndYearFromTimestamp(diesel.addedAt, firstDay)
            val (start, end, label) = getWeekRange(weekNumber, year, firstDay)
            if (
                diesel.weekNumber == weekNumber &&
                diesel.year == year &&
                diesel.weekStartDate == start &&
                diesel.weekEndDate == end &&
                diesel.weekLabel == label
            ) {
                null
            } else {
                diesel.copy(
                    weekNumber = weekNumber,
                    year = year,
                    weekStartDate = start,
                    weekEndDate = end,
                    weekLabel = label,
                )
            }
        }
    }

    fun recomputePaychecks(entries: List<Paycheck>): List<Paycheck> {
        val firstDay = WeekStartRuntime.loads
        return entries.mapNotNull { paycheck ->
            val (weekNumber, year) = paycheckWeek(paycheck, firstDay)
            val (start, end, label) = getWeekRange(weekNumber, year, firstDay)
            if (
                paycheck.weekNumber == weekNumber &&
                paycheck.year == year &&
                paycheck.weekStartDate == start &&
                paycheck.weekEndDate == end &&
                paycheck.weekLabel == label
            ) {
                null
            } else {
                paycheck.copy(
                    weekNumber = weekNumber,
                    year = year,
                    weekStartDate = start,
                    weekEndDate = end,
                    weekLabel = label,
                )
            }
        }
    }

    fun dieselIsoInRange(addedAt: Long, startIso: String, endIso: String): Boolean {
        val iso = formatIsoDate(addedAt)
        return iso >= startIso && iso <= endIso
    }

    private fun paycheckWeek(paycheck: Paycheck, firstDay: WeekStartDay): Pair<Int, Int> {
        val anchor = paycheck.weekStartDate.takeIf { it.length >= 10 }
        return if (anchor != null) {
            getWeekNumberAndYearFromDate(anchor, firstDay)
        } else {
            getWeekNumberAndYearFromTimestamp(paycheck.addedAt, firstDay)
        }
    }
}
