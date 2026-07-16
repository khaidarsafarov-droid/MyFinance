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
    version = 19,
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
        @Volatile
        private var INSTANCE: AppDatabase? = null

        @Volatile
        private var appContext: Context? = null

        fun getInstance(context: Context): AppDatabase {
            appContext = context.applicationContext
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "truckerload_db",
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
                    )
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5)
                    .build().also { INSTANCE = it }
            }
        }

        fun applicationContext(): Context? = appContext
    }
}
