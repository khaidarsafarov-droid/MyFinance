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
        assertTrue(db.contains("version = 40"))
        assertFalse(db.contains("syncPending"))
        assertFalse(db.contains("migrateLegacyDatabaseIfNeeded"))
    }

    @Test
    fun callers_routeThroughInjectableCloudSyncEngine() {
        val worker = readMain("com/truckerload/sync/CloudSyncWorker.kt")
        assertTrue(worker.contains("com.truckerload.data.sync.cloud.CloudSyncEngine"))
        assertTrue(worker.contains("cloudSyncEngine.onSessionReady()"))
        assertFalse(worker.contains("LegacyCloudSyncEngine.onSessionReady(applicationContext)"))

        val outbound = readMain("com/truckerload/sync/OutboundSyncWorker.kt")
        assertTrue(outbound.contains("cloudSyncEngine.pushLocalSnapshot()"))

        val activity = readMain("com/truckerload/presentation/MainActivity.kt")
        assertTrue(activity.contains("cloudSyncEngine.onSessionReady()"))
        assertTrue(activity.contains("SessionTeardown.signOut"))
        assertTrue(worker.contains("SessionTeardown.signOut"))
        assertTrue(mainExists("com/truckerload/presentation/components/SyncStatusBanner.kt"))
    }

    @Test
    fun stage2_syncFixes_areWired() {
        val engine = readMain("com/truckerload/data/sync/CloudSyncEngine.kt")
        assertTrue(engine.contains("lastReadUsedStaleMirror"))
        assertTrue(engine.contains("if (pulled || pushed)"))
        val paycheck = readMain("com/truckerload/data/repository/PaycheckRepository.kt")
        assertTrue(paycheck.contains("JournalSyncClock.bump"))
        val diesel = readMain("com/truckerload/data/repository/DieselRepository.kt")
        assertTrue(diesel.contains("JournalSyncClock.bump"))
        val cdc = readMain("com/truckerload/data/repository/LoadRepositorySync.kt")
        assertTrue(cdc.indexOf("db.withTransaction") < cdc.indexOf("getExistingTripIds(tripIds)"))
        val telegramWorker = readMain("com/truckerload/sync/TelegramSyncWorker.kt")
        assertFalse(telegramWorker.contains("TelegramPollCoordinator.withPollLock"))
        val driveWorker = readMain("com/truckerload/data/backup/DriveSyncWorker.kt")
        assertTrue(driveWorker.contains("if (ok) Result.success() else Result.retry()"))
        val app = readMain("com/truckerload/TruckerLoadApp.kt")
        assertTrue(app.contains("allowsCloudCalls()"))
        val hybrid = readMain("com/truckerload/data/sync/AccountCloudBackend.kt")
        assertTrue(hybrid.contains("lastReadUsedStaleMirror = true"))
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
