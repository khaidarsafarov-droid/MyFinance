package com.truckerload.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.truckerload.utils.BrandConstants
import com.truckerload.utils.LogRedactor

/**
 * Pushes TruckoRig name + app logo onto a BotFather bot via Bot API.
 * Called when the user saves a token, and again from the local poller if
 * the first attempt failed (photo changes are limited to once per 24h).
 */
object TelegramBotBranding {

    const val DISPLAY_NAME = BrandConstants.DISPLAY_NAME
    internal const val KEY_NAME = "bot_display_name_fp"
    internal const val KEY_PHOTO = "bot_profile_photo_fp"

    /** Same file as the Telegram sync worker / FGS setup flags. */
    private const val PREFS_NAME = "telegram_sync"
    private const val TAG = "TelegramBotBrand"

    fun nameStamp(tokenFp: String, name: String = DISPLAY_NAME): String = "$tokenFp:$name"

    fun isNameApplied(prefs: SharedPreferences, token: String): Boolean =
        stored(prefs, KEY_NAME) == nameStamp(TelegramBotTokenFingerprint.of(token))

    fun isPhotoApplied(prefs: SharedPreferences, token: String): Boolean =
        stored(prefs, KEY_PHOTO) == TelegramBotTokenFingerprint.of(token)

    suspend fun apply(context: Context, token: String, forceName: Boolean = false) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fp = TelegramBotTokenFingerprint.of(trimmed)
        val needName = forceName || !isNameApplied(prefs, trimmed)
        val needPhoto = !isPhotoApplied(prefs, trimmed)
        if (!needName && !needPhoto) return
        val api = TelegramApi(trimmed)
        if (needName) {
            api.setMyName(DISPLAY_NAME)
                .onSuccess { prefs.edit { putString(KEY_NAME, nameStamp(fp)) } }
                .onFailure { e -> Log.w(TAG, "setMyName: ${LogRedactor.redact(e.message)}") }
        }
        if (needPhoto) {
            val jpeg = TelegramBotLogoJpeg.encode(app) ?: return
            api.setMyProfilePhoto(jpeg)
                .onSuccess { prefs.edit { putString(KEY_PHOTO, fp) } }
                .onFailure { e ->
                    Log.w(TAG, "setMyProfilePhoto: ${LogRedactor.redact(e.message)}")
                }
        }
    }

    private fun stored(prefs: SharedPreferences, key: String): String =
        prefs.all[key] as? String ?: ""
}
