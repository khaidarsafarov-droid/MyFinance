package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Добавляет PU/DEL millis без удаления существующих грузов. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE loads ADD COLUMN firstPuMillis INTEGER")
        db.execSQL("ALTER TABLE loads ADD COLUMN lastDelMillis INTEGER")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS telegram_inbox (
                updateId INTEGER NOT NULL PRIMARY KEY,
                chatId TEXT NOT NULL,
                text TEXT NOT NULL,
                messageDateSeconds INTEGER,
                receivedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_telegram_inbox_chatId_messageDateSeconds ON telegram_inbox (chatId, messageDateSeconds)")
    }
}

/** Route metrics: duration, pace, stop count for weekly yield. */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE loads ADD COLUMN route TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE loads ADD COLUMN firstPuCityState TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE loads ADD COLUMN lastDelCityState TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE loads ADD COLUMN durationDays REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE loads ADD COLUMN pace REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE loads ADD COLUMN stopCount INTEGER NOT NULL DEFAULT 0")

        db.execSQL(
            """
            UPDATE loads SET
                route = pointA || ' → ' || pointB,
                firstPuCityState = pointA,
                lastDelCityState = pointB
            WHERE pointA != '' AND pointB != ''
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE loads SET durationDays = MAX(1.0,
                CAST((lastDelMillis - firstPuMillis + 86399999) / 86400000 AS REAL))
            WHERE firstPuMillis IS NOT NULL
              AND lastDelMillis IS NOT NULL
              AND lastDelMillis > firstPuMillis
              AND durationDays = 0
            """.trimIndent()
        )
        db.execSQL(
            """
            UPDATE loads SET pace = totalRate / durationDays
            WHERE durationDays > 0 AND pace = 0
            """.trimIndent()
        )
    }
}
