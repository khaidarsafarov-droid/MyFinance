package com.truckerload.utils

import android.content.Context
import android.content.res.Configuration
import com.truckerload.data.preferences.SettingsDataStore
import kotlinx.coroutines.runBlocking
import java.util.Locale

/** App UI locale; defaults to Russian unless user picks English in settings. */
object AppLocale {

    fun wrap(context: Context, languageTag: String? = null): Context {
        val tag = languageTag ?: readStoredLanguage(context)
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    private fun readStoredLanguage(context: Context): String = runBlocking {
        SettingsDataStore(context.applicationContext).getLanguageOnce().tag
    }
}
