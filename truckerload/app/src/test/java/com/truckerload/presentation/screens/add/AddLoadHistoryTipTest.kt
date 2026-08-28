package com.truckerload.presentation.screens.add

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddLoadHistoryTipTest {
    @Test
    fun showsOnEmptyPasteAndDocument() {
        assertTrue(shouldShowAddLoadHistoryTip(AddLoadInputMode.PASTE, "", false))
        assertTrue(shouldShowAddLoadHistoryTip(AddLoadInputMode.DOCUMENT, "", false))
    }

    @Test
    fun hidesWhenTypingOrExtractingOrOnManual() {
        assertFalse(shouldShowAddLoadHistoryTip(AddLoadInputMode.PASTE, "Trip ID: T-1", false))
        assertFalse(shouldShowAddLoadHistoryTip(AddLoadInputMode.DOCUMENT, "", true))
        assertFalse(shouldShowAddLoadHistoryTip(AddLoadInputMode.MANUAL, "", false))
    }
}
