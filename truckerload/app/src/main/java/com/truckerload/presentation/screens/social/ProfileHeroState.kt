package com.truckerload.presentation.screens.social

import com.truckerload.domain.premium.PremiumOfferQuote
import com.truckerload.presentation.components.RpmTier
import com.truckerload.presentation.components.rpmTier
import java.util.Locale

enum class ProfileRpmChip {
    ABOVE,
    MID,
    BELOW,
}

enum class ProfilePremiumPhase {
    ACTIVE,
    FREE_MONTH,
    PAID,
    UNKNOWN,
}

data class ProfilePremiumModel(
    val phase: ProfilePremiumPhase,
    val price: String?,
)

/** Pure profile-hero decisions. Price text always comes from Play. */
object ProfileHeroState {
    fun initials(givenName: String, familyName: String): String? {
        val given = firstLetter(givenName)
        val family = firstLetter(familyName)
        return when {
            given != null && family != null -> "$given$family"
            given != null -> given
            family != null -> family
            else -> null
        }
    }

    fun isEmpty(totalLoads: Int): Boolean = totalLoads <= 0

    fun rpmChip(averageRpm: Double, redThreshold: Double, greenThreshold: Double): ProfileRpmChip =
        when (rpmTier(averageRpm, redThreshold, greenThreshold)) {
            RpmTier.Good -> ProfileRpmChip.ABOVE
            RpmTier.Low -> ProfileRpmChip.BELOW
            RpmTier.Ok, null -> ProfileRpmChip.MID
        }

    fun premium(subscribed: Boolean, offerLoaded: Boolean, quote: PremiumOfferQuote?): ProfilePremiumModel {
        if (subscribed) {
            return ProfilePremiumModel(
                ProfilePremiumPhase.ACTIVE,
                quote?.formattedPrice?.takeIf { it.isNotBlank() },
            )
        }
        if (!offerLoaded) return ProfilePremiumModel(ProfilePremiumPhase.UNKNOWN, null)
        val price = quote?.formattedPrice?.takeIf { it.isNotBlank() }
            ?: return ProfilePremiumModel(ProfilePremiumPhase.UNKNOWN, null)
        val phase = if (quote.includesFreeMonth) ProfilePremiumPhase.FREE_MONTH else ProfilePremiumPhase.PAID
        return ProfilePremiumModel(phase, price)
    }

    private fun firstLetter(value: String): String? {
        val letter = value.trim().firstOrNull { it.isLetter() } ?: return null
        return letter.uppercase(Locale.ROOT)
    }
}
