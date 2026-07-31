package com.truckerload.domain.friends

import com.truckerload.domain.goal.LoadYieldCalculator
import com.truckerload.domain.model.ActualFinishDate
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.effectiveFinishDate
import com.truckerload.utils.getFirstPickUpMillis
import com.truckerload.utils.getWeekNumberAndYearFromDate
import java.time.LocalDate
import java.time.format.DateTimeParseException

enum class SharedLoadStatus {
    ACTIVE,
    COMPLETED,
    FUTURE,
    UNKNOWN,
}

data class LatLngPoint(
    val lat: Double,
    val lng: Double,
)

data class FriendPresence(
    val userId: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val updatedAtMillis: Long,
    val sharePathEnabled: Boolean,
)

data class FriendActiveRoute(
    val userId: String,
    val displayName: String,
    val loadRef: String?,
    val originLabel: String,
    val destinationLabel: String,
    val origin: LatLngPoint?,
    val destination: LatLngPoint?,
    val startDate: String,
    val endDate: String,
    val status: SharedLoadStatus,
    /** GPS crumbs already driven (past). */
    val trackPoints: List<LatLngPoint>,
)

data class RouteOverlapMatch(
    val friendUserId: String,
    val friendDisplayName: String,
    val reason: String,
    val score: Double,
)

/**
 * Picks the load that is "in progress" for map routing:
 * now ∈ [first PU clock, finish clock], preferring this reporting week's loads.
 */
object ActiveLoadSelector {

    fun startDateIso(load: Load): String =
        load.date.take(10)

    fun endDateIso(load: Load): String =
        load.effectiveFinishDate()?.take(10) ?: load.date.take(10)

    /**
     * Driver marked an explicit finish.
     * - Date-only (legacy): completed for that calendar day and after.
     * - Date+time: completed only once [nowMillis] reaches that clock.
     */
    fun isExplicitlyCompleted(
        load: Load,
        today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val raw = load.actualFinishDate ?: return false
        val finishDay = ActualFinishDate.datePart(raw)?.let { parseDate(it) } ?: return false
        if (today.isAfter(finishDay)) return true
        if (today.isBefore(finishDay)) return false
        // Finish day:
        if (!ActualFinishDate.hasTime(raw)) return true
        val finishMs = ActualFinishDate.toMillis(raw) ?: return true
        return nowMillis >= finishMs
    }

    fun statusFor(
        load: Load,
        today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis(),
    ): SharedLoadStatus {
        if (isExplicitlyCompleted(load, today, nowMillis)) return SharedLoadStatus.COMPLETED
        val start = parseDate(startDateIso(load)) ?: return SharedLoadStatus.UNKNOWN
        val end = parseDate(endDateIso(load)) ?: return SharedLoadStatus.UNKNOWN
        when {
            today.isBefore(start) -> return SharedLoadStatus.FUTURE
            today.isAfter(end) -> return SharedLoadStatus.COMPLETED
        }
        // Before first PU clock → not live yet (e.g. evening PU while waiting midday).
        val startMs = getFirstPickUpMillis(load)
        if (startMs != null && nowMillis < startMs) return SharedLoadStatus.FUTURE
        // After last DEL / finish override clock → route off.
        val finishMs = LoadYieldCalculator.resolveFinishMillis(load)
        if (finishMs != null && nowMillis >= finishMs) return SharedLoadStatus.COMPLETED
        return SharedLoadStatus.ACTIVE
    }

    /**
     * In-progress load for live map / friends share.
     * Looks at this reporting week's loads first (not "last updated"),
     * then picks the chronologically current trip by first-PU time.
     */
    fun selectActive(
        loads: List<Load>,
        today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis(),
    ): Load? {
        val active = loads.filter { statusFor(it, today, nowMillis) == SharedLoadStatus.ACTIVE }
        if (active.isEmpty()) return null
        val (week, year) = reportingWeekFor(today)
        val weekActive = active.filter { it.weekNumber == week && it.year == year }
        // Prefer this week; fall back to any ACTIVE (cross-week trip still on the road).
        val pool = weekActive.ifEmpty { active }
        return pool.maxByOrNull { getFirstPickUpMillis(it) ?: Long.MIN_VALUE }
    }

    fun selectActiveAll(
        loads: List<Load>,
        today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis(),
    ): List<Load> {
        val (week, year) = reportingWeekFor(today)
        return loads
            .filter { statusFor(it, today, nowMillis) == SharedLoadStatus.ACTIVE }
            .sortedWith(
                compareByDescending<Load> { it.weekNumber == week && it.year == year }
                    .thenByDescending { getFirstPickUpMillis(it) ?: Long.MIN_VALUE },
            )
    }

    /**
     * Load used to draw "my path" on the friends / own live map.
     * Only a truly ACTIVE load — never fall back to upcoming FUTURE trips.
     */
    fun selectForMapRoute(
        loads: List<Load>,
        today: LocalDate = LocalDate.now(),
        nowMillis: Long = System.currentTimeMillis(),
    ): Load? = selectActive(loads, today, nowMillis)

    private fun reportingWeekFor(today: LocalDate): Pair<Int, Int> =
        getWeekNumberAndYearFromDate(today.toString())

    private fun parseDate(iso: String): LocalDate? =
        try {
            LocalDate.parse(iso.take(10))
        } catch (_: DateTimeParseException) {
            null
        }
}
