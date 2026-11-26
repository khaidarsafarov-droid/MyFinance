package com.truckerload.sync

import android.content.Context
import com.truckerload.data.preferences.AuthStore
import kotlinx.coroutines.delay

/**
 * Stops account-bound foreground services **before** auth tokens are wiped on logout.
 */
object SessionTeardown {

    /**
     * Stops Telegram foreground service **before** auth tokens are wiped on logout.
     */
    suspend fun beforeLogout(context: Context) {
        val app = context.applicationContext
        TelegramBotPoller.stopForLogout(app)
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
