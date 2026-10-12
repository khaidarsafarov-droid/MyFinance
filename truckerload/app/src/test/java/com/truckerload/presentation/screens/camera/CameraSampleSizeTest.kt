package com.truckerload.presentation.screens.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraSampleSizeTest {

    @Test
    fun keepsLongEdgeAtOrAboveTarget() {
        assertEquals(1, CameraViewModel.sampleSizeFor(longEdge = 2048, targetEdge = 2048))
        assertEquals(1, CameraViewModel.sampleSizeFor(longEdge = 4000, targetEdge = 2048))
        assertEquals(2, CameraViewModel.sampleSizeFor(longEdge = 4096, targetEdge = 2048))
        assertEquals(4, CameraViewModel.sampleSizeFor(longEdge = 9000, targetEdge = 2048))
        assertEquals(1, CameraViewModel.sampleSizeFor(longEdge = 0, targetEdge = 2048))
    }
}
