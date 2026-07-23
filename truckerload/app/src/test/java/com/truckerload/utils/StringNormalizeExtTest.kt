package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StringNormalizeExtTest {

    @Test
    fun normalizeKey_trimsAndLowercasesWithUsLocale() {
        val defaultLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale("tr", "TR"))

            assertEquals("id-key", " ID-Key ".normalizeKey())
        } finally {
            Locale.setDefault(defaultLocale)
        }
    }
}
