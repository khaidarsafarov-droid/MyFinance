package com.truckerload.domain.attach

import com.truckerload.domain.model.Load
import com.truckerload.utils.isLoadInWeek

/**
 * Pure helpers for the widget Camera/Scan load picker.
 * Quick picks = up to [QUICK_PICK_LIMIT] loads from the current trucking week
 * (newest by load date, then [Load.parsedAt]) — never global `updatedAt` order.
 */
object AttachLoadSelection {
    const val QUICK_PICK_LIMIT = 3

    fun quickPickThisWeek(
        loads: List<Load>,
        weekNumber: Int,
        year: Int,
        limit: Int = QUICK_PICK_LIMIT,
    ): List<Load> {
        if (limit <= 0) return emptyList()
        return loads
            .asSequence()
            .filter { isLoadInWeek(it, weekNumber, year) }
            .sortedWith(compareByDescending<Load> { it.date }.thenByDescending { it.parsedAt })
            .take(limit)
            .toList()
    }

    fun filterBrowse(loads: List<Load>, query: String): List<Load> {
        val sorted = loads.sortedWith(
            compareByDescending<Load> { it.date }.thenByDescending { it.parsedAt },
        )
        val q = query.trim()
        if (q.isEmpty()) return sorted
        val qLower = q.lowercase()
        return sorted.filter { load ->
            load.tripId.lowercase().contains(qLower) ||
                load.pointA.lowercase().contains(qLower) ||
                load.pointB.lowercase().contains(qLower) ||
                load.route.lowercase().contains(qLower) ||
                load.date.contains(q)
        }
    }
}
