package com.truckerload.utils

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.truckerload.data.preferences.AppLanguage
import com.truckerload.data.preferences.SettingsDataStore

/** App UI locale; defaults to Russian unless user picks English in settings. */
object AppLocale {

    fun apply(context: Context, language: AppLanguage) {
        val current = AppCompatDelegate.getApplicationLocales()
        if (!current.isEmpty && current[0]?.language == language.tag) return

        val locales = LocaleListCompat.forLanguageTags(language.tag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext
                .getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
        SettingsDataStore.mirrorLanguageTag(context.applicationContext, language.tag)
    }

    /** Apply locale; AppCompat recreates the activity when the locale actually changes. */
    fun applyAndRecreate(context: Context, language: AppLanguage) {
        apply(context, language)
    }

    fun applyStored(context: Context) {
        apply(context, SettingsDataStore.readStoredLanguage(context.applicationContext))
    }

    /** Locale-aware context; use instead of manual Configuration overrides. */
    fun wrap(context: Context): Context = ContextCompat.getContextForLanguage(context)
}
