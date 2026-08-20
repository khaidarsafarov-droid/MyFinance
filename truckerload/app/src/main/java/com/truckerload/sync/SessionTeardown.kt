package com.truckerload.sync

import android.content.Context
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.remote.GoogleSignInClients
import com.truckerload.data.sync.DeviceSlotBinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

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
        runCatching { DeviceSlotBinder(app).unregisterCurrentDevice() }
        FriendsLocationShareScheduler.stopAndClear(app)
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
        // the next "Sign in with Google" can pick a different account, and Drive
        // does not keep a stale signed-in session.
        withTimeoutOrNull(5_000) {
            GoogleSignInClients.signOutDevice(context)
        }
    }
}
