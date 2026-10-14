package com.truckerload.presentation.screens.add

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FormDraftTest {

    @Test
    fun cleanUntilTheDriverChangesAField() {
        val opened = FormDraft.diesel("10", "4.00", "", "Loves", 1L, 40, 2026)
        assertFalse(FormDraft.isDirty(baseline = null, current = opened))
        assertFalse(FormDraft.isDirty(baseline = opened, current = opened))
        assertTrue(FormDraft.isDirty(baseline = opened, current = opened.replace("10", "11")))
    }

    @Test
    fun autoFillOnACleanDraftDoesNotCountAsAnEdit() {
        val before = FormDraft.diesel("", "", "", "", 1L, 40, 2026)
        val afterGps = FormDraft.diesel("", "", "", "Pilot, NC", 1L, 40, 2026)
        assertEquals(afterGps, FormDraft.baselineAfterAutoFill(before, before, afterGps))
        assertFalse(FormDraft.isDirty(afterGps, afterGps))
    }

    @Test
    fun autoFillDoesNotHideAnEditTheDriverAlreadyMade() {
        val opened = FormDraft.diesel("", "", "", "", 1L, 40, 2026)
        val typed = FormDraft.diesel("12", "", "", "", 1L, 40, 2026)
        val withGps = FormDraft.diesel("12", "", "", "Pilot, NC", 1L, 40, 2026)
        assertEquals(opened, FormDraft.baselineAfterAutoFill(opened, typed, withGps))
        assertTrue(FormDraft.isDirty(opened, withGps))
    }

    @Test
    fun paycheckAndLoadSignaturesChangeOnlyWithEditedFields() {
        val paycheck = FormDraft.paycheck("100", 5L, 41, 2026, "", "")
        assertFalse(FormDraft.isDirty(paycheck, paycheck))
        assertTrue(
            FormDraft.isDirty(
                paycheck,
                FormDraft.paycheck("100", 5L, 40, 2026, "", ""),
            ),
        )
        val load = FormDraft.addLoad(
            mode = "MANUAL",
            rawText = "",
            tripId = "",
            date = "2026-10-14",
            rate = "",
            miles = "",
            points = listOf("", ""),
            equipment = "",
            documentName = "",
        )
        assertTrue(
            FormDraft.isDirty(
                load,
                FormDraft.addLoad(
                    mode = "MANUAL",
                    rawText = "",
                    tripId = "T-1",
                    date = "2026-10-14",
                    rate = "",
                    miles = "",
                    points = listOf("", ""),
                    equipment = "",
                    documentName = "",
                ),
            ),
        )
    }
}
