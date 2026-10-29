package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Optional repeat interval for date-based maintenance reminders. */
val MIGRATION_43_44 = object : Migration(43, 44) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing(
            table = "maintenance_tasks",
            column = "repeatIntervalDays",
            definitionSql = "INTEGER",
            requireTable = true,
        )
    }
}
