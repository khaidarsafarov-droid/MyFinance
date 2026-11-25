package com.truckerload.data.backup

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.entities.MiscExpenseEntity
import com.truckerload.data.local.entities.PerDiemDayOverrideEntity
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.repository.LoadRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Misc expenses, per-diem overrides and photo/scan records (GPS, date, time) must survive a
 * backup round trip. A backup file that predates those fields must not wipe them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupExtrasRestoreTest {

    private lateinit var db: AppDatabase
    private lateinit var loadRepo: LoadRepository

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        loadRepo = LoadRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val expense = MiscExpenseEntity(
        id = 7,
        amount = 4_250,
        description = "Scale ticket",
        date = "2026-07-02",
        receiptPhotoPath = "/data/receipts/a.jpg",
        createdAt = 10L,
        updatedAt = 20L,
    )

    private val photo = PhotoEntity(
        id = "photo-gps",
        fileName = "garner.jpg",
        filePath = "/photos/garner.jpg",
        latitude = 35.711001,
        longitude = -78.614002,
        city = "Garner",
        state = "NC",
        zipCode = "27529",
        timestamp = 1_720_000_000_000L,
        loadId = "load-kept",
    )

    private val scan = ScanEntity(
        id = "scan-1",
        fileName = "bol.pdf",
        filePath = "/scans/bol.pdf",
        timestamp = 1_720_000_100_000L,
        fileSizeBytes = 2048,
        pageCount = 3,
        ocrText = "BOL 123",
        loadId = "load-gone",
        category = "BOL",
    )

    @Test
    fun extrasSurviveJsonRoundTripAndRestoreIntoEmptyDatabase() = runBlocking {
        val load = BackupTestFixtures.sampleLoad(id = "load-kept")
        val backup = BackupData(
            schemaVersion = BackupSchema.CURRENT,
            accountId = "user-abc",
            loads = listOf(load),
            miscExpenses = listOf(BackupMiscExpense.from(expense)),
            perDiemOverrides = listOf(BackupPerDiemOverride.from(PerDiemDayOverrideEntity("2026-07-04", false))),
            photos = listOf(BackupPhoto.from(photo)),
            scans = listOf(BackupScan.from(scan)),
        )

        val decoded = BackupDataCodec.decode(BackupDataCodec.toJson(backup))
        BackupRoomApplier.applyFullReplace(db, decoded)
        BackupRoomApplier.pruneOrphanMedia(db)

        assertEquals(expense, db.miscExpenseDao().getAll().single())
        assertEquals(PerDiemDayOverrideEntity("2026-07-04", false), db.perDiemDayOverrideDao().getAll().single())
        val restoredPhoto = db.photoDao().getAllPhotosOnce().single()
        assertEquals(photo, restoredPhoto)
        assertEquals(35.711001, restoredPhoto.latitude, 0.0)
        assertEquals(-78.614002, restoredPhoto.longitude, 0.0)
        assertEquals(1_720_000_000_000L, restoredPhoto.timestamp)
        // The scan pointed at a load the file does not restore: kept, unlinked.
        val restoredScan = db.scanDao().getAllScansOnce().single()
        assertNull(restoredScan.loadId)
        assertEquals("BOL 123", restoredScan.ocrText)
        assertEquals(3, restoredScan.pageCount)
    }

    @Test
    fun backupWithoutExtraFieldsKeepsLocalRows() = runBlocking {
        val load = BackupTestFixtures.sampleLoad(id = "load-kept")
        loadRepo.insertLoad(load, playFeedback = false)
        db.miscExpenseDao().upsert(expense)
        db.perDiemDayOverrideDao().upsert(PerDiemDayOverrideEntity("2026-07-04", true))
        db.photoDao().insert(photo)
        db.scanDao().insert(scan.copy(loadId = null))

        // A file written before these fields existed has no such keys at all.
        val legacyJson = """{"schemaVersion":2,"version":2,"accountId":"user-abc","loads":[],"paychecks":[],"diesel":[]}"""
        val legacy = BackupDataCodec.decode(legacyJson)
        assertNull(legacy.miscExpenses)
        assertNull(legacy.photos)

        BackupRoomApplier.applyFullReplace(db, legacy.copy(loads = listOf(load)))
        BackupRoomApplier.pruneOrphanMedia(db)

        assertEquals(1, db.miscExpenseDao().getAll().size)
        assertEquals(1, db.perDiemDayOverrideDao().getAll().size)
        assertNotNull(db.photoDao().getById("photo-gps"))
        assertNotNull(db.scanDao().getById("scan-1"))
    }

    @Test
    fun restoringOlderBackupUnlinksMediaOfNewerLoadsInsteadOfDeletingIt() = runBlocking {
        val kept = BackupTestFixtures.sampleLoad(id = "load-kept")
        val newer = BackupTestFixtures.sampleLoad(id = "load-newer")
        loadRepo.insertLoad(kept, playFeedback = false)
        loadRepo.insertLoad(newer, playFeedback = false)
        db.photoDao().insert(photo.copy(id = "photo-newer", loadId = newer.id))

        BackupRoomApplier.applyFullReplace(
            db,
            BackupData(schemaVersion = BackupSchema.CURRENT, accountId = "user-abc", loads = listOf(kept)),
        )
        val unlinked = BackupRoomApplier.pruneOrphanMedia(db)

        assertEquals(1, unlinked)
        val row = db.photoDao().getById("photo-newer")
        assertNotNull(row)
        assertNull(row!!.loadId)
        assertEquals(35.711001, row.latitude, 0.0)
        assertEquals(1_720_000_000_000L, row.timestamp)
        assertTrue(loadRepo.getAllLoadsOnce().none { it.id == newer.id })
    }
}
