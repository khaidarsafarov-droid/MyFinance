package com.truckerload.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class StoredFileNamesTest {

    @Test
    fun stripsTraversalFromFileName() {
        assertEquals("отчёт.pdf", StoredFileNames.sanitize("inbox/отчёт.pdf"))
        assertEquals("settlement.pdf", StoredFileNames.sanitize("../../settlement.pdf"))
        assertEquals("file", StoredFileNames.sanitize(".."))
    }

    @Test
    fun keepsDownloadsInsideTruckoRig() {
        assertEquals("TruckoRig", StoredFileNames.downloadsRelativeDir(".."))
        assertEquals("TruckoRig/etc", StoredFileNames.downloadsRelativeDir("../etc"))
        assertEquals("TruckoRig/Reports", StoredFileNames.downloadsRelativeDir("TruckoRig/Reports"))
        assertEquals("TruckoRig/secret", StoredFileNames.downloadsRelativeDir("TruckoRig/../../secret"))
        assertFalse(StoredFileNames.downloadsRelativeDir("TruckoRig/../../secret").contains(".."))
    }
}
