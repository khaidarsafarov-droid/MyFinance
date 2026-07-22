package com.truckerload.sync

/** Pure helpers for Telegram API error handling in the sync engine. */
object TelegramAuthErrors {
    /** Unauthorized token — stop FGS so we do not spin on a bad credential. */
    fun shouldStopService(errorMessage: String?): Boolean =
        errorMessage?.contains("401") == true
}
