package com.truckerload.domain.geo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryCatalogByIso2Test {

    @Test
    fun byIso2_findsCountriesIgnoringCaseAndWhitespace() {
        assertEquals("United States", CountryCatalog.byIso2("US")?.nameEn)
        assertEquals("Canada", CountryCatalog.byIso2(" ca ")?.nameEn)
        assertEquals("Ukraine", CountryCatalog.byIso2("ua")?.nameEn)
    }

    @Test
    fun byIso2_returnsNullForMissingOrBlankIso() {
        assertNull(CountryCatalog.byIso2(null))
        assertNull(CountryCatalog.byIso2(""))
        assertNull(CountryCatalog.byIso2("ZZ"))
    }
}
