package com.truckerload.domain.assistant

import com.truckerload.domain.goal.GoalMoneyMath
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.utils.canonicalDateString
import com.truckerload.utils.dateStringToStartOfDayMillis
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.getWeekRange
import java.util.Calendar

/**
 * Builds diesel / paycheck rows the same way as the manual Add screens:
 * week metadata from [recordedAt] (or explicit week for paycheck), positive amounts only.
 */
object JournalEntryFactory {
    fun diesel(
        amount: Double,
        gallons: Double?,
        date: String?,
        nowMillis: Long = System.currentTimeMillis(),
    ): Diesel? {
        val money = GoalMoneyMath.roundMoney(amount)
        if (money <= 0.0) return null
        val gallonsValue = gallons?.let { GoalMoneyMath.roundMoney(it) }?.takeIf { it > 0.0 }
        val recordedAt = recordedAtMillis(date, nowMillis)
        val (weekNumber, year) = getWeekNumberAndYearFromTimestamp(recordedAt)
        val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
        val ppg = if (gallonsValue != null && gallonsValue > 0.0) {
            GoalMoneyMath.roundMoney(money / gallonsValue)
        } else {
            null
        }
        return Diesel(
            id = 0,
            weekNumber = weekNumber,
            year = year,
            weekLabel = weekLabel,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            totalAmount = money,
            gallons = gallonsValue,
            pricePerGallon = ppg,
            location = null,
            rawExtractedText = "",
            sourceFileName = null,
            addedAt = recordedAt,
        )
    }

    fun paycheck(
        amount: Double,
        weekNumber: Int?,
        year: Int?,
        nowMillis: Long = System.currentTimeMillis(),
    ): Paycheck? {
        val money = GoalMoneyMath.roundMoney(amount)
        if (money <= 0.0) return null
        val (resolvedWeek, resolvedYear) = resolveWeek(weekNumber, year, nowMillis)
        val (weekStart, weekEnd, weekLabel) = getWeekRange(resolvedWeek, resolvedYear)
        return Paycheck(
            id = 0,
            weekNumber = resolvedWeek,
            year = resolvedYear,
            weekLabel = weekLabel,
            weekStartDate = weekStart,
            weekEndDate = weekEnd,
            driverName = null,
            grossAmount = null,
            netAmount = money,
            rawExtractedText = "",
            sourceFileName = null,
            addedAt = nowMillis,
        )
    }

    fun resolveWeek(
        weekNumber: Int?,
        year: Int?,
        nowMillis: Long,
    ): Pair<Int, Int> {
        val current = getWeekNumberAndYearFromTimestamp(nowMillis)
        val resolvedWeek = weekNumber?.takeIf { it in 1..53 }
        val resolvedYear = year?.takeIf { it in 1970..2100 }
        return when {
            resolvedWeek != null && resolvedYear != null -> resolvedWeek to resolvedYear
            resolvedWeek != null -> resolvedWeek to current.second
            else -> current
        }
    }

    fun recordedAtMillis(date: String?, nowMillis: Long): Long {
        val canonical = canonicalDateString(date) ?: return nowMillis
        val start = dateStringToStartOfDayMillis(canonical) ?: return nowMillis
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dateCal = Calendar.getInstance().apply { timeInMillis = start }
        dateCal.set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
        dateCal.set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE))
        dateCal.set(Calendar.SECOND, 0)
        dateCal.set(Calendar.MILLISECOND, 0)
        return dateCal.timeInMillis
    }
}
