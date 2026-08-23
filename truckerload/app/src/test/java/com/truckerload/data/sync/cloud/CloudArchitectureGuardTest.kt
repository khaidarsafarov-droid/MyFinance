package com.truckerload.data.sync.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CloudArchitectureGuardTest {
    @Test
    fun ktorPackage_exposesRequiredApis() {
        assertTrue(mainExists("com/truckerload/data/remote/ktor/KtorLoadApi.kt"))
        assertTrue(mainExists("com/truckerload/data/remote/ktor/KtorJournalApi.kt"))
        assertTrue(mainExists("com/truckerload/data/remote/ktor/KtorAuthInterceptor.kt"))
        assertTrue(mainExists("com/truckerload/data/remote/ktor/HttpClientProvider.kt"))
        assertTrue(mainExists("com/truckerload/data/remote/ktor/MediaPresignApi.kt"))
    }

    @Test
    fun cloudPackage_exposesOrchestration() {
        assertTrue(mainExists("com/truckerload/data/sync/cloud/CloudSyncEngine.kt"))
        assertTrue(mainExists("com/truckerload/data/sync/cloud/SyncConflictResolver.kt"))
        assertTrue(mainExists("com/truckerload/data/sync/cloud/SyncStatusTracker.kt"))
        assertTrue(mainExists("com/truckerload/data/sync/cloud/SyncMode.kt"))
        assertTrue(
            listOf(
                File("docs/CLOUD_ROLLBACK.md"),
                File("../docs/CLOUD_ROLLBACK.md"),
                File("../../docs/CLOUD_ROLLBACK.md"),
            ).any(File::isFile),
        )
    }

    @Test
    fun restoreAndHydration_clearLoadHistory() {
        val applier = readMain("com/truckerload/data/backup/BackupRoomApplier.kt")
        assertTrue(applier.contains("loadHistoryDao().deleteAll()"))
        val engine = readMain("com/truckerload/data/sync/CloudSyncEngine.kt")
        assertTrue(engine.contains("BackupRoomApplier.applyFullReplace"))
        val dao = readMain("com/truckerload/data/local/dao/LoadHistoryDao.kt")
        assertTrue(dao.contains("DELETE FROM load_history"))
    }

    @Test
    fun roomSchema_tracksCurrentVersion() {
        val db = readMain("com/truckerload/data/local/AppDatabase.kt")
        assertTrue(db.contains("version = 34"))
        assertFalse(db.contains("syncPending"))
        assertFalse(db.contains("migrateLegacyDatabaseIfNeeded"))
    }

    private fun mainExists(relative: String): Boolean =
        listOf(
            File("src/main/java/$relative"),
            File("app/src/main/java/$relative"),
            File("../app/src/main/java/$relative"),
        ).any(File::isFile)

    private fun readMain(relative: String): String {
        val file = listOf(
            File("src/main/java/$relative"),
            File("app/src/main/java/$relative"),
            File("../app/src/main/java/$relative"),
        ).firstOrNull(File::isFile) ?: error("missing $relative")
        return file.readText()
    }
}
