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
    ) {
        val id = userId.trim()
        require(id.isNotBlank()) { "userId required" }
        require(profile.email.isNotBlank() || id.startsWith("local_") || id == AccountIds.LOCAL_DEV) {
            "email required for account session"
        }
        userProfileStore.bindUser(id)
        userProfileStore.saveProfile(profile)
        authStore.login(
            userId = id,
            email = profile.email,
            rememberMe = rememberMe,
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
    }

    /**
     * Resolves account id and completes login, or returns false when identity is incomplete
     * (e.g. Google token without email).
     */
    fun tryCompleteLogin(
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
        supabaseUserId: String?,
        profile: UserProfile,
        rememberMe: Boolean = true,
        accessToken: String? = null,
        refreshToken: String? = null,
    ): Boolean {
        val userId = AccountIds.resolveOrNull(supabaseUserId, profile.email) ?: return false
        completeLogin(
            authStore = authStore,
            userProfileStore = userProfileStore,
            userId = userId,
            profile = profile,
            rememberMe = rememberMe,
            accessToken = accessToken,
            refreshToken = refreshToken,
        )
        return true
    }
}
