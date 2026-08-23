package com.truckerload.domain.crowd

import com.truckerload.domain.model.EquipmentType

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
 * Allowed fields are numbers plus coarse 2-letter US states (or `WA-OR` lane)
 * and optional equipment type. Do **not** log instances of this class (same
 * rule as JWT / OCR / signed URLs).
 *
 * There is currently no HTTP Crowd RPM publisher; map/stats use only the
 * signed-in user's local loads. Community share UI and opt-in were removed.
 *
 * @see <a href="https://github.com/khaidarsafarov-droid/MyFinance/blob/main/truckerload/docs/CROWD_RPM_PRIVACY.md">docs/CROWD_RPM_PRIVACY.md</a>
 */
data class AnonymizedRpmSample(
    val rpm: Double,
    val miles: Double,
    val region: String? = null,
    val weekNumber: Int = 0,
    val fromState: String = "",
    val toState: String = "",
    val week: Int = 0,
    val year: Int = 0,
    val equipmentType: EquipmentType? = null,
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
