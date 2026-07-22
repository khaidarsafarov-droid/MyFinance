package com.truckerload.presentation.screens.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CameraAttachContextTest {

    @Test
    fun fromExplicit_requiresLoadOrTrip() {
        assertNull(CameraAttachContext.fromExplicit(null, null, "2026-07-01"))
        assertNull(CameraAttachContext.fromExplicit("  ", "", "2026-07-01"))
    }

    @Test
    fun fromExplicit_usesProvidedFields() {
        val ctx = CameraAttachContext.fromExplicit("id-1", "T-1", "2026-07-21")
        assertEquals("id-1", ctx?.loadId)
        assertEquals("T-1", ctx?.tripId)
        assertEquals("2026-07-21", ctx?.loadDate)
    }

    @Test
    fun fromLatestLoad_blankBecomesNull() {
        val ctx = CameraAttachContext.fromLatestLoad("  ", "TRIP", "")
        assertNull(ctx.loadId)
        assertEquals("TRIP", ctx.tripId)
        assertNull(ctx.loadDate)
    }

    @Test
    fun freeCameraWatermarkOnly_keepsTripWithoutLoadId() {
        val ctx = CameraAttachContext.fromLatestLoad(null, "T-LATEST", "2026-07-21")
        assertNull(ctx.loadId)
        assertEquals("T-LATEST", ctx.tripId)
        assertEquals("2026-07-21", ctx.loadDate)
    }
}
