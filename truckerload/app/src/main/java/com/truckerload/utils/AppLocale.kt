package com.truckerload.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
        val locales = LocaleListCompat.forLanguageTags(language.tag)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.applicationContext
                .getSystemService(android.app.LocaleManager::class.java)
                ?.applicationLocales = LocaleList.forLanguageTags(language.tag)
        }
        AppCompatDelegate.setApplicationLocales(locales)
        SettingsDataStore.mirrorLanguageTag(context.applicationContext, language.tag)
    }

    /** Apply locale and rebuild the current screen so strings update immediately. */
    fun applyAndRecreate(context: Context, language: AppLanguage) {
        apply(context, language)
        findActivity(context)?.recreate()
    }

    private fun findActivity(context: Context): Activity? {
        var current: Context? = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }

    fun applyStored(context: Context) {
        apply(context, SettingsDataStore.readStoredLanguage(context.applicationContext))
    }

    /** Locale-aware context; use instead of manual Configuration overrides. */
    fun wrap(context: Context): Context = ContextCompat.getContextForLanguage(context)
}
