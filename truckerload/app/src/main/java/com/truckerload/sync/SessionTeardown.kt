package com.truckerload.sync

import android.content.Context
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.remote.GoogleSignInClients
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Stops account-bound foreground services **before** auth tokens are wiped on logout.
 */
object SessionTeardown {

    /**
     * Stops Telegram foreground service **before** auth tokens are wiped on logout.
     */
    suspend fun beforeLogout(context: Context) {
        val app = context.applicationContext
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
        // Play Services keeps the last Google account after app logout; clear it so
        // the next Drive connect can pick a different account.
        withTimeoutOrNull(5_000) {
            GoogleSignInClients.signOutDevice(context)
        }
    }
}
