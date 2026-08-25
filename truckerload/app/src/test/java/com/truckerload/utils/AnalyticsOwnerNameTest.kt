package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsOwnerNameTest {

    @Test
    fun fromProfile_prefersGivenAndFamilyOverSocial() {
        val parts = AnalyticsOwnerName.fromProfile(
            givenName = "Ivan",
            familyName = "Petrov",
            email = "ivan@example.com",
            socialDisplayName = "Nickname",
        )
        assertEquals("Ivan" to "Petrov", parts)
    }

    @Test
    fun fromProfile_ignoresPlaceholderAndEmail() {
        val parts = AnalyticsOwnerName.fromProfile(
            givenName = "Driver",
            familyName = "",
            email = "ivan@example.com",
            socialDisplayName = "ivan@example.com",
        )
        assertEquals("" to "", parts)
    }

    @Test
    fun fromProfile_fallsBackToSocialFullName() {
        val parts = AnalyticsOwnerName.fromProfile(
            givenName = "",
            familyName = "",
            email = "ivan@example.com",
            socialDisplayName = "Anna Volkova",
        )
        assertEquals("Anna" to "Volkova", parts)
    }

    @Test
    fun display_joinsNonBlankParts() {
        assertEquals("Ivan Petrov", AnalyticsOwnerName.display("Ivan", "Petrov"))
        assertEquals("Ivan", AnalyticsOwnerName.display("Ivan", "  "))
        assertEquals("", AnalyticsOwnerName.display(" ", ""))
    }
}
