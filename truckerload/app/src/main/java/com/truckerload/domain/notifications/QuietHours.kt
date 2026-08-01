package com.truckerload.domain.notifications

/**
 * Pure helper for mindful notification quiet hours.
 * When [startHour] wraps past midnight (e.g. 22 → 7), the quiet window spans overnight.
 */
object QuietHours {
    fun isActive(
        nowHour: Int,
        startHour: Int,
        endHour: Int,
        enabled: Boolean,
    ): Boolean {
        if (!enabled) return false
        val now = nowHour.coerceIn(0, 23)
        val start = startHour.coerceIn(0, 23)
        val end = endHour.coerceIn(0, 23)
        if (start == end) return true
        return if (start < end) {
            now in start until end
        } else {
            now >= start || now < end
        }
    }
}
