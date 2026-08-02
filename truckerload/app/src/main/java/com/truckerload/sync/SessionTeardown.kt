package com.truckerload.sync

import android.content.Context
import com.truckerload.data.preferences.AuthStore
import kotlinx.coroutines.delay

/**
 * Stops account-bound foreground services and clears remote presence
 * **before** auth tokens are wiped on logout.
 */
object SessionTeardown {

    /**
     * Clears friends presence/route while the session is still valid, then
     * stops Telegram and location foreground services.
     */
    suspend fun beforeLogout(context: Context) {
        val app = context.applicationContext
        FriendsLocationShareService.stopForLogout(app)
        TelegramBotForegroundService.stopForLogout(app)
        delay(300)
    }

    /**
     * Full sign-out path shared by Settings, drawer, and [com.truckerload.data.repository.auth.AuthRepository].
     */
    suspend fun signOut(
        context: Context,
        authStore: AuthStore,
        endSession: () -> Unit,
    ) {
        beforeLogout(context)
        endSession()
        authStore.logout()
    }
}
