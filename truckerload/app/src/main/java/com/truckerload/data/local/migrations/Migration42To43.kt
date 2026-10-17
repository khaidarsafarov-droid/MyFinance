package com.truckerload.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * REAL dollar amounts become INTEGER cents. Diesel price per gallon becomes
 * thousandths of a dollar. An empty pre-v42 stub is replaced with the current
 * shape. A stub that already has rows keeps them: the missing marker column is
 * added instead of dropping the table.
 */
val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {

        rebuildMoneyTable(
            db,
            table = "loads",
            requiredColumn = "deadheadMiles",
            missingColumnSql = "REAL NOT NULL DEFAULT 0.0",
            createNewSql = "CREATE TABLE IF NOT EXISTS `loads_new` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, `date` TEXT NOT NULL, `totalRate` INTEGER NOT NULL, `totalMiles` REAL NOT NULL, `pointA` TEXT NOT NULL, `pointB` TEXT NOT NULL, `puCount` INTEGER NOT NULL, `delCount` INTEGER NOT NULL, `weekNumber` INTEGER NOT NULL, `year` INTEGER NOT NULL, `rawMessage` TEXT NOT NULL, `parsedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `firstPuMillis` INTEGER, `lastDelMillis` INTEGER, `route` TEXT NOT NULL, `firstPuCityState` TEXT NOT NULL, `lastDelCityState` TEXT NOT NULL, `durationDays` REAL NOT NULL, `pace` INTEGER NOT NULL, `stopCount` INTEGER NOT NULL, `isDispute` INTEGER NOT NULL, `disputeResponseDate` TEXT, `disputeCompleted` INTEGER NOT NULL, `disputeAmount` INTEGER, `disputeApplyToLoad` INTEGER NOT NULL, `disputeAmountApplied` INTEGER NOT NULL, `actualFinishDate` TEXT, `equipmentType` TEXT, `deadheadMiles` REAL NOT NULL, PRIMARY KEY(`id`))",
            insertSql = "INSERT INTO `loads_new` (`id`, `tripId`, `date`, `totalRate`, `totalMiles`, `pointA`, `pointB`, `puCount`, `delCount`, `weekNumber`, `year`, `rawMessage`, `parsedAt`, `updatedAt`, `firstPuMillis`, `lastDelMillis`, `route`, `firstPuCityState`, `lastDelCityState`, `durationDays`, `pace`, `stopCount`, `isDispute`, `disputeResponseDate`, `disputeCompleted`, `disputeAmount`, `disputeApplyToLoad`, `disputeAmountApplied`, `actualFinishDate`, `equipmentType`, `deadheadMiles`) SELECT `id`, `tripId`, `date`, CAST(ROUND(`totalRate` * 100) AS INTEGER), `totalMiles`, `pointA`, `pointB`, `puCount`, `delCount`, `weekNumber`, `year`, `rawMessage`, `parsedAt`, `updatedAt`, `firstPuMillis`, `lastDelMillis`, `route`, `firstPuCityState`, `lastDelCityState`, `durationDays`, CAST(ROUND(`pace` * 100) AS INTEGER), `stopCount`, `isDispute`, `disputeResponseDate`, `disputeCompleted`, CASE WHEN `disputeAmount` IS NULL THEN NULL ELSE CAST(ROUND(`disputeAmount` * 100) AS INTEGER) END, `disputeApplyToLoad`, `disputeAmountApplied`, `actualFinishDate`, `equipmentType`, `deadheadMiles` FROM `loads`",
            createFinalSql = "CREATE TABLE IF NOT EXISTS `loads` (`id` TEXT NOT NULL, `tripId` TEXT NOT NULL, `date` TEXT NOT NULL, `totalRate` INTEGER NOT NULL, `totalMiles` REAL NOT NULL, `pointA` TEXT NOT NULL, `pointB` TEXT NOT NULL, `puCount` INTEGER NOT NULL, `delCount` INTEGER NOT NULL, `weekNumber` INTEGER NOT NULL, `year` INTEGER NOT NULL, `rawMessage` TEXT NOT NULL, `parsedAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `firstPuMillis` INTEGER, `lastDelMillis` INTEGER, `route` TEXT NOT NULL, `firstPuCityState` TEXT NOT NULL, `lastDelCityState` TEXT NOT NULL, `durationDays` REAL NOT NULL, `pace` INTEGER NOT NULL, `stopCount` INTEGER NOT NULL, `isDispute` INTEGER NOT NULL, `disputeResponseDate` TEXT, `disputeCompleted` INTEGER NOT NULL, `disputeAmount` INTEGER, `disputeApplyToLoad` INTEGER NOT NULL, `disputeAmountApplied` INTEGER NOT NULL, `actualFinishDate` TEXT, `equipmentType` TEXT, `deadheadMiles` REAL NOT NULL, PRIMARY KEY(`id`))",
        )
        rebuildMoneyTable(
            db,
            table = "diesel",
            requiredColumn = "weekLabel",
            missingColumnSql = "TEXT NOT NULL DEFAULT ''",
            createNewSql = "CREATE TABLE IF NOT EXISTS `diesel_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekNumber` INTEGER NOT NULL, `year` INTEGER NOT NULL, `weekLabel` TEXT NOT NULL, `weekStartDate` TEXT NOT NULL, `weekEndDate` TEXT NOT NULL, `totalAmount` INTEGER NOT NULL, `gallons` REAL, `pricePerGallon` INTEGER, `discountPricePerGallon` INTEGER, `location` TEXT, `rawExtractedText` TEXT NOT NULL, `sourceFileName` TEXT, `addedAt` INTEGER NOT NULL)",
            insertSql = "INSERT INTO `diesel_new` (`id`, `weekNumber`, `year`, `weekLabel`, `weekStartDate`, `weekEndDate`, `totalAmount`, `gallons`, `pricePerGallon`, `discountPricePerGallon`, `location`, `rawExtractedText`, `sourceFileName`, `addedAt`) SELECT `id`, `weekNumber`, `year`, `weekLabel`, `weekStartDate`, `weekEndDate`, CAST(ROUND(`totalAmount` * 100) AS INTEGER), `gallons`, CASE WHEN `pricePerGallon` IS NULL THEN NULL ELSE CAST(ROUND(`pricePerGallon` * 1000) AS INTEGER) END, CASE WHEN `discountPricePerGallon` IS NULL THEN NULL ELSE CAST(ROUND(`discountPricePerGallon` * 1000) AS INTEGER) END, `location`, `rawExtractedText`, `sourceFileName`, `addedAt` FROM `diesel`",
            createFinalSql = "CREATE TABLE IF NOT EXISTS `diesel` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekNumber` INTEGER NOT NULL, `year` INTEGER NOT NULL, `weekLabel` TEXT NOT NULL, `weekStartDate` TEXT NOT NULL, `weekEndDate` TEXT NOT NULL, `totalAmount` INTEGER NOT NULL, `gallons` REAL, `pricePerGallon` INTEGER, `discountPricePerGallon` INTEGER, `location` TEXT, `rawExtractedText` TEXT NOT NULL, `sourceFileName` TEXT, `addedAt` INTEGER NOT NULL)",
        )
        rebuildMoneyTable(
            db,
            table = "paychecks",
            requiredColumn = "weekLabel",
            missingColumnSql = "TEXT NOT NULL DEFAULT ''",
            createNewSql = "CREATE TABLE IF NOT EXISTS `paychecks_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekNumber` INTEGER NOT NULL, `year` INTEGER NOT NULL, `weekLabel` TEXT NOT NULL, `weekStartDate` TEXT NOT NULL, `weekEndDate` TEXT NOT NULL, `driverName` TEXT, `grossAmount` INTEGER, `netAmount` INTEGER NOT NULL, `rawExtractedText` TEXT NOT NULL, `sourceFileName` TEXT, `addedAt` INTEGER NOT NULL, `sourceFilePath` TEXT)",
            insertSql = "INSERT INTO `paychecks_new` (`id`, `weekNumber`, `year`, `weekLabel`, `weekStartDate`, `weekEndDate`, `driverName`, `grossAmount`, `netAmount`, `rawExtractedText`, `sourceFileName`, `addedAt`, `sourceFilePath`) SELECT `id`, `weekNumber`, `year`, `weekLabel`, `weekStartDate`, `weekEndDate`, `driverName`, CASE WHEN `grossAmount` IS NULL THEN NULL ELSE CAST(ROUND(`grossAmount` * 100) AS INTEGER) END, CAST(ROUND(`netAmount` * 100) AS INTEGER), `rawExtractedText`, `sourceFileName`, `addedAt`, `sourceFilePath` FROM `paychecks`",
            createFinalSql = "CREATE TABLE IF NOT EXISTS `paychecks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `weekNumber` INTEGER NOT NULL, `year` INTEGER NOT NULL, `weekLabel` TEXT NOT NULL, `weekStartDate` TEXT NOT NULL, `weekEndDate` TEXT NOT NULL, `driverName` TEXT, `grossAmount` INTEGER, `netAmount` INTEGER NOT NULL, `rawExtractedText` TEXT NOT NULL, `sourceFileName` TEXT, `addedAt` INTEGER NOT NULL, `sourceFilePath` TEXT)",
        )
        rebuildMoneyTable(
            db,
            table = "penalties",
            requiredColumn = "description",
            missingColumnSql = "TEXT NOT NULL DEFAULT ''",
            createNewSql = "CREATE TABLE IF NOT EXISTS `penalties_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `loadId` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` INTEGER NOT NULL, FOREIGN KEY(`loadId`) REFERENCES `loads`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            insertSql = "INSERT INTO `penalties_new` (`id`, `loadId`, `description`, `amount`) SELECT `id`, `loadId`, `description`, CAST(ROUND(`amount` * 100) AS INTEGER) FROM `penalties`",
            createFinalSql = "CREATE TABLE IF NOT EXISTS `penalties` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `loadId` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` INTEGER NOT NULL, FOREIGN KEY(`loadId`) REFERENCES `loads`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        rebuildMoneyTable(
            db,
            table = "misc_expenses",
            requiredColumn = "receiptPhotoPath",
            missingColumnSql = "TEXT",
            createNewSql = "CREATE TABLE IF NOT EXISTS `misc_expenses_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` INTEGER NOT NULL, `description` TEXT NOT NULL, `date` TEXT NOT NULL, `receiptPhotoPath` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
            insertSql = "INSERT INTO `misc_expenses_new` (`id`, `amount`, `description`, `date`, `receiptPhotoPath`, `createdAt`, `updatedAt`) SELECT `id`, CAST(ROUND(`amount` * 100) AS INTEGER), `description`, `date`, `receiptPhotoPath`, `createdAt`, `updatedAt` FROM `misc_expenses`",
            createFinalSql = "CREATE TABLE IF NOT EXISTS `misc_expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount` INTEGER NOT NULL, `description` TEXT NOT NULL, `date` TEXT NOT NULL, `receiptPhotoPath` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
        )
        rebuildMoneyTable(
            db,
            table = "maintenance_archive",
            requiredColumn = "serviceName",
            missingColumnSql = "TEXT NOT NULL DEFAULT ''",
            createNewSql = "CREATE TABLE IF NOT EXISTS `maintenance_archive_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serviceName` TEXT NOT NULL, `serviceDate` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` INTEGER NOT NULL, `photoPath` TEXT, `ocrText` TEXT, `createdAt` INTEGER NOT NULL)",
            insertSql = "INSERT INTO `maintenance_archive_new` (`id`, `serviceName`, `serviceDate`, `description`, `amount`, `photoPath`, `ocrText`, `createdAt`) SELECT `id`, `serviceName`, `serviceDate`, `description`, CAST(ROUND(`amount` * 100) AS INTEGER), `photoPath`, `ocrText`, `createdAt` FROM `maintenance_archive`",
            createFinalSql = "CREATE TABLE IF NOT EXISTS `maintenance_archive` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serviceName` TEXT NOT NULL, `serviceDate` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` INTEGER NOT NULL, `photoPath` TEXT, `ocrText` TEXT, `createdAt` INTEGER NOT NULL)",
        )
        rebuildMoneyTable(
            db,
            table = "crowd_rates",
            requiredColumn = "fromState",
            missingColumnSql = "TEXT NOT NULL DEFAULT ''",
            createNewSql = "CREATE TABLE IF NOT EXISTS `crowd_rates_new` (`id` TEXT NOT NULL, `fromState` TEXT NOT NULL, `toState` TEXT NOT NULL, `rpm` INTEGER NOT NULL, `rate` INTEGER NOT NULL, `miles` REAL NOT NULL, `reportedAtMillis` INTEGER NOT NULL, `source` TEXT NOT NULL, `peerLabel` TEXT, `syncedAtMillis` INTEGER NOT NULL, `equipmentType` TEXT, PRIMARY KEY(`id`))",
            insertSql = "INSERT INTO `crowd_rates_new` (`id`, `fromState`, `toState`, `rpm`, `rate`, `miles`, `reportedAtMillis`, `source`, `peerLabel`, `syncedAtMillis`, `equipmentType`) SELECT `id`, `fromState`, `toState`, CAST(ROUND(`rpm` * 100) AS INTEGER), CAST(ROUND(`rate` * 100) AS INTEGER), `miles`, `reportedAtMillis`, `source`, `peerLabel`, `syncedAtMillis`, `equipmentType` FROM `crowd_rates`",
            createFinalSql = "CREATE TABLE IF NOT EXISTS `crowd_rates` (`id` TEXT NOT NULL, `fromState` TEXT NOT NULL, `toState` TEXT NOT NULL, `rpm` INTEGER NOT NULL, `rate` INTEGER NOT NULL, `miles` REAL NOT NULL, `reportedAtMillis` INTEGER NOT NULL, `source` TEXT NOT NULL, `peerLabel` TEXT, `syncedAtMillis` INTEGER NOT NULL, `equipmentType` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_loads_tripId` ON `loads` (`tripId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loads_date` ON `loads` (`date`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loads_weekNumber_year` ON `loads` (`weekNumber`, `year`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loads_parsedAt` ON `loads` (`parsedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loads_updatedAt` ON `loads` (`updatedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loads_firstPuCityState` ON `loads` (`firstPuCityState`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_loads_isDispute_disputeCompleted` ON `loads` (`isDispute`, `disputeCompleted`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_penalties_loadId` ON `penalties` (`loadId`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_paychecks_weekNumber_year` ON `paychecks` (`weekNumber`, `year`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_paychecks_addedAt` ON `paychecks` (`addedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diesel_weekNumber_year` ON `diesel` (`weekNumber`, `year`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diesel_addedAt` ON `diesel` (`addedAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_archive_serviceDate` ON `maintenance_archive` (`serviceDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_maintenance_archive_createdAt` ON `maintenance_archive` (`createdAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_crowd_rates_fromState_reportedAtMillis` ON `crowd_rates` (`fromState`, `reportedAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_crowd_rates_source_reportedAtMillis` ON `crowd_rates` (`source`, `reportedAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_crowd_rates_fromState_equipmentType_reportedAtMillis` ON `crowd_rates` (`fromState`, `equipmentType`, `reportedAtMillis`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_misc_expenses_date` ON `misc_expenses` (`date`)")
    }
}

private fun rebuildMoneyTable(
    db: SupportSQLiteDatabase,
    table: String,
    requiredColumn: String,
    missingColumnSql: String,
    createNewSql: String,
    insertSql: String,
    createFinalSql: String,
) {
    if (!db.hasColumn(table, requiredColumn)) {
        // An empty stub can be replaced. A table that already has rows must keep them.
        if (!db.hasTable(table) || tableRowCount(db, table) == 0) {
            db.execSQL("DROP TABLE IF EXISTS `$table`")
            db.execSQL(createFinalSql)
            return
        }
        db.addColumnIfMissing(table, requiredColumn, missingColumnSql)
    }
    db.execSQL(createNewSql)
    db.execSQL(insertSql)
    db.execSQL("DROP TABLE `$table`")
    db.execSQL("ALTER TABLE `${table}_new` RENAME TO `$table`")
}

private fun tableRowCount(db: SupportSQLiteDatabase, table: String): Int {
    db.query("SELECT COUNT(*) FROM `$table`").use { cursor ->
        return if (cursor.moveToFirst()) cursor.getInt(0) else 0
    }
}

