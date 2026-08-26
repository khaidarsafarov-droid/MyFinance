package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Thrown when opening a pre-v6 Room file. Destructive wipe was removed —
 * user must restore from backup / reinstall rather than lose data silently.
 */
class UnsupportedDatabaseUpgradeException(
    val fromVersion: Int,
) : IllegalStateException(
    "Database version $fromVersion is no longer supported. " +
        "Please reinstall the app and restore from backup. " +
        "Требуется переустановка. Сделайте бэкап.",
)

private fun blockedLegacyMigration(fromVersion: Int): Migration =
    object : Migration(fromVersion, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            throw UnsupportedDatabaseUpgradeException(fromVersion)
        }
    }

/** Pre-v6 installs: fail closed (no destructive wipe). */
val MIGRATION_1_6_BLOCKED = blockedLegacyMigration(1)
val MIGRATION_2_6_BLOCKED = blockedLegacyMigration(2)
val MIGRATION_3_6_BLOCKED = blockedLegacyMigration(3)
val MIGRATION_4_6_BLOCKED = blockedLegacyMigration(4)
val MIGRATION_5_6_BLOCKED = blockedLegacyMigration(5)

/** All Room migrations registered on [AppDatabase] (including blocked pre-v6). */
val ALL_ROOM_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_1_6_BLOCKED,
    MIGRATION_2_6_BLOCKED,
    MIGRATION_3_6_BLOCKED,
    MIGRATION_4_6_BLOCKED,
    MIGRATION_5_6_BLOCKED,
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
    MIGRATION_23_24,
    MIGRATION_24_25,
    MIGRATION_25_26,
    MIGRATION_26_27,
    MIGRATION_27_28,
    MIGRATION_28_29,
    MIGRATION_29_30,
    MIGRATION_30_31,
    MIGRATION_31_32,
    MIGRATION_32_33,
    MIGRATION_33_34,
    MIGRATION_34_35,
    MIGRATION_35_36,
    MIGRATION_36_37,
    MIGRATION_37_38,
    MIGRATION_38_39,
    MIGRATION_39_40,
    MIGRATION_40_41,
)

/** Forward path from the first supported schema (v6) to current. */
val ALL_MIGRATIONS_FROM_V6: Array<Migration> = ALL_ROOM_MIGRATIONS
    .filter { it.startVersion >= 6 }
    .toTypedArray()
