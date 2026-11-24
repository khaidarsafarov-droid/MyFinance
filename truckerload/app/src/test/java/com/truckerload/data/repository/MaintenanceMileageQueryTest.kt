package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.domain.maintenance.MaintenanceMileageUseCase
import com.truckerload.domain.model.MaintenanceReminderType
import com.truckerload.domain.model.MaintenanceTask
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** The SQL ТО sum must give the same miles and load count as the in-memory rule. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MaintenanceMileageQueryTest {

    private lateinit var db: AppDatabase
    private val today = LocalDate.of(2026, 7, 28)
    private val zone = ZoneId.systemDefault()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        AppDatabase.getInstance(context, "maintenance-mileage-query-test")
        AppDatabase.closeCurrent()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.closeCurrent()
    }

    @Test
    fun sqlSum_matchesInMemoryRule_onEdgeCases() = runBlocking {
        val rows = listOf(
            row("T-OLD", 5_000.0, "2026-07-20", finish = "2026-07-22"),
            row("T-SAME-DAY", 1_778.0, "2026-07-24", finish = "2026-07-24"),
            row("T-B", 1_502.4, "2026-07-28", finish = "2026-07-28"),
            row("T-HALF", 0.5, "2026-07-25"),
            row("T-TINY", 0.4, "2026-07-25"),
            row("T-ZERO", 0.0, "2026-07-25"),
            row("T-NEG", -40.0, "2026-07-25"),
            row("T-HUGE", 99_999.0, "2026-07-25"),
            row("T-FUTURE", 900.0, "2027-07-25"),
            row("T-PADDED", 300.0, "2026-07-01", finish = "  2026-07-26  "),
            row("T-SHORT-FINISH", 200.0, "2026-07-26", finish = "2026-07"),
            row("T-DEL-IN", 250.0, "2026-07-10", lastDel = millis("2026-07-24", 0, 0)),
            row("T-DEL-BEFORE", 260.0, "2026-07-26", lastDel = millis("2026-07-23", 23, 59)),
            row("T-DEL-LAST", 270.0, "2026-07-01", lastDel = millis("2026-08-01", 23, 59)),
            row("T-DEL-AFTER", 280.0, "2026-07-26", lastDel = millis("2026-08-02", 0, 0)),
            row("T-BAD-DATE", 100.0, "2026-7-2"),
        )
        rows.forEach { db.loadDao().insert(it) }
        assertSame(rows, "2026-07-24")
        assertSame(rows, "2026-07-01")
        assertSame(rows, "2026-08-10")
    }

    @Test
    fun sqlSum_matchesInMemoryRule_onRandomJournal() = runBlocking {
        val random = Random(42)
        val rows = (0 until 400).map { i ->
            val day = LocalDate.of(2026, 1, 1).plusDays(random.nextLong(0, 300))
            val finish = when (random.nextInt(4)) {
                0 -> day.plusDays(random.nextLong(0, 5)).toString()
                1 -> ""
                else -> null
            }
            val lastDel = if (random.nextBoolean()) {
                day.plusDays(random.nextLong(0, 4)).atStartOfDay(zone).toInstant().toEpochMilli() +
                    random.nextLong(0, 86_400_000)
            } else {
                null
            }
            row("T-$i", random.nextDouble(-10.0, 20_000.0), day.toString(), finish, lastDel)
        }
        rows.forEach { db.loadDao().insert(it) }
        listOf("2026-01-01", "2026-03-15", "2026-06-30", "2026-07-26", "2026-09-01").forEach {
            assertSame(rows, it)
        }
    }

    @Test
    fun progress_dateTaskSkipsMiles() = runBlocking {
        db.loadDao().insert(row("T-1", 500.0, "2026-07-25"))
        val task = MaintenanceTask(
            title = "Inspection",
            reminderType = MaintenanceReminderType.DATE,
            startDate = "2026-07-01",
            dueDate = "2026-07-30",
        )
        val sql = MaintenanceMileageQuery.progress(db.loadDao(), task, today)
        val memory = MaintenanceMileageUseCase.progressForTask(task, emptyList(), today)
        assertEquals(memory, sql)
    }

    private suspend fun assertSame(rows: List<LoadEntity>, serviceDate: String) {
        val inputs = rows.map {
            MaintenanceMileageUseCase.LoadInput(
                tripId = it.tripId,
                id = it.id,
                miles = it.totalMiles,
                date = it.date,
                actualFinishDate = it.actualFinishDate,
                lastDelMillis = it.lastDelMillis,
            )
        }
        val expected = MaintenanceMileageUseCase.calculate(0L, 0L, serviceDate, inputs, today)
        val actual = MaintenanceMileageQuery.serviceMiles(db.loadDao(), serviceDate, today, zone)
        assertEquals("miles since $serviceDate", expected.milesSinceService, actual.miles)
        assertEquals("loads since $serviceDate", expected.loadCount, actual.loadCount)
    }

    private fun millis(date: String, hour: Int, minute: Int): Long =
        LocalDate.parse(date).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun row(
        tripId: String,
        miles: Double,
        date: String,
        finish: String? = null,
        lastDel: Long? = null,
    ) = LoadEntity(
        id = "id-$tripId",
        tripId = tripId,
        date = date,
        totalRate = 100_00L,
        totalMiles = miles,
        pointA = "A",
        pointB = "B",
        puCount = 1,
        delCount = 1,
        weekNumber = 1,
        year = 2026,
        rawMessage = "",
        parsedAt = 1L,
        updatedAt = 1L,
        lastDelMillis = lastDel,
        actualFinishDate = finish,
    )
}
