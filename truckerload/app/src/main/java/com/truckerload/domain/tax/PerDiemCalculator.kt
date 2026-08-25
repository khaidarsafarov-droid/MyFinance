package com.truckerload.domain.tax

import com.truckerload.domain.model.Load
import com.truckerload.utils.getLoadDateRange

/**
 * IRS-style trucker per-diem estimate: each unique calendar day on a load
 * (PU→DEL / finish) counts once at [DAILY_RATE] USD.
 *
 * Overlapping loads on the same day do not double-count.
 */
object PerDiemCalculator {
    const val DAILY_RATE: Double = 69.0

    /** Unique YYYY-MM-DD dates covered by [loads] that fall in [year]. */
    fun uniqueOnDutyDates(loads: List<Load>, year: Int): Set<String> {
        val prefix = "%04d-".format(year)
        return loads
            .asSequence()
            .flatMap { getLoadDateRange(it).asSequence() }
            .filter { it.startsWith(prefix) }
            .toSet()
    }

    fun dayCount(loads: List<Load>, year: Int): Int =
        uniqueOnDutyDates(loads, year).size

    fun amount(dayCount: Int): Double = dayCount * DAILY_RATE

    fun amount(loads: List<Load>, year: Int): Double =
        amount(dayCount(loads, year))
}
