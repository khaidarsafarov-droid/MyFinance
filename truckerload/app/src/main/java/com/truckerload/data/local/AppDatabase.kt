package com.truckerload.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.truckerload.utils.CrashReporting
import com.truckerload.data.local.dao.BlockedUserDao
import com.truckerload.data.local.dao.CallSessionDao
import com.truckerload.data.local.dao.ChallengeParticipationDao
import com.truckerload.data.local.dao.ChatMemberDao
import com.truckerload.data.local.dao.DieselDao
import com.truckerload.data.local.dao.DriverFollowDao
import com.truckerload.data.local.dao.DriverProfileDao
import com.truckerload.data.local.dao.DriverStatusDao
import com.truckerload.data.local.dao.LoadDao
import com.truckerload.data.local.dao.LoadHistoryDao
import com.truckerload.data.local.dao.MaintenanceDao
import com.truckerload.data.local.dao.MessageReactionDao
import com.truckerload.data.local.dao.MediaSyncQueueDao
import com.truckerload.data.local.dao.PaycheckDao
import com.truckerload.data.local.dao.PenaltyDao
import com.truckerload.data.local.dao.PhotoDao
import com.truckerload.data.local.dao.ScanDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.dao.StopDao
import com.truckerload.data.local.dao.SyncOutboxDao
import com.truckerload.data.local.dao.TelegramInboxDao
import com.truckerload.data.local.dao.VoiceRoomDao
import com.truckerload.data.local.dao.VoiceRoomParticipantDao
import com.truckerload.data.local.dao.VoiceSignalDao
import com.truckerload.data.local.entities.CrowdRateEntity
import com.truckerload.data.local.entities.BlockedUserEntity
import com.truckerload.data.local.entities.CallSessionEntity
import com.truckerload.data.local.entities.ChallengeParticipationEntity
import com.truckerload.data.local.entities.ChatMemberEntity
import com.truckerload.data.local.entities.DieselEntity
import com.truckerload.data.local.entities.DriverFollowEntity
import com.truckerload.data.local.entities.DriverProfileEntity
import com.truckerload.data.local.entities.DriverStatusEntity
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.LoadHistory
import com.truckerload.data.local.entities.MaintenanceArchiveEntity
import com.truckerload.data.local.entities.MaintenanceTaskEntity
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.MediaSyncQueueEntity
import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.data.local.entities.SyncOutboxEntity
import com.truckerload.data.local.entities.TelegramInboxEntity
import com.truckerload.data.local.entities.VoiceRoomEntity
import com.truckerload.data.local.entities.VoiceRoomParticipantEntity
import com.truckerload.data.local.entities.VoiceSignalEntity

/**
 * Main Room database for account-scoped local app data and social/Telegram sync state.
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
        SocialChatEntity::class,
        SocialMessageEntity::class,
        BlockedUserEntity::class,
        DriverStatusEntity::class,
        ChallengeParticipationEntity::class,
        MessageReactionEntity::class,
        VoiceRoomEntity::class,
        VoiceRoomParticipantEntity::class,
        CallSessionEntity::class,
        VoiceSignalEntity::class,
        DriverFollowEntity::class,
        ChatMemberEntity::class,
        SocialPeerEntity::class,
        SyncOutboxEntity::class,
        MediaSyncQueueEntity::class,
        MaintenanceTaskEntity::class,
        MaintenanceArchiveEntity::class,
        CrowdRateEntity::class,
    ],
    version = 29,
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
    abstract fun socialChatDao(): SocialChatDao
    abstract fun socialMessageDao(): SocialMessageDao
    abstract fun blockedUserDao(): BlockedUserDao
    abstract fun driverStatusDao(): DriverStatusDao
    abstract fun challengeParticipationDao(): ChallengeParticipationDao
    abstract fun messageReactionDao(): MessageReactionDao
    abstract fun voiceRoomDao(): VoiceRoomDao
    abstract fun voiceRoomParticipantDao(): VoiceRoomParticipantDao
    abstract fun callSessionDao(): CallSessionDao
    abstract fun voiceSignalDao(): VoiceSignalDao
    abstract fun driverFollowDao(): DriverFollowDao
    abstract fun chatMemberDao(): ChatMemberDao
    abstract fun socialPeerDao(): SocialPeerDao
    abstract fun syncOutboxDao(): SyncOutboxDao
    abstract fun mediaSyncQueueDao(): MediaSyncQueueDao
    abstract fun maintenanceDao(): MaintenanceDao
    abstract fun crowdRateDao(): com.truckerload.data.local.dao.CrowdRateDao

    companion object {
        private const val LEGACY_DB_NAME = "truckerload_db"
        private const val META_PREFS = LegacyDatabaseAbsorb.META_PREFS
        private const val KEY_LEGACY_DB_MIGRATED = "legacy_db_migrated"
        private const val KEY_LEGACY_DB_OWNER = LegacyDatabaseAbsorb.KEY_LEGACY_DB_OWNER
        private const val KEY_LEGACY_DB_CLAIMED = LegacyDatabaseAbsorb.KEY_LEGACY_DB_CLAIMED

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
         * Legacy single-file `truckerload_db` is copied once to the first logged-in account.
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
                migrateLegacyDatabaseIfNeeded(context.applicationContext, id)
                // Stage3: never auto-copy another account's DB — prompt via LegacyAbsorbDialog
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

        private fun migrateLegacyDatabaseIfNeeded(context: Context, userId: String) {
            val meta = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            if (meta.getBoolean(KEY_LEGACY_DB_MIGRATED, false)) return
            val legacy = context.getDatabasePath(LEGACY_DB_NAME)
            if (!legacy.exists()) {
                meta.edit().putBoolean(KEY_LEGACY_DB_MIGRATED, true).apply()
                return
            }
            val target = context.getDatabasePath(databaseNameFor(userId))
            if (target.exists() && DatabaseFileCopy.isHealthyDatabase(target)) {
                meta.edit()
                    .putBoolean(KEY_LEGACY_DB_MIGRATED, true)
                    .putString(KEY_LEGACY_DB_OWNER, userId)
                    .apply()
                return
            }
            // Broken leftover from a previous failed copy — remove and retry.
            if (target.exists()) {
                DatabaseFileCopy.deleteDbTree(target)
            }
            val copied = copyDatabaseOrReport(
                source = legacy,
                target = target,
                userId = userId,
                op = "legacy_copy",
            )
            if (copied) {
                meta.edit()
                    .putBoolean(KEY_LEGACY_DB_MIGRATED, true)
                    .putString(KEY_LEGACY_DB_OWNER, userId)
                    .apply()
            }
            // On failure: do NOT set the migrated flag so the next open retries.
        }

        private fun copyDatabaseOrReport(
            source: java.io.File,
            target: java.io.File,
            userId: String,
            op: String,
        ): Boolean {
            val result = DatabaseFileCopy.copyWithSidecars(source, target)
            if (result.isSuccess) return true
            val error = result.exceptionOrNull()
                ?: IllegalStateException("$op failed without exception")
            Log.e(TAG, "$op failed for user=$userId", error)
            CrashReporting.setCustomKey("db_copy_op", op)
            CrashReporting.setCustomKey("legacy_copy_user", userId)
            CrashReporting.setCustomKey("legacy_db_size", source.length())
            CrashReporting.recordException(error)
            return false
        }

        private const val TAG = "AppDatabase"
    }
}
