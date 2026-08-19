package com.truckerload.data.preferences

import android.content.Context
import androidx.core.content.edit
import com.truckerload.domain.voice.CallPrivacy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Local call privacy: who may ring this device, and per-group call policy.
 */
class CallPrivacyStore(context: Context) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _privacy = MutableStateFlow(readPrivacy())
    val privacy: StateFlow<CallPrivacy> = _privacy.asStateFlow()

    private fun account(): String =
        AuthStore(app).currentUserIdOrNull()?.let(AccountIds::sanitizeFilePart) ?: "local"

    fun current(): CallPrivacy = readPrivacy()

    fun setPrivacy(value: CallPrivacy) {
        prefs.edit { putString(KEY_PRIVACY + "_" + account(), value.name) }
        _privacy.value = value
    }

    fun groupCallsEnabled(chatId: String): Boolean =
        prefs.getBoolean(groupKey(chatId, "enabled"), true)

    fun setGroupCallsEnabled(chatId: String, enabled: Boolean) {
        prefs.edit { putBoolean(groupKey(chatId, "enabled"), enabled) }
    }

    fun groupAdminsOnly(chatId: String): Boolean =
        prefs.getBoolean(groupKey(chatId, "admins"), false)

    fun setGroupAdminsOnly(chatId: String, adminsOnly: Boolean) {
        prefs.edit { putBoolean(groupKey(chatId, "admins"), adminsOnly) }
    }

    private fun readPrivacy(): CallPrivacy =
        runCatching {
            CallPrivacy.valueOf(prefs.getString(KEY_PRIVACY + "_" + account(), null).orEmpty())
        }.getOrDefault(CallPrivacy.EVERYONE)

    private fun groupKey(chatId: String, suffix: String): String =
        "${account()}_group_${AccountIds.sanitizeFilePart(chatId)}_$suffix"

    companion object {
        private const val PREFS = "truckerload_call_privacy"
        private const val KEY_PRIVACY = "who_can_call"
    }
}
