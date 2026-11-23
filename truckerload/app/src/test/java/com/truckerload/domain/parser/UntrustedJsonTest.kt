package com.truckerload.domain.parser

import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UntrustedJsonTest {

    @Test
    fun parsesSmallObject() {
        val parsed = UntrustedJson.objectOrNull("""{"trip":"4401901"}""")
        assertTrue(parsed!!.getString("trip") == "4401901")
    }

    @Test
    fun rejectsOversizedPayload() {
        val huge = "{" + "1".repeat(UntrustedJson.MAX_CHARS) + "}"
        assertNull(UntrustedJson.objectOrNull(huge))
        assertNull(UntrustedJson.valueOrNull(huge))
        assertNull(JsonLoadParser.parseOne(huge))
    }
}
