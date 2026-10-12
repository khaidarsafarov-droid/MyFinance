package com.truckerload.data.preferences

/**
 * Opens the device profile: bind the profile store, merge the profile, then start the session.
 */
object AuthLogin {
    fun completeLogin(
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
        userId: String,
        profile: UserProfile,
        rememberMe: Boolean = true,
        accessToken: String? = null,
        refreshToken: String? = null,
        googleIdToken: String? = null,
        provider: AuthProvider = when {
            !profile.googleId.isNullOrBlank() -> AuthProvider.GOOGLE
            else -> AuthProvider.EMAIL
        },
    ) {
        val requestedId = userId.trim()
        require(requestedId.isNotBlank()) { "userId required" }
        val googleSub = profile.googleId?.trim()?.takeIf { it.isNotBlank() }
        val id = authStore.currentUserIdOrNull()
            ?: if (googleSub != null) AccountIds.fromGoogleSub(googleSub) else requestedId
        require(profile.email.isNotBlank() || id.startsWith("local_") || id.startsWith("google_") || id == AccountIds.LOCAL_DEV) {
            "email required for account session"
        }
        userProfileStore.bindUser(id)
        // Preserve nickname, and any name/photo the driver set in the app (Google is seed-only).
        val merged = ProfileIdentity.mergeLoginProfile(
            incoming = profile,
            existing = userProfileStore.profile.value,
        )
        userProfileStore.saveProfile(merged)
        // Always persist identity on disk — next cold start must not ask for login again.
        authStore.login(
            userId = id,
            email = profile.email,
            rememberMe = true,
            accessToken = accessToken,
            refreshToken = refreshToken,
            googleSub = googleSub,
            provider = provider,
            googleIdToken = googleIdToken,
        )
    }
}
