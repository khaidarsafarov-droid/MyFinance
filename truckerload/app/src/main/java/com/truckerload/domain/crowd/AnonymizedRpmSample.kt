package com.truckerload.domain.crowd

/**
 * The **only** type allowed to leave the device for Crowd RPM (community stats).
 *
 * Invariant — must **not** include:
 * - `Load.id` / `tripId`
 * - `rawMessage`
 * - exact PU/DEL addresses, city names, or facility codes
 * - calendar dates / timestamps
 * - user name, nickname, or account id
 *
 * Allowed fields are numbers plus an optional coarse region (2-letter US state
 * or `WA-OR` lane). Do **not** log instances of this class (same rule as JWT /
 * OCR / signed URLs).
 *
 * There is currently no HTTP Crowd RPM publisher; any future send must pass
 * through [CrowdRpmShareGate] and [com.truckerload.data.preferences.SettingsDataStore.crowdStatsOptIn].
 *
 * @see <a href="https://github.com/khaidarsafarov-droid/MyFinance/blob/main/truckerload/docs/CROWD_RPM_PRIVACY.md">docs/CROWD_RPM_PRIVACY.md</a>
 */
data class AnonymizedRpmSample(
    val rpm: Double,
    val miles: Double,
    val region: String?,
    val weekNumber: Int,
)

/**
 * Local weekly roll-up built only from [AnonymizedRpmSample] (no Load fields).
 */
data class CrowdRpmWeekSummary(
    val sampleCount: Int,
    val avgRpm: Double,
    val totalMiles: Double,
    val weekNumber: Int,
)
