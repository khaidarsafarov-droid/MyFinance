package com.truckerload.domain.tax

import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.model.Load
import java.time.LocalDate

/**
 * IRS-style trucker per-diem estimate. Days are the same on-road windows as the
 * weekly goal: pickup through the load's own finish, cut to the next pickup when
 * that comes first, not past today, each calendar day once, at [DAILY_RATE] USD.
 *
 * [DAILY_RATE] is the IRS special transportation-industry M&IE rate for travel in the
 * continental US ($80 since Oct 1 2024; Notices 2024-68, 2025-54, 2026-60). Drivers under
 * DOT hours-of-service rules deduct [DEDUCTIBLE_SHARE] of it, so the tax estimate uses
 * [deductibleAmount]; the accountant export lists the full [amount].
 */
object PerDiemCalculator {
    const val DAILY_RATE: Double = 80.0
    const val DEDUCTIBLE_SHARE: Double = 0.8

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

    /** Part of the per diem a DOT-regulated driver can deduct (80% meal limit). */
    fun deductibleAmount(dayCount: Int): Double =
        kotlin.math.round(amount(dayCount) * DEDUCTIBLE_SHARE * 100.0) / 100.0
}
