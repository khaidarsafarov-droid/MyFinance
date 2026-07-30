package com.truckerload.data.local

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase

private const val TAG = "MigrationHelpers"

/** True when [table] exists in the current SQLite schema. */
fun SupportSQLiteDatabase.hasTable(table: String): Boolean {
    query(
        "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
        arrayOf(table),
    ).use { cursor ->
        return cursor.moveToFirst()
    }
}

/** True when [column] exists on [table]. Missing table → false. */
fun SupportSQLiteDatabase.hasColumn(table: String, column: String): Boolean {
    if (!hasTable(table)) return false
    query("PRAGMA table_info(`$table`)").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        if (nameIndex < 0) return false
        while (cursor.moveToNext()) {
            if (cursor.getString(nameIndex) == column) return true
        }
    }
    return false
}

/**
 * Idempotent ADD COLUMN. [definitionSql] is the type + constraints fragment
 * (e.g. `TEXT NOT NULL DEFAULT ''` or `INTEGER`).
 */
fun SupportSQLiteDatabase.addColumnIfMissing(
    table: String,
    column: String,
    definitionSql: String,
) {
    if (!hasTable(table)) {
        Log.w(TAG, "addColumnIfMissing: table missing table=$table column=$column")
        return
    }
    if (hasColumn(table, column)) {
        Log.d(TAG, "skip ADD COLUMN $table.$column (already present)")
        return
    }
    val sql = "ALTER TABLE `$table` ADD COLUMN `$column` $definitionSql"
    Log.i(TAG, "DDL: $sql")
    execSQL(sql)
}

/**
 * Best-effort DROP COLUMN (SQLite 3.35+ / modern Android).
 * Returns true when the column was dropped; false when missing or unsupported.
 */
fun SupportSQLiteDatabase.dropColumnIfExists(table: String, column: String): Boolean {
    if (!hasTable(table) || !hasColumn(table, column)) return false
    return runCatching {
        val sql = "ALTER TABLE `$table` DROP COLUMN `$column`"
        Log.i(TAG, "DDL: $sql")
        execSQL(sql)
        true
    }.getOrElse { e ->
        Log.w(TAG, "DROP COLUMN unsupported or failed for $table.$column", e)
        false
    }
}

fun SupportSQLiteDatabase.execLogged(sql: String) {
    Log.i(TAG, "DDL: ${sql.lineSequence().first().trim()}…")
    execSQL(sql)
}
