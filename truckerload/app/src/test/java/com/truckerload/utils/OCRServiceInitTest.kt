package com.truckerload.utils

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression for the 1.6.2 Maintenance (ТО) crash: MaintenanceViewModel and
 * ScannerViewModel build OCRService in their constructors. When ML Kit is not
 * registered, TextRecognition.getClient() throws, and that used to crash the screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OCRServiceInitTest {

    @Test
    fun constructAndClose_doNotTouchMlKit() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        // No MlKitContext under Robolectric: construction must still succeed.
        val service = OCRService(app)
        service.close()
    }

    @Test
    fun createLatinRecognizer_returnsNullWhenMlKitIsNotRegistered() {
        assertEquals(null, OCRService.createLatinRecognizer())
    }
}
