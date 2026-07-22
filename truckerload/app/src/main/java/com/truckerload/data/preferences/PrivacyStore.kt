package com.truckerload.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Privacy lockdown: by default nothing financial leaves the device
 * (no public Downloads, no Telegram money replies, no Android Auto Backup).
 */
class PrivacyStore(context: Context) {
    private val prefs: SharedPreferences = SecurePreferences.open(context, PREFS_NAME)

    private val _lockdown = MutableStateFlow(prefs.getBoolean(KEY_LOCKDOWN, true))
    val lockdownEnabled: StateFlow<Boolean> = _lockdown.asStateFlow()

    /** Master switch. When true, all sharing/export-to-public/Telegram finance is blocked. */
    fun isLockdownEnabled(): Boolean = prefs.getBoolean(KEY_LOCKDOWN, true)

    fun setLockdownEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LOCKDOWN, enabled) }
        _lockdown.value = enabled
    }

    /** Raw flag ignoring master lockdown (for Settings UI). */
    fun isTelegramFinanceOptIn(): Boolean = prefs.getBoolean(KEY_ALLOW_TELEGRAM_FINANCE, false)

    fun isPublicDownloadsOptIn(): Boolean = prefs.getBoolean(KEY_ALLOW_PUBLIC_DOWNLOADS, false)

    /** Allow writing finance CSV/PDF/notes to public Downloads. Default: false. */
    fun allowPublicDownloads(): Boolean =
        !isLockdownEnabled() && isPublicDownloadsOptIn()

    fun setAllowPublicDownloads(allow: Boolean) {
        prefs.edit { putBoolean(KEY_ALLOW_PUBLIC_DOWNLOADS, allow) }
    }

    /** Allow Telegram /stats and document uploads with dollar amounts. Default: false. */
    fun allowTelegramFinancialShare(): Boolean =
        !isLockdownEnabled() && isTelegramFinanceOptIn()

    fun setAllowTelegramFinancialShare(allow: Boolean) {
        prefs.edit { putBoolean(KEY_ALLOW_TELEGRAM_FINANCE, allow) }
    }

    /** Hide $ amounts on home-screen widget. Default: true under lockdown. */
    fun hideWidgetAmounts(): Boolean =
        isLockdownEnabled() || prefs.getBoolean(KEY_HIDE_WIDGET_AMOUNTS, false)

    fun setHideWidgetAmounts(hide: Boolean) {
        prefs.edit { putBoolean(KEY_HIDE_WIDGET_AMOUNTS, hide) }
    }

    companion object {
        private const val PREFS_NAME = "truckerload_privacy_enc"
        private const val KEY_LOCKDOWN = "privacy_lockdown"
        private const val KEY_ALLOW_PUBLIC_DOWNLOADS = "allow_public_downloads"
        private const val KEY_ALLOW_TELEGRAM_FINANCE = "allow_telegram_finance"
        private const val KEY_HIDE_WIDGET_AMOUNTS = "hide_widget_amounts"
    }
}
