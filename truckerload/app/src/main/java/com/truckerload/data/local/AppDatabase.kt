package com.truckerload.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.truckerload.data.local.AppDatabase.Companion.getInstanceForActiveUser
import com.truckerload.data.local.dao.DieselDao
import com.truckerload.data.local.dao.DriverProfessionalDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.LoadDao
import com.truckerload.data.local.dao.LoadHistoryDao
import com.truckerload.data.local.dao.MaintenanceDao
import com.truckerload.data.local.dao.MediaSyncQueueDao
import com.truckerload.data.local.dao.MiscExpenseDao
import com.truckerload.data.local.dao.PaycheckDao
import com.truckerload.data.local.dao.PenaltyDao
import com.truckerload.data.local.dao.PerDiemDayOverrideDao
import com.truckerload.data.local.dao.PhotoDao
import com.truckerload.data.local.dao.ScanDao
import com.truckerload.data.local.dao.StopDao
import com.truckerload.data.local.dao.SyncOutboxDao
import com.truckerload.data.local.dao.TelegramInboxDao
import com.truckerload.data.local.dao.UserAccountDao
import com.truckerload.data.local.entities.CrowdRateEntity
import com.truckerload.data.local.entities.DieselEntity
import com.truckerload.data.local.entities.DriverProfessionalEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.data.local.entities.MaintenanceArchiveEntity
import com.truckerload.data.local.entities.MaintenanceTaskEntity
import com.truckerload.data.local.entities.MediaSyncQueueEntity
import com.truckerload.data.local.entities.MiscExpenseEntity
import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.PerDiemDayOverrideEntity
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.data.local.entities.SyncOutboxEntity
import com.truckerload.data.local.entities.TelegramInboxEntity
import com.truckerload.data.local.entities.UserAccountEntity

/**
 * Main Room database for account-scoped local app data and Telegram sync state.
 */
