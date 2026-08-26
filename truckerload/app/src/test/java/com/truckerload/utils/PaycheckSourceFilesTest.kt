package com.truckerload.utils

import com.truckerload.domain.paycheck.PaycheckSourceFileNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaycheckSourceFilesTest {

    @Test
    fun copyFromBytes_storesUnderPaychecksAndCanBeDeleted() {
        val context = RuntimeEnvironment.getApplication()
        val path = PaycheckSourceFiles.copyFromBytes(
            context,
            "%PDF-1.4 settlement".toByteArray(),
            "Settlement 08.10–08.16 Khaidar Safarov .pdf",
        )
        assertNotNull(path)
        assertTrue(path!!.startsWith("paychecks/"))
        assertTrue(path.endsWith(".pdf") || path.contains(".pdf"))
        assertTrue(PaycheckSourceFiles.exists(context, path))
        val stored = PaycheckSourceFiles.file(context, path)
        assertNotNull(stored)
        assertEquals("%PDF-1.4 settlement", stored!!.readText())
        PaycheckSourceFiles.delete(context, path)
        assertFalse(PaycheckSourceFiles.exists(context, path))
    }

    @Test
    fun copyFromBytes_emptyReturnsNull() {
        val context = RuntimeEnvironment.getApplication()
        assertEquals(null, PaycheckSourceFiles.copyFromBytes(context, ByteArray(0), "x.pdf"))
    }

    @Test
    fun copiedName_isSanitized() {
        assertEquals("settlement.pdf", PaycheckSourceFileNames.sanitize("/tmp/../settlement.pdf"))
    }
}
