package com.truckerload.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.truckerload.data.local.dao.DieselDao
import com.truckerload.data.local.dao.LoadDao
import com.truckerload.data.local.dao.PenaltyDao
import com.truckerload.data.local.dao.PaycheckDao
import com.truckerload.data.local.dao.StopDao
import com.truckerload.data.local.dao.TelegramInboxDao
import com.truckerload.data.local.entities.DieselEntity
import com.truckerload.data.local.entities.LoadEntity
import com.truckerload.data.local.entities.PenaltyEntity
import com.truckerload.data.local.entities.PaycheckEntity
import com.truckerload.data.local.entities.StopEntity
import com.truckerload.data.local.entities.TelegramInboxEntity

@Database(
    entities = [
        LoadEntity::class, StopEntity::class, PenaltyEntity::class,
        PaycheckEntity::class, DieselEntity::class, TelegramInboxEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun loadDao(): LoadDao
    abstract fun stopDao(): StopDao
    abstract fun penaltyDao(): PenaltyDao
    abstract fun paycheckDao(): PaycheckDao
    abstract fun dieselDao(): DieselDao
    abstract fun telegramInboxDao(): TelegramInboxDao

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
                    "truckerload_db"
                )
                    .addMigrations(MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    .build().also { INSTANCE = it }
            }
        }

        fun applicationContext(): Context? = appContext
    }
}
