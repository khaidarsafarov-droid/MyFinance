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
import com.truckerload.data.local.dao.PaycheckDao
import com.truckerload.data.local.dao.PenaltyDao
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
import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.data.local.entities.PenaltyEntity
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
    ],
    version = 35,
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
            if (existing != null && currentUserId == id) return existing
            synchronized(this) {
                if (INSTANCE != null && currentUserId == id) return INSTANCE as AppDatabase
                INSTANCE?.close()
                INSTANCE = null
                currentUserId = null
                LegacyDatabaseAbsorb.notePendingIfNeeded(context.applicationContext, id)
                val dbName = databaseNameFor(id)
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName,
                )
                    .addMigrations(*ALL_ROOM_MIGRATIONS)
                    // Pre-v6: blocked migrations throw UnsupportedDatabaseUpgradeException
                    // (no destructive wipe — restore from backup / reinstall).
                    .build()
                INSTANCE = db
                currentUserId = id
                return db
            }
        }

        /**
         * Convenience for background workers: open DB for the active AuthStore session,
         * or null when logged out.
         */
        fun getInstanceForActiveUser(context: Context): AppDatabase? {
            val userId = com.truckerload.data.preferences.AuthStore(context).currentUserIdOrNull()
                ?: return null
            return getInstance(context, userId)
        }

        /**
         * Opens the active user's DB, or [AccountIds.LOCAL_DEV] only in LOCAL_ONLY_MODE.
         * Prefer [getInstanceForActiveUser] in background jobs — returns null when logged out.
         */
        fun getInstance(context: Context): AppDatabase {
            val userId = com.truckerload.data.preferences.AuthStore(context).currentUserIdOrNull()
            if (userId != null) return getInstance(context, userId)
            if (com.truckerload.BuildConfig.LOCAL_ONLY_MODE) {
                return getInstance(context, com.truckerload.data.preferences.AccountIds.LOCAL_DEV)
            }
            error("No active user session — open AppDatabase with userId or wait until login")
        }

        fun closeCurrent() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                currentUserId = null
            }
        }

        fun currentUserIdOrNull(): String? = currentUserId

        fun databaseNameFor(userId: String): String =
            "truckerload_${com.truckerload.data.preferences.AccountIds.sanitizeFilePart(userId)}"

        fun applicationContext(): Context? = appContext
    }
}
