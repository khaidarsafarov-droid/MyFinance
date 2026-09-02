package com.truckerload.data.auth

import android.content.Context
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AuthSessionHealth
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.preferences.UserProfileStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Cold-start session check. Identity lives on this device (Room + encrypted prefs).
 * Never blocks the UI; only updates [AuthStore.sessionHealth].
 *
 * Google cold-start must NOT call Credential Manager. Even `silent=true` can show
 * the Google account sheet on every launch.
 */
object SilentAuthRestorer {

    @Suppress("UNUSED_PARAMETER")
    suspend fun restore(
        context: Context,
        authStore: AuthStore,
        userProfileStore: UserProfileStore,
    ): AuthSessionHealth = withContext(Dispatchers.IO) {
        if (BuildConfig.LOCAL_ONLY_MODE) {
            authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
            return@withContext AuthSessionHealth.VERIFIED
        }
        if (authStore.sessionOrNull() == null) {
            return@withContext AuthSessionHealth.VERIFIED
        }
        authStore.markSessionHealth(AuthSessionHealth.VERIFIED)
        AuthSessionHealth.VERIFIED
    }
}
