package com.truckerload.domain.tax

import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.model.Load
import java.time.LocalDate

/**
 * IRS-style trucker per-diem estimate. Days are the same on-road windows as the
 * weekly goal: pickup through the load's own finish, cut to the next pickup when
 * that comes first, not past today, each calendar day once, at [DAILY_RATE] USD.
 */
object PerDiemCalculator {
    const val DAILY_RATE: Double = 69.0

    /** Unique YYYY-MM-DD dates covered by [loads] that fall in [year]. */
    fun uniqueOnDutyDates(
        loads: List<Load>,
        year: Int,
        today: LocalDate = LocalDate.now(),
    ): Set<String> {
        val prefix = "%04d-".format(year)
        return LoadYieldCalculator.coveredDates(loads, today)
            .asSequence()
            .map { it.toString() }
            .filter { it.startsWith(prefix) }
            .toSet()
    }

    fun dayCount(loads: List<Load>, year: Int): Int =
        uniqueOnDutyDates(loads, year).size

    fun amount(dayCount: Int): Double = dayCount * DAILY_RATE

    fun amount(loads: List<Load>, year: Int): Double =
        amount(dayCount(loads, year))
}
