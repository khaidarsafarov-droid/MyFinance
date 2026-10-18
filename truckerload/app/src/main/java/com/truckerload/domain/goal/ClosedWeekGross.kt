package com.truckerload.domain.goal

import com.truckerload.domain.model.Load
import com.truckerload.domain.money.sum
import com.truckerload.utils.isLoadInWeek

/**
 * Closed-week gross from the same in-memory journal the current-week goal already walks.
 * A persisted `weekNumber`/`year` sum is not used: it drops rows whose reporting week
 * comes from the pickup or a later finish (including `weekNumber = 0`).
 */
object ClosedWeekGross {
    fun dollars(loads: List<Load>, weekNumber: Int, year: Int): Double =
        loads
            .filter { isLoadInWeek(it, weekNumber, year) }
            .map { it.totalRate }
            .sum()
            .cents / 100.0
}
