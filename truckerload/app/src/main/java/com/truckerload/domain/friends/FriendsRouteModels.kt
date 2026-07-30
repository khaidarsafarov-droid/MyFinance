package com.truckerload.domain.friends

import com.truckerload.domain.model.Load
import com.truckerload.domain.model.effectiveFinishDate
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
 * today ∈ [startDate, endDate] and not completed.
 */
object ActiveLoadSelector {

    fun startDateIso(load: Load): String =
        load.date.take(10)

    fun endDateIso(load: Load): String =
        load.effectiveFinishDate()?.take(10) ?: load.date.take(10)

    fun isExplicitlyCompleted(load: Load, today: LocalDate = LocalDate.now()): Boolean {
        val finish = load.actualFinishDate?.trim()?.take(10) ?: return false
        return try {
            !LocalDate.parse(finish).isAfter(today)
        } catch (_: DateTimeParseException) {
            false
        }
    }

    fun statusFor(
        load: Load,
        today: LocalDate = LocalDate.now(),
    ): SharedLoadStatus {
        if (isExplicitlyCompleted(load, today)) return SharedLoadStatus.COMPLETED
        val start = parseDate(startDateIso(load)) ?: return SharedLoadStatus.UNKNOWN
        val end = parseDate(endDateIso(load)) ?: return SharedLoadStatus.UNKNOWN
        return when {
            today.isBefore(start) -> SharedLoadStatus.FUTURE
            today.isAfter(end) -> SharedLoadStatus.COMPLETED
            else -> SharedLoadStatus.ACTIVE
        }
    }

    fun selectActive(
        loads: List<Load>,
        today: LocalDate = LocalDate.now(),
    ): Load? =
        loads
            .filter { statusFor(it, today) == SharedLoadStatus.ACTIVE }
            .maxByOrNull { it.updatedAt }

    fun selectActiveAll(
        loads: List<Load>,
        today: LocalDate = LocalDate.now(),
    ): List<Load> =
        loads.filter { statusFor(it, today) == SharedLoadStatus.ACTIVE }
            .sortedByDescending { it.updatedAt }

    private fun parseDate(iso: String): LocalDate? =
        try {
            LocalDate.parse(iso.take(10))
        } catch (_: DateTimeParseException) {
            null
        }
}
