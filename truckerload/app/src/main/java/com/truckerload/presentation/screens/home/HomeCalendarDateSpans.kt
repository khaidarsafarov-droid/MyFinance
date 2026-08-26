package com.truckerload.presentation.screens.home

import com.truckerload.data.local.entities.LoadDateSpan
import java.util.Calendar

/**
 * Expand light load date spans into calendar day keys (yyyy-MM-dd)
 * without hydrating stops.
 */
internal object HomeCalendarDateSpans {
    private const val MAX_DAYS_PER_SPAN = 60

    fun toDateKeys(spans: List<LoadDateSpan>): Set<String> {
        val dates = linkedSetOf<String>()
        for (span in spans) {
            val start = span.startDate.take(10)
            if (start.length < 10) continue
            dates.add(start)
            val end = span.endDate?.take(10)?.takeIf { it.length >= 10 && it >= start } ?: start
            if (end == start) continue
            val p1 = start.split("-").mapNotNull { it.toIntOrNull() }
            val p2 = end.split("-").mapNotNull { it.toIntOrNull() }
            if (p1.size != 3 || p2.size != 3) continue
            val cal = Calendar.getInstance()
            cal.set(p1[0], p1[1] - 1, p1[2])
            val endCal = Calendar.getInstance()
            endCal.set(p2[0], p2[1] - 1, p2[2])
            var guard = 0
            while (!cal.after(endCal) && guard < MAX_DAYS_PER_SPAN) {
                dates.add(
                    "%04d-%02d-%02d".format(
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH) + 1,
                        cal.get(Calendar.DAY_OF_MONTH),
                    ),
                )
                cal.add(Calendar.DAY_OF_YEAR, 1)
                guard++
            }
        }
        return dates
    }
}
