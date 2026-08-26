package com.truckerload.data.remote

import java.security.MessageDigest

/** Stable non-secret id for a BotFather token, used to apply brand setup once per bot. */
object TelegramBotTokenFingerprint {

    fun of(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { byte -> "%02x".format(byte) }
    }
}
