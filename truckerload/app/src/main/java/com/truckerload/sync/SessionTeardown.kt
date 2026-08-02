package com.truckerload.sync

import android.content.Context
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
        // FIX: brief settle so FGS stop intents are delivered before Room/auth teardown
        delay(300)
    }
}
