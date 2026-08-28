package com.truckerload.utils

import com.truckerload.data.preferences.AppLanguage
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageManagerTest {

    @Test
    fun supportedLanguages_areEnglishRussianAndSpanish() {
        val codes = AppLanguageManager.getSupportedLanguages().map { it.code }
        assertEquals(listOf("en", "ru", "es"), codes)
        assertFalse(codes.contains("ar"))
    }

    @Test
    fun supportedLanguages_useNativeNames() {
        val byCode = AppLanguageManager.getSupportedLanguages().associateBy { it.code }
        assertEquals("English", byCode.getValue("en").nativeName)
        assertEquals("Русский", byCode.getValue("ru").nativeName)
        assertEquals("Español", byCode.getValue("es").nativeName)
    }

    @Test
    fun canonicalize_fallsBackToEnglishForUnsupportedTags() {
        assertEquals("en", AppLanguageManager.canonicalize("de"))
        assertEquals("en", AppLanguageManager.canonicalize("fr-CA"))
        assertEquals("en", AppLanguageManager.canonicalize(""))
        assertEquals("en", AppLanguageManager.canonicalize("ar"))
    }

    @Test
    fun canonicalize_keepsSupportedLanguageFromRegionalTag() {
        assertEquals("ru", AppLanguageManager.canonicalize("ru-RU"))
        assertEquals("es", AppLanguageManager.canonicalize("es-MX"))
        assertEquals("en", AppLanguageManager.canonicalize("EN-US"))
    }

    @Test
    fun resolveLanguageCode_usesAppChoiceThenSystemThenEnglish() {
        assertEquals("es", AppLanguageManager.resolveLanguageCode("es", "de"))
        assertEquals("ru", AppLanguageManager.resolveLanguageCode(null, "ru"))
        assertEquals("en", AppLanguageManager.resolveLanguageCode(null, "de"))
        assertEquals("en", AppLanguageManager.resolveLanguageCode("", "zh"))
    }

    @Test
    fun isRtl_isFalseWithoutArabic() {
        assertFalse(AppLanguageManager.isRtlLanguage("en"))
        assertFalse(AppLanguageManager.isRtlLanguage("ru"))
        assertFalse(AppLanguageManager.isRtlLanguage("es"))
        assertFalse(AppLanguageManager.isRtlLanguage("de"))
    }

    @Test
    fun appLanguage_fromTagAndOrdinalStayCompatibleWithBackups() {
        assertEquals(AppLanguage.RU, AppLanguage.fromOrdinal(0))
        assertEquals(AppLanguage.EN, AppLanguage.fromOrdinal(1))
        assertEquals(AppLanguage.ES, AppLanguage.fromOrdinal(2))
        assertEquals(AppLanguage.EN, AppLanguage.fromOrdinal(99))
        assertEquals(AppLanguage.ES, AppLanguage.fromTag("es-MX"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("de"))
    }
}

class AppLanguageResourcesTest {

    @Test
    fun localeConfig_listsEnglishRussianAndSpanish() {
        val text = localeConfigFile().readText()
        assertTrue(text.contains("android:name=\"en\""))
        assertTrue(text.contains("android:name=\"ru\""))
        assertTrue(text.contains("android:name=\"es\""))
        assertFalse(text.contains("android:name=\"ar\""))
    }

    @Test
    fun manifest_enablesPerAppLocalesAndRtl() {
        val text = manifestFile().readText()
        assertTrue(text.contains("android:localeConfig=\"@xml/locales_config\""))
        assertTrue(text.contains("android:supportsRtl=\"true\""))
        assertTrue(text.contains("androidx.appcompat.app.AppLocalesMetadataHolderService"))
        assertTrue(text.contains("android:name=\"autoStoreLocales\""))
        assertTrue(text.contains("android:value=\"true\""))
        assertFalse(text.contains("tools:locale"))
    }

    @Test
    fun translationFolders_includeRussianAndSpanish() {
        val res = resDir()
        assertTrue(File(res, "values-ru/strings.xml").isFile)
        assertTrue(File(res, "values-es/strings.xml").isFile)
        val base = File(res, "values/strings.xml").readText()
        val ru = File(res, "values-ru/strings.xml").readText()
        val es = File(res, "values-es/strings.xml").readText()
        assertTrue(base.contains("settings_language_es"))
        assertTrue(ru.contains("settings_language_es"))
        assertTrue(es.contains("settings_language_es"))
        assertTrue(base.contains(">Language<") || base.contains("settings_language_title\">Language<"))
        assertTrue(ru.contains("Язык"))
        assertTrue(es.contains("Idioma"))
    }

    private fun localeConfigFile(): File = listOf(
        File("src/main/res/xml/locales_config.xml"),
        File("app/src/main/res/xml/locales_config.xml"),
        File("/workspace/truckerload/app/src/main/res/xml/locales_config.xml"),
    ).first { it.isFile }

    private fun manifestFile(): File = listOf(
        File("src/main/AndroidManifest.xml"),
        File("app/src/main/AndroidManifest.xml"),
        File("/workspace/truckerload/app/src/main/AndroidManifest.xml"),
    ).first { it.isFile }

    private fun resDir(): File = listOf(
        File("src/main/res"),
        File("app/src/main/res"),
        File("/workspace/truckerload/app/src/main/res"),
    ).first { it.isDirectory }
}
