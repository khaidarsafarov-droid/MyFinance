package com.truckerload.domain.paycheck

import org.junit.Assert.assertEquals
import org.junit.Test

class PaycheckSourceFileNamesTest {

    @Test
    fun sanitize_stripsPathAndUnsafeChars() {
        assertEquals(
            "Settlement 08.10–08.16 Khaidar Safarov .pdf",
            PaycheckSourceFileNames.sanitize("Settlement 08.10–08.16 Khaidar Safarov .pdf"),
        )
        assertEquals("settlement.pdf", PaycheckSourceFileNames.sanitize("/tmp/../settlement.pdf"))
        assertEquals("bad_name.pdf", PaycheckSourceFileNames.sanitize("bad:name.pdf"))
        assertEquals("settlement", PaycheckSourceFileNames.sanitize("   "))
    }

    @Test
    fun mimeType_matchesCommonSettlementTypes() {
        assertEquals("application/pdf", PaycheckSourceFileNames.mimeType("Settlement.pdf"))
        assertEquals("image/jpeg", PaycheckSourceFileNames.mimeType("photo.JPG"))
        assertEquals("image/png", PaycheckSourceFileNames.mimeType("shot.png"))
        assertEquals("application/octet-stream", PaycheckSourceFileNames.mimeType("notes"))
    }
}
