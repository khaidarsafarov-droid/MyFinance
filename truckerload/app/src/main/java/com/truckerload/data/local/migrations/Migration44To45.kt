package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Cancelled loads (TONU). Existing loads stay as they are: not cancelled,
 * no stored original rate or miles.
 */
val MIGRATION_44_45 = object : Migration(44, 45) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.addColumnIfMissing(
            table = "loads",
            column = "isCancelled",
            definitionSql = "INTEGER NOT NULL DEFAULT 0",
            requireTable = true,
        )
        db.addColumnIfMissing(table = "loads", column = "cancelledAt", definitionSql = "INTEGER", requireTable = true)
        db.addColumnIfMissing(table = "loads", column = "originalRate", definitionSql = "INTEGER", requireTable = true)
        db.addColumnIfMissing(table = "loads", column = "originalMiles", definitionSql = "REAL", requireTable = true)
    }
}
