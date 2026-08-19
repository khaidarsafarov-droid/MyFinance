package com.truckerload.domain.social

import com.truckerload.utils.getCurrentWeekNumberAndYear
import com.truckerload.utils.getWeekRange
import java.time.LocalDate
import java.time.ZoneId

data class CommunityWeekWindow(
    val week: Int,
    val year: Int,
    val startMillis: Long,
    val endMillis: Long,
) {
    companion object {
        fun current(zoneId: ZoneId = ZoneId.systemDefault()): CommunityWeekWindow {
            val (week, year) = getCurrentWeekNumberAndYear()
            val (startIso, endIso, _) = getWeekRange(week, year)
            val start = LocalDate.parse(startIso).atStartOfDay(zoneId).toInstant().toEpochMilli()
            val end = LocalDate.parse(endIso).plusDays(1).atStartOfDay(zoneId).toInstant()
                .toEpochMilli() - 1
            return CommunityWeekWindow(
                week = week,
                year = year,
                startMillis = start,
                endMillis = end
            )
        }
    }
}
