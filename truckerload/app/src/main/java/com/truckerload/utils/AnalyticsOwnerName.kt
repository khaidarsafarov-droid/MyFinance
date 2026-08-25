package com.truckerload.utils

import com.truckerload.data.preferences.ProfileIdentity

/** Resolves first/last name for a shareable My numbers report. */
object AnalyticsOwnerName {

    fun fromProfile(
        givenName: String?,
        familyName: String?,
        email: String?,
        socialDisplayName: String?,
    ): Pair<String, String> {
        val given = givenName.orEmpty().trim()
        val family = familyName.orEmpty().trim()
        val joined = display(given, family)
        if (!ProfileIdentity.isPlaceholderName(joined) && joined != email?.trim()) {
            return given to family
        }
        val social = socialDisplayName.orEmpty().trim()
        if (!ProfileIdentity.isPlaceholderName(social) && social != email?.trim()) {
            val parts = social.split(Regex("\\s+"), limit = 2)
            return parts[0] to parts.getOrNull(1).orEmpty()
        }
        return "" to ""
    }

    fun display(givenName: String, familyName: String): String =
        listOf(givenName.trim(), familyName.trim()).filter { it.isNotBlank() }.joinToString(" ")
}
