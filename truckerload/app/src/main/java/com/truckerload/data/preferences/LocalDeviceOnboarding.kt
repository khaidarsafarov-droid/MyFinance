package com.truckerload.data.preferences

/**
 * First-launch local identity: first + last name, no Google/email account.
 * Room and prefs stay keyed by [AccountIds.LOCAL_DEV] on this device.
 */
object LocalDeviceOnboarding {
    const val LOCAL_EMAIL = "local@truckerload.local"

    fun namesAreValid(givenName: String, familyName: String): Boolean =
        givenName.trim().isNotBlank() && familyName.trim().isNotBlank()

    fun complete(
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
        givenName: String,
        familyName: String,
    ) {
        val given = givenName.trim()
        val family = familyName.trim()
        require(namesAreValid(given, family)) { "NAME_REQUIRED" }
        AuthLogin.completeLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            userId = AccountIds.LOCAL_DEV,
            profile = UserProfile(
                email = LOCAL_EMAIL,
                givenName = given,
                familyName = family,
                photoUrl = null,
                customDisplayName = true,
            ),
            rememberMe = true,
            provider = AuthProvider.LOCAL,
        )
        userProfileStore.setSetupComplete(true)
    }
}
