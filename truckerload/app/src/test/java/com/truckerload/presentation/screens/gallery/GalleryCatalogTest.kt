package com.truckerload.presentation.screens.gallery

import com.truckerload.domain.week.WeekStartDay
import com.truckerload.domain.week.WeekStartRuntime
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GalleryCatalogTest {

    private val locale = Locale.US

    @Before
    fun sundayWeek() {
        WeekStartRuntime.install(WeekStartDay.SUNDAY, WeekStartDay.SUNDAY)
    }

    @Test
    fun allGroupsLoadsUnlinkedAndUnknownIds() {
        val now = cal(2026, Calendar.OCTOBER, 20, 12, 0)
        val loads = listOf(
            load("l1", "T-116KYL6KW", "Garner, NC", "Dallas, TX", "2026-10-20"),
            load("l2", "T-88ATLANTA", "Chicago, IL", "Atlanta, GA", "2026-10-18"),
        )
        val items = listOf(
            media("p1", "l1", now.timeInMillis, "a.jpg"),
            media("p2", "l1", now.timeInMillis - DAY, "b.jpg"),
            media("p3", "l1", now.timeInMillis - 2 * DAY, "c.jpg"),
            media("p4", "l1", now.timeInMillis - 3 * DAY, "d.jpg"),
            media("p5", "l1", now.timeInMillis - 4 * DAY, "e.jpg"),
            media("s1", "l2", now.timeInMillis - DAY, "rate.pdf"),
            media("orphan", null, now.timeInMillis - 2 * DAY, "loose.jpg"),
            media("gone", "deleted-load", now.timeInMillis - 3 * DAY, "old.jpg"),
        )
        val cards = GalleryCatalog.buildCards(
            items, loads, GalleryFilter.ALL, "", now, "Unlinked", locale,
        )
        assertEquals(listOf("l1", "l2", GalleryCatalog.UNLINKED), cards.map { it.loadKey })
        val first = cards[0]
        assertEquals("T-116KYL6KW", first.tripId)
        assertEquals("Garner, NC → Dallas, TX", first.route)
        assertEquals("Oct 20, 2026", first.dateLabel)
        assertEquals(5, first.count)
        assertEquals(GalleryCatalog.THUMBNAIL_CAP, first.thumbnailPaths.size)
        assertEquals(listOf("a.jpg", "b.jpg", "c.jpg", "d.jpg"), first.thumbnailPaths.map { it.substringAfterLast('/') })
        assertEquals(listOf("p1", "p2", "p3", "p4", "p5"), first.itemIds)
        val unlinked = cards[2]
        assertEquals("Unlinked", unlinked.tripId)
        assertEquals("", unlinked.route)
        assertEquals(2, unlinked.count)
        assertEquals(listOf("orphan", "gone"), unlinked.itemIds)
    }

    @Test
    fun todayAndThisWeekKeepOnlyMatchingFiles() {
        val now = cal(2026, Calendar.JULY, 22, 12, 0)
        val loads = listOf(load("l1", "T-1", "A", "B", "2026-07-22"))
        val today = media("today", "l1", now.timeInMillis, "today.jpg")
        val yesterday = media("yesterday", "l1", cal(2026, Calendar.JULY, 21, 23, 59).timeInMillis, "y.jpg")
        val weekStart = media("start", "l1", PhotoGalleryTimeBounds.startOfWeek(now), "start.jpg")
        val beforeWeek = media(
            "before",
            "l1",
            PhotoGalleryTimeBounds.startOfWeek(now) - 1,
            "before.jpg",
        )
        val todayCards = GalleryCatalog.buildCards(
            listOf(today, yesterday), loads, GalleryFilter.TODAY, "", now, "Unlinked", locale,
        )
        assertEquals(1, todayCards.single().count)
        assertEquals(listOf("today"), todayCards.single().itemIds)

        val weekCards = GalleryCatalog.buildCards(
            listOf(weekStart, beforeWeek, today), loads, GalleryFilter.THIS_WEEK, "", now, "Unlinked", locale,
        )
        assertEquals(setOf("start", "today"), weekCards.single().itemIds.toSet())
        assertTrue(weekCards.single().itemIds.none { it == "before" })
    }

    @Test
    fun byLoadFiltersTripRouteAndFileName() {
        val now = cal(2026, Calendar.OCTOBER, 20, 15, 0)
        val loads = listOf(
            load("l1", "T-116KYL6KW", "Garner, NC", "Dallas, TX", "2026-10-20"),
            load("l2", "T-88ATLANTA", "Chicago, IL", "Atlanta, GA", "2026-10-18"),
        )
        val items = listOf(
            media("p1", "l1", now.timeInMillis, "bol.jpg"),
            media("p2", "l2", now.timeInMillis - DAY, "rate-confirmation.pdf"),
            media("loose", null, now.timeInMillis, "note.jpg"),
        )
        val all = GalleryCatalog.buildCards(items, loads, GalleryFilter.ALL, "Garner", now, "Unlinked", locale)
        assertEquals(3, all.size)

        val garner = GalleryCatalog.buildCards(items, loads, GalleryFilter.BY_LOAD, "  GARNER ", now, "Unlinked", locale)
        assertEquals(listOf("l1"), garner.map { it.loadKey })

        val dallas = GalleryCatalog.buildCards(items, loads, GalleryFilter.BY_LOAD, "dallas", now, "Unlinked", locale)
        assertEquals(listOf("T-116KYL6KW"), dallas.map { it.tripId })

        val file = GalleryCatalog.buildCards(items, loads, GalleryFilter.BY_LOAD, "rate-confirmation", now, "Unlinked", locale)
        assertEquals(listOf("l2"), file.map { it.loadKey })

        val unlinked = GalleryCatalog.buildCards(items, loads, GalleryFilter.BY_LOAD, "unlink", now, "Unlinked", locale)
        assertEquals(listOf(GalleryCatalog.UNLINKED), unlinked.map { it.loadKey })

        val blank = GalleryCatalog.buildCards(items, loads, GalleryFilter.BY_LOAD, "   ", now, "Unlinked", locale)
        assertEquals(
            GalleryCatalog.buildCards(items, loads, GalleryFilter.ALL, "", now, "Unlinked", locale).map { it.loadKey },
            blank.map { it.loadKey },
        )
        val none = GalleryCatalog.buildCards(items, loads, GalleryFilter.BY_LOAD, "zzzz", now, "Unlinked", locale)
        assertTrue(none.isEmpty())
    }

    @Test
    fun itemsForLoadUsesSameGroupingAsCards() {
        val loads = setOf("l1")
        val items = listOf(
            media("new", "l1", 30, "n.jpg"),
            media("old", "l1", 10, "o.jpg"),
            media("other", "l2", 20, "x.jpg"),
        )
        assertEquals(
            listOf("new", "old"),
            GalleryCatalog.itemsForLoad(items, "l1", loads).map { it.id },
        )
        assertEquals(
            listOf("other"),
            GalleryCatalog.itemsForLoad(items, GalleryCatalog.UNLINKED, loads).map { it.id },
        )
    }

    private fun load(id: String, trip: String, origin: String, destination: String, date: String) =
        GalleryLoadInfo(id, trip, origin, destination, "", date)

    private fun media(id: String, loadId: String?, timestamp: Long, fileName: String) =
        GalleryMediaRef(id, loadId, timestamp, "/files/$fileName", fileName)

    private fun cal(year: Int, month: Int, day: Int, hour: Int, minute: Int): Calendar =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1000L
    }
}
