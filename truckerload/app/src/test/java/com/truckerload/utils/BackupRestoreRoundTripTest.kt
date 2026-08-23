package com.truckerload.utils

import androidx.room.Room
import com.truckerload.data.backup.BackupData
import com.truckerload.data.backup.BackupDataCodec
import com.truckerload.data.backup.BackupRestoreParser
import com.truckerload.data.backup.BackupSchema
import com.truckerload.data.backup.BackupTestFixtures
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toEntity
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupRestoreRoundTripTest {

    private lateinit var db: AppDatabase
    private lateinit var loadRepo: LoadRepository
    private lateinit var paycheckRepo: PaycheckRepository
    private lateinit var dieselRepo: DieselRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        loadRepo = LoadRepository(db)
        paycheckRepo = PaycheckRepository(db)
        dieselRepo = DieselRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportImport_restoresLoadsStopsPenaltiesPaycheckDiesel() = runBlocking {
        loadRepo.insertLoad(BackupTestFixtures.sampleLoad(), playFeedback = false)
        paycheckRepo.insertPaycheck(BackupTestFixtures.samplePaycheck())
        dieselRepo.insertDiesel(BackupTestFixtures.sampleDiesel())

        val originalLoads = loadRepo.getAllLoadsOnce()
        val originalPay = paycheckRepo.getAllPaychecksOnce()
        val originalDiesel = dieselRepo.getAllDieselOnce()
        assertEquals(1, originalLoads.size)
        assertEquals(2, originalLoads[0].stops.size)
        assertEquals(1, originalLoads[0].penalties.size)

        val exported = BackupData(
            exportedAt = BackupTestFixtures.EXPORTED_AT,
            accountId = "user-abc",
            loads = originalLoads,
            paychecks = originalPay,
            diesel = originalDiesel,
        )
        val fileBytes = BackupDataCodec.toUtf8Bytes(exported)
        assertEquals("application/json", BackupSchema.JSON_MIME)
        assertTrue(BackupSchema.jsonFileName(exported.exportedAt).endsWith(".json"))

        val parsedJson = BackupRestoreParser.parseToJson(fileBytes).getOrThrow()
        val decoded = BackupDataCodec.decode(parsedJson)

        assertEquals(originalLoads, decoded.loads)
        assertEquals(originalPay, decoded.paychecks)
        assertEquals(originalDiesel, decoded.diesel)
        assertEquals(BackupTestFixtures.PARSED_AT, decoded.loads[0].parsedAt)
        assertEquals(BackupTestFixtures.UPDATED_AT, decoded.loads[0].updatedAt)
        assertEquals("T-116KYL6KW", decoded.loads[0].tripId)
        assertEquals(originalLoads[0].id, decoded.loads[0].stops[0].loadId)
        assertEquals(originalLoads[0].id, decoded.loads[0].penalties[0].loadId)

        applyBackupLikeService(decoded)

        val restoredLoads = loadRepo.getAllLoadsOnce()
        val restoredPay = paycheckRepo.getAllPaychecksOnce()
        val restoredDiesel = dieselRepo.getAllDieselOnce()
        assertEquals(originalLoads, restoredLoads)
        assertEquals(originalPay, restoredPay)
        assertEquals(originalDiesel, restoredDiesel)
    }

    @Test
    fun legacyChartNote_doesNotRestore() {
        val note = BackupNoteFormatter.buildNote(BackupTestFixtures.sampleBackup()).visibleText
        val result = BackupRestoreParser.parseToJson(note.toByteArray())
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is com.truckerload.data.backup.BackupRestoreException.ChartNoteNotBackup)
    }

    private suspend fun applyBackupLikeService(backup: BackupData) {
        db.loadDao().deleteAll()
        db.paycheckDao().deleteAll()
        db.dieselDao().deleteAll()
        backup.loads.forEach { load ->
            db.loadDao().insert(load.toEntity())
            if (load.stops.isNotEmpty()) {
                db.stopDao().insertAll(load.stops.map { it.toEntity(load.id) })
            }
            if (load.penalties.isNotEmpty()) {
                db.penaltyDao().insertAll(load.penalties.map { it.toEntity(load.id) })
            }
        }
        if (backup.paychecks.isNotEmpty()) {
            db.paycheckDao().insertAll(backup.paychecks.map { it.toEntity() })
        }
        if (backup.diesel.isNotEmpty()) {
            db.dieselDao().insertAll(backup.diesel.map { it.toEntity() })
        }
    }
}
