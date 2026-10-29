package com.truckerload.presentation.screens.gallery

import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.domain.model.Load
import java.text.DateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class GalleryKind {
    PHOTOS,
    SCANS,
    ;

    val route: String
        get() = name.lowercase()

    companion object {
        fun fromRoute(value: String?): GalleryKind =
            entries.firstOrNull { it.route == value } ?: PHOTOS
    }
}

enum class GalleryFilter {
    ALL,
    TODAY,
    THIS_WEEK,
    BY_LOAD,
}

/** One stored photo or scan. [loadId] is null when the file is not attached. */
data class GalleryMediaRef(
    val id: String,
    val loadId: String?,
    val timestamp: Long,
    val filePath: String,
    val fileName: String,
)

/** Load fields the gallery card needs. Stops and money stay out of this list. */
data class GalleryLoadInfo(
    val id: String,
    val tripId: String,
    val origin: String,
    val destination: String,
    val route: String,
    val dateIso: String,
)

data class GalleryLoadCard(
    val loadKey: String,
    val tripId: String,
    val route: String,
    val dateLabel: String,
    val count: Int,
    val thumbnailPaths: List<String>,
    val itemIds: List<String>,
    val newestTimestamp: Long,
)

data class GalleryControls(
    val tab: GalleryKind = GalleryKind.PHOTOS,
    val filter: GalleryFilter = GalleryFilter.ALL,
    val query: String = "",
)

/**
 * Groups photos or scans into compact load cards and applies All / Today /
 * This week / By load. The same function serves both tabs.
 */
object GalleryCatalog {
    const val UNLINKED = "unlinked"
    const val THUMBNAIL_CAP = 4

    fun buildCards(
        items: List<GalleryMediaRef>,
        loads: List<GalleryLoadInfo>,
        filter: GalleryFilter,
        query: String,
        now: Calendar,
        unlinkedLabel: String,
        locale: Locale = Locale.getDefault(),
    ): List<GalleryLoadCard> {
        val knownIds = loads.mapTo(HashSet()) { it.id }
        val timed = items.filter { matchesTime(it.timestamp, filter, now) }
        val loadsById = loads.associateBy { it.id }
        val grouped = timed.groupBy { groupKey(it, knownIds) }
        val cards = grouped.map { (key, group) ->
            val sorted = group.sortedByDescending { it.timestamp }
            val info = loadsById[key]
            val newest = sorted.first().timestamp
            GalleryLoadCard(
                loadKey = key,
                tripId = when {
                    key == UNLINKED -> unlinkedLabel
                    info != null && info.tripId.isNotBlank() -> info.tripId
                    else -> key.take(8)
                },
                route = info?.let { displayRoute(it.origin, it.destination, it.route) }.orEmpty(),
                dateLabel = cardDate(info, newest, locale),
                count = sorted.size,
                thumbnailPaths = sorted.take(THUMBNAIL_CAP).map { it.filePath },
                itemIds = sorted.map { it.id },
                newestTimestamp = newest,
            )
        }.sortedByDescending { it.newestTimestamp }
        if (filter != GalleryFilter.BY_LOAD) return cards
        val needle = query.trim().lowercase(locale)
        if (needle.isEmpty()) return cards
        return cards.filter { card ->
            cardMatches(card, grouped[card.loadKey].orEmpty(), needle, unlinkedLabel, locale)
        }
    }

    fun itemsForLoad(
        items: List<GalleryMediaRef>,
        loadKey: String,
        knownLoadIds: Set<String>,
    ): List<GalleryMediaRef> =
        items.filter { groupKey(it, knownLoadIds) == loadKey }
            .sortedByDescending { it.timestamp }

    fun groupKey(item: GalleryMediaRef, knownLoadIds: Set<String>): String {
        val id = item.loadId?.takeIf { it.isNotBlank() } ?: return UNLINKED
        if (knownLoadIds.isNotEmpty() && id !in knownLoadIds) return UNLINKED
        return id
    }

    fun displayRoute(origin: String, destination: String, storedRoute: String): String {
        val from = origin.trim()
        val to = destination.trim()
        if (from.isNotEmpty() && to.isNotEmpty()) return "$from → $to"
        if (storedRoute.isNotBlank()) return storedRoute.trim()
        return from.ifEmpty { to }
    }

    private fun matchesTime(timestamp: Long, filter: GalleryFilter, now: Calendar): Boolean =
        when (filter) {
            GalleryFilter.ALL, GalleryFilter.BY_LOAD -> true
            GalleryFilter.TODAY -> PhotoGalleryTimeBounds.isInToday(timestamp, now)
            GalleryFilter.THIS_WEEK -> PhotoGalleryTimeBounds.isInThisWeek(timestamp, now)
        }

    private fun cardDate(info: GalleryLoadInfo?, newestMillis: Long, locale: Locale): String {
        val iso = info?.dateIso?.trim().orEmpty()
        if (iso.length >= 10) return formatIsoDate(iso, locale)
        return formatMillis(newestMillis, locale)
    }

    private fun cardMatches(
        card: GalleryLoadCard,
        items: List<GalleryMediaRef>,
        needle: String,
        unlinkedLabel: String,
        locale: Locale,
    ): Boolean {
        if (card.tripId.lowercase(locale).contains(needle)) return true
        if (card.route.lowercase(locale).contains(needle)) return true
        if (card.loadKey == UNLINKED && unlinkedLabel.lowercase(locale).contains(needle)) return true
        return items.any { it.fileName.lowercase(locale).contains(needle) }
    }

    fun formatIsoDate(iso: String, locale: Locale): String {
        val day = iso.trim().take(10)
        val parsed = runCatching { LocalDate.parse(day) }.getOrNull() ?: return iso.trim()
        return parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }

    fun formatMillis(millis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale).format(Date(millis))
}

internal fun Load.toGalleryLoadInfo(): GalleryLoadInfo = GalleryLoadInfo(
    id = id,
    tripId = tripId,
    origin = firstPuCityState.ifBlank { pointA },
    destination = lastDelCityState.ifBlank { pointB },
    route = route,
    dateIso = date,
)

internal fun PhotoEntity.asGalleryMedia(): GalleryMediaRef = GalleryMediaRef(
    id = id,
    loadId = loadId?.takeIf { it.isNotBlank() },
    timestamp = timestamp,
    filePath = filePath,
    fileName = fileName,
)

internal fun ScanEntity.asGalleryMedia(): GalleryMediaRef = GalleryMediaRef(
    id = id,
    loadId = loadId?.takeIf { it.isNotBlank() },
    timestamp = timestamp,
    filePath = filePath,
    fileName = fileName,
)