@Database(
    entities = [
        LoadEntity::class,
        StopEntity::class,
        PenaltyEntity::class,
        PaycheckEntity::class,
        DieselEntity::class,
        TelegramInboxEntity::class,
        PhotoEntity::class,
        ScanEntity::class,
        LoadHistory::class,
        DriverProfileEntity::class,
        SyncOutboxEntity::class,
        MediaSyncQueueEntity::class,
        MaintenanceTaskEntity::class,
        MaintenanceArchiveEntity::class,
        CrowdRateEntity::class,
        UserAccountEntity::class,
        DriverProfessionalEntity::class,
        PerDiemDayOverrideEntity::class,
        MiscExpenseEntity::class,
    ],
    version = 43,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loadDao(): LoadDao
    abstract fun loadHistoryDao(): LoadHistoryDao
    abstract fun stopDao(): StopDao
    abstract fun penaltyDao(): PenaltyDao
    abstract fun paycheckDao(): PaycheckDao
    abstract fun dieselDao(): DieselDao
    abstract fun telegramInboxDao(): TelegramInboxDao
    abstract fun photoDao(): PhotoDao
    abstract fun scanDao(): ScanDao
    abstract fun driverProfileDao(): DriverProfileDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun mediaSyncQueueDao(): MediaSyncQueueDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun crowdRateDao(): com.truckerload.data.local.dao.CrowdRateDao
    abstract fun userAccountDao(): UserAccountDao
    abstract fun driverProfessionalDao(): DriverProfessionalDao
    abstract fun perDiemDayOverrideDao(): PerDiemDayOverrideDao
    abstract fun miscExpenseDao(): MiscExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var currentUserId: String? = null

        @Volatile
        private var appContext: Context? = null

        /**
         * Opens the Room database for [userId]. Each account has its own file
         * (`truckerload_<userId>`), so loads / Telegram inbox never leak across users.
         *
         * Legacy `truckerload_db` is never auto-copied; [LegacyDatabaseAbsorb] asks first.
         */
        fun getInstance(context: Context, userId: String): AppDatabase {
            val id = userId.trim()
            require(id.isNotBlank()) { "userId required for database" }
            appContext = context.applicationContext
            val existing = INSTANCE
            if (existing != null && reusable(existing, id)) return existing
            synchronized(this) {
                val current = INSTANCE
                if (current != null && reusable(current, id)) return current
                INSTANCE?.close()
                INSTANCE = null
                currentUserId = null
                LegacyDatabaseAbsorb.notePendingIfNeeded(context.applicationContext, id)
                val dbName = databaseNameFor(id)
                // Builder stays lazy. SQLCipher opens inside [RecoveryGatedOpenHelper]
                // on Room's connection pool, after journal recovery releases the gate.
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName,
                )
                    .addMigrations(*ALL_ROOM_MIGRATIONS)
                    .openHelperFactory(
                        RecoveryGatedOpenHelperFactory(
                            appContext = context.applicationContext,
                            dbName = dbName,
                            useSqlCipher = !DatabasePassphraseStore.isRobolectric(),
                        ),
                    )
                    .build()
                INSTANCE = db
                currentUserId = id
                return db
            }
        }

        /**
         * Convenience for background workers: open DB for the active AuthStore session,
         * or null when logged out. Unknown (prefs still opening) is not logged out.
         */
        fun getInstanceForActiveUser(context: Context): AppDatabase? {
            val userId = resolveActiveUserId(context) ?: return null
            return getInstance(context, userId)
        }

        /**
         * Opens the active user's DB, or [AccountIds.LOCAL_DEV] only in LOCAL_ONLY_MODE.
         * Prefer [getInstanceForActiveUser] in background jobs — returns null when logged out.
         */
        fun getInstance(context: Context): AppDatabase {
            val userId = resolveActiveUserId(context)
            if (userId != null) return getInstance(context, userId)
            if (com.truckerload.BuildConfig.LOCAL_ONLY_MODE) {
                return getInstance(context, com.truckerload.data.preferences.AccountIds.LOCAL_DEV)
            }
            error("No active user session — open AppDatabase with userId or wait until login")
        }

        /**
         * Off the main thread this waits until cold start publishes, same as before.
         * On the main thread a short wait is followed by the rest of the wait on IO,
         * so a still-restoring session is not treated as signed out and does not throw.
         */
        private fun resolveActiveUserId(context: Context): String? {
            val auth = com.truckerload.data.preferences.AuthStore(context)
            if (!JournalRecoveryGate.callerIsMainThread() || auth.sessionKnown.value) {
                return auth.currentUserIdOrNull()
            }
            if (!auth.awaitReadyBlocking(JournalRecoveryGate.mainOpenWaitMillis)) {
                android.util.Log.w(
                    "AppDatabase",
                    "Main-thread database open is waiting for session restore on IO",
                )
                return RecoveryOpenThreads.submit { auth.currentUserIdOrNull() }.get()
            }
            return auth.currentUserIdOrNull()
        }

        private fun reusable(existing: AppDatabase, userId: String): Boolean {
            if (currentUserId != userId) return false
            val helper = runCatching { existing.openHelper }.getOrNull() ?: return false
            return helper !is RecoveryGatedOpenHelper || !helper.isClosed
        }

        fun closeCurrent() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                currentUserId = null
            }
        }

        /** Closes the singleton only when it still belongs to [userId] (safe during account switch). */
        fun closeIfCurrentUser(userId: String) {
            val id = userId.trim()
            if (id.isBlank()) return
            synchronized(this) {
                if (currentUserId == id) {
                    INSTANCE?.close()
                    INSTANCE = null
                    currentUserId = null
                }
            }
        }

        fun currentUserIdOrNull(): String? = currentUserId

        fun databaseNameFor(userId: String): String =
            "truckerload_${com.truckerload.data.preferences.AccountIds.sanitizeFilePart(userId)}"

        fun applicationContext(): Context? = appContext
    }
}
