package com.truckerload.data.preferences

/**
 * Completes a multi-user login: bind profile namespace, save identity, then open the session.
 * Order matters so [UserProfileStore] is bound before [AuthStore.login] triggers DB switch.
 */
object AuthSession {
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
}
