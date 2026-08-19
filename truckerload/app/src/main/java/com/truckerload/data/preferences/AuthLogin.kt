package com.truckerload.data.preferences

/**
 * Completes a multi-user login: bind profile namespace, save identity, then open the session.
 * Order matters so [UserProfileStore] is bound before [AuthStore.login] triggers DB switch.
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
        val id = userId.trim()
        require(id.isNotBlank()) { "userId required" }
        require(profile.email.isNotBlank() || id.startsWith("local_") || id.startsWith("google_") || id == AccountIds.LOCAL_DEV) {
            "email required for account session"
        }
        userProfileStore.bindUser(id)
        // Preserve / prefer cloud nickname; never wipe a stored handle with a blank login payload.
        val existingNick = userProfileStore.profile.value?.nickname
        val merged = when {
            !profile.nickname.isNullOrBlank() -> profile
            !existingNick.isNullOrBlank() -> profile.copy(nickname = existingNick)
            else -> profile
        }
        userProfileStore.saveProfile(merged)
        // Always persist identity on disk — next cold start must not ask for login again.
        authStore.login(
            userId = id,
            email = profile.email,
            rememberMe = true,
            accessToken = accessToken,
            refreshToken = refreshToken,
            googleSub = profile.googleId,
            provider = provider,
            googleIdToken = googleIdToken,
        )
    }

    /**
     * Resolves account id and completes login, or returns false when identity is incomplete
     * (e.g. Google token without email and without `sub`).
     */
    fun tryCompleteLogin(
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
        supabaseUserId: String?,
        profile: UserProfile,
        rememberMe: Boolean = true,
        accessToken: String? = null,
        refreshToken: String? = null,
        googleIdToken: String? = null,
    ): Boolean {
        val userId = AccountIds.resolveOrNull(
            supabaseUserId = supabaseUserId,
            email = profile.email,
            googleSub = profile.googleId,
        ) ?: return false
        completeLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            userId = userId,
            profile = profile,
            rememberMe = rememberMe,
            accessToken = accessToken,
            refreshToken = refreshToken,
            googleIdToken = googleIdToken,
        )
        return true
    }
}
