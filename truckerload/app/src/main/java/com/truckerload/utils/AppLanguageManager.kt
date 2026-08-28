package com.truckerload.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

data class LanguageItem(
    val code: String,
    val nativeName: String,
    val flagEmoji: String,
)

/**
 * Per-app language via AndroidX [AppCompatDelegate.setApplicationLocales].
 * Persistence is AppCompat `autoStoreLocales` (API 24–32) / LocaleManager (API 33+).
 */
object AppLanguageManager {

    const val DEFAULT_LANGUAGE_CODE = "en"

    private val supported = listOf(
        LanguageItem(code = "en", nativeName = "English", flagEmoji = "🇬🇧"),
        LanguageItem(code = "ru", nativeName = "Русский", flagEmoji = "🇷🇺"),
        LanguageItem(code = "es", nativeName = "Español", flagEmoji = "🇪🇸"),
    )

    private val supportedCodes = supported.map { it.code }.toSet()

    fun getSupportedLanguages(): List<LanguageItem> = supported

    fun getCurrentLanguageCode(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val applicationLanguage = if (appLocales.isEmpty) {
            null
        } else {
            appLocales[0]?.language
        }
        return resolveLanguageCode(
            applicationLanguage = applicationLanguage,
            systemLanguage = Locale.getDefault().language,
        )
    }

    fun setLanguage(code: String) {
        val resolved = canonicalize(code)
        val current = AppCompatDelegate.getApplicationLocales()
        if (!current.isEmpty && current[0]?.language == resolved) return
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(resolved))
    }

    fun isRtl(): Boolean = isRtlLanguage(getCurrentLanguageCode())

    fun isRtlLanguage(code: String): Boolean {
        val language = code.lowercase(Locale.US)
            .substringBefore('-')
            .substringBefore('_')
            .trim()
        return language == "ar"
    }

    fun canonicalize(code: String): String {
        val language = code.lowercase(Locale.US)
            .substringBefore('-')
            .substringBefore('_')
            .trim()
        return if (language in supportedCodes) language else DEFAULT_LANGUAGE_CODE
    }

    fun resolveLanguageCode(applicationLanguage: String?, systemLanguage: String): String {
        val app = applicationLanguage?.trim().orEmpty()
        if (app.isNotEmpty()) return canonicalize(app)
        return canonicalize(systemLanguage)
    }

    /**
     * Restore a previously saved in-app choice when AppCompat has no locales yet.
     * Leaves the list empty on first launch so the system language is used.
     */
    fun migrateLegacyIfNeeded(storedTag: String?) {
        if (!AppCompatDelegate.getApplicationLocales().isEmpty) return
        val tag = storedTag?.trim().orEmpty()
        if (tag.isEmpty()) return
        setLanguage(tag)
    }
}
