package com.truckerload.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
import com.truckerload.data.local.dao.MessageReactionDao
import com.truckerload.data.local.dao.PaycheckDao
import com.truckerload.data.local.dao.PenaltyDao
import com.truckerload.data.local.dao.PhotoDao
import com.truckerload.data.local.dao.ScanDao
import com.truckerload.data.local.dao.SocialChatDao
import com.truckerload.data.local.dao.SocialMessageDao
import com.truckerload.data.local.dao.SocialPeerDao
import com.truckerload.data.local.dao.StopDao
import com.truckerload.data.local.dao.TelegramInboxDao
import com.truckerload.data.local.dao.VoiceRoomDao
import com.truckerload.data.local.dao.VoiceRoomParticipantDao
import com.truckerload.data.local.dao.VoiceSignalDao
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
import com.truckerload.data.local.entities.MessageReactionEntity
import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.data.local.entities.ScanEntity
import com.truckerload.data.local.entities.SocialChatEntity
import com.truckerload.data.local.entities.SocialMessageEntity
import com.truckerload.data.local.entities.SocialPeerEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.data.local.entities.TelegramInboxEntity
import com.truckerload.data.local.entities.VoiceRoomEntity
import com.truckerload.data.local.entities.VoiceRoomParticipantEntity
import com.truckerload.data.local.entities.VoiceSignalEntity

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
    ],
    version = 23,
    // exportSchema=false: Room will not write schema JSON under schemas/. Migrations still
    // run from code, but CI cannot diff exported schemas — enable exportSchema=true +
    // schemas/ in VCS before shipping destructive migration changes.
    exportSchema = false,
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

    companion object {
        private const val LEGACY_DB_NAME = "truckerload_db"
        private const val META_PREFS = "truckerload_account_meta"
        private const val KEY_LEGACY_DB_MIGRATED = "legacy_db_migrated"
        private const val KEY_LEGACY_DB_OWNER = "legacy_db_owner"
        private const val KEY_LEGACY_DB_CLAIMED = "legacy_db_claimed_by_account"

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
                absorbPreviousLocalDatabaseIfNeeded(context.applicationContext, id)
                val dbName = databaseNameFor(id)
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName,
                )
                    .addMigrations(
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19,
                        MIGRATION_19_20,
                        MIGRATION_20_21,
                        MIGRATION_21_22,
                        MIGRATION_22_23,
                    )
                    // RISK: versions 1–5 have no migrations — opening an ancient DB wipes all tables.
                    // Ship with exportSchema=true + documented upgrade path before removing this.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5)
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
            if (target.exists()) {
                meta.edit()
                    .putBoolean(KEY_LEGACY_DB_MIGRATED, true)
                    .putString(KEY_LEGACY_DB_OWNER, userId)
                    .apply()
                return
            }
            runCatching {
                legacy.copyTo(target, overwrite = false)
                copySidecar(legacy, target, "-wal")
                copySidecar(legacy, target, "-shm")
                copySidecar(legacy, target, "-journal")
            }
            meta.edit()
                .putBoolean(KEY_LEGACY_DB_MIGRATED, true)
                .putString(KEY_LEGACY_DB_OWNER, userId)
                .apply()
        }

        private fun copySidecar(legacy: java.io.File, target: java.io.File, suffix: String) {
            val src = java.io.File(legacy.path + suffix)
            if (!src.exists()) return
            src.copyTo(java.io.File(target.path + suffix), overwrite = false)
        }

        /**
         * One-time: if this account has no DB yet but a previous single-user DB exists
         * under [AccountIds.LOCAL_DEV] or email-hash id, copy it so the first cloud login
         * keeps existing loads/Telegram history.
         */
        private fun absorbPreviousLocalDatabaseIfNeeded(context: Context, userId: String) {
            if (userId == com.truckerload.data.preferences.AccountIds.LOCAL_DEV) return
            val meta = context.getSharedPreferences(META_PREFS, Context.MODE_PRIVATE)
            if (meta.getBoolean(KEY_LEGACY_DB_CLAIMED, false)) return
            val target = context.getDatabasePath(databaseNameFor(userId))
            if (target.exists()) {
                meta.edit().putBoolean(KEY_LEGACY_DB_CLAIMED, true).apply()
                return
            }
            val email = com.truckerload.data.preferences.AuthStore(context).email.value
            val candidates = buildList {
                add(com.truckerload.data.preferences.AccountIds.LOCAL_DEV)
                if (email.isNotBlank()) {
                    add(com.truckerload.data.preferences.AccountIds.fromEmail(email))
                }
            }
            val sourceId = candidates.firstOrNull { candidate ->
                candidate != userId && context.getDatabasePath(databaseNameFor(candidate)).exists()
            } ?: return
            val source = context.getDatabasePath(databaseNameFor(sourceId))
            runCatching {
                source.copyTo(target, overwrite = false)
                copySidecar(source, target, "-wal")
                copySidecar(source, target, "-shm")
                copySidecar(source, target, "-journal")
            }
            meta.edit()
                .putBoolean(KEY_LEGACY_DB_CLAIMED, true)
                .putString(KEY_LEGACY_DB_OWNER, userId)
                .apply()
        }
    }
}
