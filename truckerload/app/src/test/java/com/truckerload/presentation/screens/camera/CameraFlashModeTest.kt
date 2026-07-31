package com.truckerload.presentation.screens.camera

import androidx.camera.core.ImageCapture
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraFlashModeTest {

    @Test
    fun cyclesOffAutoOn() {
        assertEquals(CameraFlashMode.AUTO, CameraFlashMode.OFF.next())
        assertEquals(CameraFlashMode.ON, CameraFlashMode.AUTO.next())
        assertEquals(CameraFlashMode.OFF, CameraFlashMode.ON.next())
    }

    @Test
    fun mapsToImageCaptureConstants() {
        assertEquals(ImageCapture.FLASH_MODE_OFF, CameraFlashMode.OFF.toImageCaptureMode())
        assertEquals(ImageCapture.FLASH_MODE_AUTO, CameraFlashMode.AUTO.toImageCaptureMode())
        assertEquals(ImageCapture.FLASH_MODE_ON, CameraFlashMode.ON.toImageCaptureMode())
    }
}
