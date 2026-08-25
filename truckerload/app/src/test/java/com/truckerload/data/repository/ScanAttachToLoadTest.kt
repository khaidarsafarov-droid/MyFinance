package com.truckerload.data.repository

import androidx.room.Room
import com.truckerload.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/**
 * QUALITY_100 #37 — scan saved with loadId appears under that load.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScanAttachToLoadTest {

    private lateinit var db: AppDatabase
    private lateinit var scanRepo: ScanRepository
    private lateinit var scansDir: File

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scanRepo = ScanRepository(db)
        scansDir = File(context.cacheDir, "scan_attach_test").apply {
            deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        db.close()
        scansDir.deleteRecursively()
    }

    @Test
    fun saveScan_withLoadId_appearsInWatchByLoadId() = runBlocking {
        val pdf = File(scansDir, "attach.pdf").apply { writeText("%PDF-1") }
        val saved = scanRepo.saveScan(
            fileName = pdf.name,
            filePath = pdf.absolutePath,
            timestamp = 99L,
            fileSizeBytes = pdf.length(),
            pageCount = 1,
            ocrText = "BOL",
            loadId = "load-attach-1",
        )

        val byLoad = scanRepo.watchScansByLoadId("load-attach-1").first()
        assertEquals(1, byLoad.size)
        assertEquals(saved.id, byLoad[0].id)
        assertEquals("load-attach-1", byLoad[0].loadId)
        assertEquals("LOAD", byLoad[0].category)
    }
}
