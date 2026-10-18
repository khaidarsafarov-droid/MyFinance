package com.truckerload.presentation.screens.settings

import com.truckerload.R
import com.truckerload.domain.premium.PremiumFeature
import com.truckerload.domain.premium.PremiumOfferQuote
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** What the settings hero is allowed to promise. Price text always comes from Play. */
enum class SettingsPremiumPhase {
    /** Play reports an active subscription or free-month offer. */
    ACTIVE,
    /** Play returned a free-month offer and a recurring price. */
    FREE_MONTH,
    /** Play returned only a paid price. */
    PAID,
    /** Offer has not loaded, or Play sent no price. */
    UNKNOWN,
}

data class SettingsPremiumHeroModel(
    val phase: SettingsPremiumPhase,
    val price: String?,
)

enum class SettingsPremiumMark {
    NONE,
    CROWN,
    LOCK,
}

/**
 * The six hero lines, in the order the hero draws them.
 * Every [PremiumFeature] belongs to exactly one group.
 */
enum class SettingsPremiumGroup {
    TELEGRAM,
    ANALYTICS,
    GOAL,
    MAP_AND_TAX,
    SCANNER_AND_CAMERA,
    EXPORT_AND_DRIVE,
}

data class SettingsSyncMoment(
    val isToday: Boolean,
    /** HH:mm */
    val clock: String,
    /** dd.MM.yyyy HH:mm */
    val dateTime: String,
)

object SettingsPremiumState {
    fun hero(subscribed: Boolean, offerLoaded: Boolean, quote: PremiumOfferQuote?): SettingsPremiumHeroModel {
        if (subscribed) {
            return SettingsPremiumHeroModel(SettingsPremiumPhase.ACTIVE, quote?.formattedPrice?.takeIf { it.isNotBlank() })
        }
        if (!offerLoaded) return SettingsPremiumHeroModel(SettingsPremiumPhase.UNKNOWN, null)
        val price = quote?.formattedPrice?.takeIf { it.isNotBlank() } ?: return SettingsPremiumHeroModel(
            SettingsPremiumPhase.UNKNOWN,
            null,
        )
        val phase = if (quote.includesFreeMonth) SettingsPremiumPhase.FREE_MONTH else SettingsPremiumPhase.PAID
        return SettingsPremiumHeroModel(phase, price)
    }

    fun mark(premiumRow: Boolean, unlocked: Boolean): SettingsPremiumMark = when {
        !premiumRow -> SettingsPremiumMark.NONE
        unlocked -> SettingsPremiumMark.CROWN
        else -> SettingsPremiumMark.LOCK
    }

    fun groupOf(feature: PremiumFeature): SettingsPremiumGroup = when (feature) {
        PremiumFeature.TELEGRAM -> SettingsPremiumGroup.TELEGRAM
        PremiumFeature.ANALYTICS -> SettingsPremiumGroup.ANALYTICS
        PremiumFeature.WEEKLY_GOAL -> SettingsPremiumGroup.GOAL
        PremiumFeature.MAP, PremiumFeature.TAX -> SettingsPremiumGroup.MAP_AND_TAX
        PremiumFeature.SCANNER, PremiumFeature.CAMERA -> SettingsPremiumGroup.SCANNER_AND_CAMERA
        PremiumFeature.EXPORT, PremiumFeature.DRIVE -> SettingsPremiumGroup.EXPORT_AND_DRIVE
    }

    /** String resource for one hero line. Declaration order of [SettingsPremiumGroup] is the grid order. */
    fun featureLabelRes(group: SettingsPremiumGroup): Int = when (group) {
        SettingsPremiumGroup.TELEGRAM -> R.string.settings_premium_feature_telegram
        SettingsPremiumGroup.ANALYTICS -> R.string.settings_premium_feature_analytics
        SettingsPremiumGroup.GOAL -> R.string.settings_premium_feature_goal
        SettingsPremiumGroup.MAP_AND_TAX -> R.string.settings_premium_feature_map_tax
        SettingsPremiumGroup.SCANNER_AND_CAMERA -> R.string.settings_premium_feature_scan
        SettingsPremiumGroup.EXPORT_AND_DRIVE -> R.string.settings_premium_feature_export
    }

    fun syncMoment(lastSyncAtMillis: Long, nowMillis: Long, zone: ZoneId): SettingsSyncMoment? {
        if (lastSyncAtMillis <= 0L) return null
        val sync = Instant.ofEpochMilli(lastSyncAtMillis).atZone(zone)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val clock = sync.format(CLOCK)
        val dateTime = sync.format(DATE_TIME)
        return SettingsSyncMoment(
            isToday = sync.toLocalDate() == now.toLocalDate(),
            clock = clock,
            dateTime = dateTime,
        )
    }

    fun hourLabel(hour: Int): String = String.format(Locale.US, "%02d:00", hour.coerceIn(0, 23))

    fun rpmAmount(value: Double): String {
        val cents = (value * 100.0).roundToLong()
        return if (cents % 100L == 0L) {
            (cents / 100L).toString()
        } else {
            String.format(Locale.US, "%.2f", cents / 100.0)
        }
    }

    private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.US)
}
