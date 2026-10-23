package com.truckerload.data.backup

import androidx.annotation.Keep

/**
 * Portable app preferences for cloud / file restore.
 * Omits secrets (auth tokens, bot token, biometric).
 */
@Keep
data class BackupAppSettings(
    val themeModeOrdinal: Int? = null,
    val languageOrdinal: Int? = null,
    val reduceMotion: Boolean? = null,
    val oledDark: Boolean? = null,
    val dynamicColor: Boolean? = null,
    val parserAutoUpdate: Boolean? = null,
    val parserPriceThresholdPercent: Double? = null,
    val quietHoursEnabled: Boolean? = null,
    val quietHoursStart: Int? = null,
    val quietHoursEnd: Int? = null,
    val notifyMissingWeek: Boolean? = null,
    val notifyMaintenance: Boolean? = null,
    val lastEquipmentType: String? = null,
    val weeklyProfitGoal: Double? = null,
    val rpmMinProfit: Double? = null,
    val rpmTargetProfit: Double? = null,
    /** Read-compat for old backups only; never written or restored (pair again on a new phone). */
    val telegramChatId: Long? = null,
    val loadWeekStartDay: Int? = null,
    val dieselWeekStartDay: Int? = null,
    /** "mi" or "km" when the driver chose. Null means no explicit choice. */
    val distanceUnit: String? = null,
)
