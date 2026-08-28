package com.truckerload.utils

import android.content.Context
import androidx.core.content.ContextCompat
import com.truckerload.data.preferences.AppLanguage
import com.truckerload.widget.WidgetRefresh

/** Locale-aware context wrapper; language changes go through [AppLanguageManager]. */
object AppLocale {

    fun apply(context: Context, language: AppLanguage) {
        AppLanguageManager.setLanguage(language.tag)
        WidgetRefresh.refreshAndUpdateAsync(context)
    }

    /** Apply locale; AppCompat recreates the activity when the locale actually changes. */
    fun applyAndRecreate(context: Context, language: AppLanguage) {
        apply(context, language)
    }

    fun applyStored(context: Context) {
        AppLanguageManager.setLanguage(AppLanguageManager.getCurrentLanguageCode())
    }

    /** Locale-aware context; use instead of manual Configuration overrides. */
    fun wrap(context: Context): Context = ContextCompat.getContextForLanguage(context)
}
