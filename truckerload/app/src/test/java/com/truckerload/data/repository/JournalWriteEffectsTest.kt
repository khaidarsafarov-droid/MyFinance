package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.domain.model.Load
import com.truckerload.domain.money.Money
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Repositories report writes through [JournalWriteEffects]; they do not reach app services. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class JournalWriteEffectsTest {

    private lateinit var db: AppDatabase
    private val effects = CountingEffects()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        AppDatabase.getInstance(context, "journal-write-effects-test")
        AppDatabase.closeCurrent()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        context.getSharedPreferences("truckerload_deleted_loads", 0).edit().clear().commit()
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.closeCurrent()
    }

    @Test
    fun insertLoad_reportsDataBackupAndMileage() = runBlocking {
        LoadRepository(db, effects).insertLoad(sample("T-FX-1"), playFeedback = false)
        assertEquals(1, effects.data)
        assertEquals(1, effects.backup)
        assertEquals(1, effects.mileage)
    }

    @Test
    fun batchWrite_reportsOnceAtTheEnd() = runBlocking {
        val repo = LoadRepository(db, effects)
        repo.runBatchWrite {
            repo.insertLoad(sample("T-FX-2"), playFeedback = false)
            repo.insertLoad(sample("T-FX-3"), playFeedback = false)
            assertEquals(0, effects.data)
        }
        assertEquals(1, effects.data)
        assertEquals(1, effects.backup)
        assertEquals(1, effects.mileage)
    }

    @Test
    fun deleteAll_refreshesWithoutBackup() = runBlocking {
        LoadRepository(db, effects).deleteAllLoads()
        assertEquals(1, effects.data)
        assertEquals(0, effects.backup)
    }

    @Test
    fun repositories_doNotImportAppServices() {
        listOf("LoadRepository", "MaintenanceRepository", "DieselRepository", "PaycheckRepository").forEach { name ->
            val src = readMain("data/repository/$name.kt")
            listOf("BackupService", "WidgetDataUpdater", "MaintenanceReminderCoordinator").forEach { service ->
                assertFalse("$name must not call $service", src.contains(service))
            }
        }
    }

    private class CountingEffects : JournalWriteEffects {
        var data = 0
        var backup = 0
        var mileage = 0
        override fun onDataChanged() { data++ }
        override fun onBackupWorthyWrite() { backup++ }
        override fun onMileageInputsChanged() { mileage++ }
    }

    private fun readMain(relativePath: String): String {
        val candidates = listOf(
            java.io.File("src/main/java/com/truckerload/$relativePath"),
            java.io.File("app/src/main/java/com/truckerload/$relativePath"),
        )
        return candidates.firstOrNull(java.io.File::isFile)?.readText()
            ?: error("Source not found: $relativePath")
    }

    private fun sample(tripId: String) = Load(
        id = tripId,
        tripId = tripId,
        date = "2026-09-01",
        totalRate = Money.fromDollars(2500.0),
        totalMiles = 850.0,
        pointA = "Garner, NC",
        pointB = "Dallas, TX",
        puCount = 1,
        delCount = 1,
        weekNumber = 36,
        year = 2026,
        rawMessage = "Trip ID: $tripId",
        parsedAt = 1L,
        updatedAt = 1L,
    )
}
