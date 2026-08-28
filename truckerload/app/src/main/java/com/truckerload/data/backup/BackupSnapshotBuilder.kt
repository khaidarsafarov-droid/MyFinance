package com.truckerload.data.backup

import android.content.Context
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.local.toDomain
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.RpmThresholdsStore
import com.truckerload.data.preferences.SettingsDataStore
import com.truckerload.data.preferences.WeeklyProfitGoalStore
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.MaintenanceArchiveEntry
import com.truckerload.domain.model.MaintenanceTask

/** Builds a full-account [BackupData] (journal + ТО + settings) for Drive / cloud / file. */
object BackupSnapshotBuilder {

    suspend fun build(context: Context, db: AppDatabase): BackupData {
        val app = context.applicationContext
        val accountId = AuthStore(app).currentUserIdOrNull()
        val loads = LoadRepository(db).getAllLoadsOnce()
        val paychecks = PaycheckRepository(db).getAllPaychecksOnce()
        val diesel = DieselRepository(db).getAllDieselOnce()
        val maintenanceDao = db.maintenanceDao()
        val tasks = maintenanceDao.getAllTasksOnce().map { it.toDomain() }
        val archive = maintenanceDao.getAllArchiveOnce().map { it.toDomain() }
        val settings = exportSettings(app, accountId)
        return BackupData(
            schemaVersion = BackupSchema.CURRENT,
            version = BackupSchema.CURRENT,
            accountId = accountId,
            loads = loads,
            paychecks = paychecks,
            diesel = diesel,
            maintenanceTasks = tasks.map { it.toBackup() },
            maintenanceArchive = archive.map { it.toBackup() },
            appSettings = settings,
        )
    }

    /** Journal / ТО rows only — [appSettings] alone must not trigger an empty export. */
    fun hasExportableContent(backup: BackupData): Boolean =
        backup.loads.isNotEmpty() ||
                backup.paychecks.isNotEmpty() ||
                backup.diesel.isNotEmpty() ||
                backup.maintenanceTasks.isNotEmpty() ||
                backup.maintenanceArchive.isNotEmpty()

    private suspend fun exportSettings(context: Context, accountId: String?): BackupAppSettings {
        val settings = SettingsDataStore(context)
        val userId = accountId ?: return BackupAppSettings(
            themeModeOrdinal = settings.getThemeModeOnce().ordinal,
            languageOrdinal = settings.getExplicitLanguageOnce()?.ordinal,
            reduceMotion = settings.getReduceMotionOnce(),
            oledDark = settings.getOledDarkOnce(),
            dynamicColor = settings.getDynamicColorOnce(),
            parserAutoUpdate = settings.getParserAutoUpdateOnce(),
            parserPriceThresholdPercent = settings.getParserPriceThresholdOnce(),
            quietHoursEnabled = settings.getQuietHoursEnabledOnce(),
            quietHoursStart = settings.getQuietHoursStartOnce(),
            quietHoursEnd = settings.getQuietHoursEndOnce(),
            notifyMissingWeek = settings.getNotifyMissingWeekOnce(),
            notifyMaintenance = settings.getNotifyMaintenanceOnce(),
            lastEquipmentType = settings.getLastEquipmentTypeOnce()?.name,
            telegramChatId = settings.getTelegramChatIdOnce(),
            loadWeekStartDay = settings.getLoadWeekStartDayOnce().calendarDay,
            dieselWeekStartDay = settings.getDieselWeekStartDayOnce().calendarDay,
        )
        val goals = WeeklyProfitGoalStore(context, userId)
        val rpm = RpmThresholdsStore(context, userId).thresholds.value
        return BackupAppSettings(
            themeModeOrdinal = settings.getThemeModeOnce().ordinal,
            languageOrdinal = settings.getExplicitLanguageOnce()?.ordinal,
            reduceMotion = settings.getReduceMotionOnce(),
            oledDark = settings.getOledDarkOnce(),
            dynamicColor = settings.getDynamicColorOnce(),
            parserAutoUpdate = settings.getParserAutoUpdateOnce(),
            parserPriceThresholdPercent = settings.getParserPriceThresholdOnce(),
            quietHoursEnabled = settings.getQuietHoursEnabledOnce(),
            quietHoursStart = settings.getQuietHoursStartOnce(),
            quietHoursEnd = settings.getQuietHoursEndOnce(),
            notifyMissingWeek = settings.getNotifyMissingWeekOnce(),
            notifyMaintenance = settings.getNotifyMaintenanceOnce(),
            lastEquipmentType = settings.getLastEquipmentTypeOnce()?.name,
            weeklyProfitGoal = goals.getGoal().takeIf { goals.isConfigured },
            rpmMinProfit = rpm.minProfit,
            rpmTargetProfit = rpm.targetProfit,
            telegramChatId = settings.getTelegramChatIdOnce(),
            loadWeekStartDay = settings.getLoadWeekStartDayOnce().calendarDay,
            dieselWeekStartDay = settings.getDieselWeekStartDayOnce().calendarDay,
        )
    }

    private fun MaintenanceTask.toBackup() = BackupMaintenanceTask(
        id = id,
        title = title,
        startDate = startDate,
        reminderType = reminderType.name,
        intervalMiles = intervalMiles,
        odometerAtStart = odometerAtStart,
        dueDate = dueDate,
        isCompleted = isCompleted,
        completedAt = completedAt,
        notifiedAt = notifiedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun MaintenanceArchiveEntry.toBackup() = BackupMaintenanceArchive(
        id = id,
        serviceName = serviceName,
        serviceDate = serviceDate,
        description = description,
        amount = amount,
        photoPath = photoPath,
        ocrText = ocrText,
        createdAt = createdAt,
    )
}
